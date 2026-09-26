import React, { useEffect, useMemo, useRef, useState } from 'react';
import L from 'leaflet';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/v1';
const hazards = [
  ['FLOOD','Flood','floods', [['peakWaterLevelMetres','Peak water level (m)','number'],['riverBasin','River basin','text'],['householdsDisplaced','Households displaced','number'],['areaFloodedHectares','Area flooded (ha)','number'],['durationDays','Duration (days)','number']]],
  ['DROUGHT','Drought','droughts', [['rainfallDeficitMm','Rainfall deficit (mm)','number'],['consecutiveDryDays','Consecutive dry days','number'],['cropFailurePercentage','Crop failure (%)','number'],['peopleFacingWaterShortages','People facing water shortages','number'],['livestockMortalityCount','Livestock mortality count','number']]],
  ['FIRE','Fire','fires', [['areaBurnedHectares','Area burned (ha)','number'],['suspectedCause','Suspected cause','text'],['injuriesFatalities','Injuries/fatalities','number'],['structuresDestroyed','Structures destroyed','number'],['active','Still active','checkbox']]],
  ['ZOONOTIC','Zoonotic disease','zoonotic', [['pathogenName','Pathogen/disease','text'],['animalSpecies','Animal species','text'],['confirmedHumanCases','Confirmed human cases','number'],['confirmedAnimalCases','Confirmed animal cases','number'],['eventClassification','Classification (CLUSTER/OUTBREAK)','text']]],
  ['MINING','Mining accident','mining-accidents', [['mineName','Mine name','text'],['mineType','Mine type','text'],['accidentType','Accident type','text'],['trappedOrInjuredMiners','Trapped/injured miners','number'],['fatalities','Fatalities','number'],['rescueOngoing','Rescue ongoing','checkbox']]]
];

async function api(path, options={}){
  const token=localStorage.getItem('dpdms_token');
  const headers=new Headers(options.headers||{});
  if(token) headers.set('Authorization',`Bearer ${token}`);
  if(options.body && !(options.body instanceof Blob)) headers.set('Content-Type','application/json');
  const r=await fetch(`${API_BASE}${path}`,{...options,headers});
  if(!r.ok){let msg=`HTTP ${r.status}`;try{const j=await r.json();msg=j.message||j.error||msg;}catch{}throw new Error(msg);}
  return r;
}

function Login({onLoggedIn}){
  const [username,setUsername]=useState('flood.recorder');const [password,setPassword]=useState('Password123!');const [error,setError]=useState('');const [busy,setBusy]=useState(false);
  async function login(e){e.preventDefault();setBusy(true);setError('');try{const r=await fetch(`${API_BASE}/auth/login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username,password})});const j=await r.json();if(!r.ok)throw new Error(j.message||'Login failed');localStorage.setItem('dpdms_token',j.token);localStorage.setItem('dpdms_user',JSON.stringify(j));onLoggedIn(j);}catch(err){setError(err.message)}finally{setBusy(false)}}
  return <div className="login"><div className="login-card"><h1>DPDMS</h1><p>Rushinga Provincial Disaster Monitoring and Management System</p><form onSubmit={login}><label>Username<input value={username} onChange={e=>setUsername(e.target.value)}/></label><label>Password<input type="password" value={password} onChange={e=>setPassword(e.target.value)}/></label>{error&&<div className="error">{error}</div>}<button disabled={busy}>{busy?'Signing in…':'Sign in'}</button></form><small>Demo password: Password123!</small></div></div>;
}

function MapView({incidents=[]}){
  const ref=useRef(null);useEffect(()=>{if(!ref.current)return;const map=L.map(ref.current).setView([-16.65,31.43],10);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{attribution:'© OpenStreetMap contributors'}).addTo(map);incidents.forEach(i=>{const lat=Number(i.latitude),lng=Number(i.longitude);if(Number.isFinite(lat)&&Number.isFinite(lng))L.circleMarker([lat,lng],{radius:7}).addTo(map).bindPopup(`<strong>${i.hazard}</strong><br/>${i.ward||''}<br/>Severity: ${i.severity||''}<br/>${i.occurrenceAt||''}`);});return()=>map.remove();},[incidents]);return <div className="map" ref={ref}/>;
}

function Dashboard({summary,onRefresh}){
  if(!summary)return <div className="loading">Loading dashboard…</div>;
  return <div><div className="section-head"><div><h2>Live dashboard</h2><p>Approved incidents only. Last refresh: {summary.lastRefresh||'—'}</p></div><button onClick={onRefresh}>Refresh</button></div><div className="cards"><div className="card"><span>Total approved</span><strong>{summary.totalApproved}</strong></div>{Object.entries(summary.countsByHazard||{}).map(([k,v])=><div className="card" key={k}><span>{k}</span><strong>{v}</strong></div>)}</div><div className="two-col"><div className="panel"><h3>Severity</h3>{Object.entries(summary.countsBySeverity||{}).map(([k,v])=><div className="bar-row" key={k}><span>{k}</span><b style={{width:`${Math.min(100,v*15+10)}%`}}>{v}</b></div>)}</div><div className="panel"><h3>Trend</h3>{Object.entries(summary.trend||{}).map(([k,v])=><div className="trend-row" key={k}><span>{k}</span><b>{v}</b></div>)}</div></div><div className="panel"><h3>Incident map</h3><MapView incidents={summary.mapIncidents||[]}/></div><div className="panel"><h3>Recent incidents</h3><Table rows={summary.recentIncidents||[]}/></div></div>;
}

function Table({rows}){return <div className="table-wrap"><table><thead><tr><th>Hazard</th><th>Ward</th><th>District</th><th>Severity</th><th>Status</th><th>Date</th></tr></thead><tbody>{rows.map((r,i)=><tr key={r.id||i}><td>{r.hazard}</td><td>{r.ward}</td><td>{r.district}</td><td>{r.severity}</td><td>{r.status||'APPROVED'}</td><td>{r.occurrenceAt}</td></tr>)}</tbody></table></div>}

function Incidents({user}){
  const [hazard,setHazard]=useState(user.hazard==='ALL'?'FLOOD':user.hazard);const [rows,setRows]=useState([]);const [error,setError]=useState('');const cfg=hazards.find(h=>h[0]===hazard)||hazards[0];
  async function load(){setError('');try{const r=await api(`/${cfg[2]}`);setRows(await r.json());}catch(e){setRows([]);setError(e.message)}}useEffect(()=>{load()},[hazard]);
  const canApprove=user.role.endsWith('_SUPERVISOR')||user.role==='PROVINCIAL_ADMIN';
  async function action(id,type){try{if(type==='approve')await api(`/${cfg[2]}/${id}/approve`,{method:'POST'});else{const reason=prompt('Reason:');if(!reason)return;await api(`/${cfg[2]}/${id}/${type==='reject'?'reject':'corrections'}?reason=${encodeURIComponent(reason)}`,{method:'POST'});}load();}catch(e){alert(e.message)}}
  return <div><div className="section-head"><div><h2>Incidents</h2><p>Backend scoping is enforced per role and hazard.</p></div><select value={hazard} onChange={e=>setHazard(e.target.value)} disabled={user.hazard!=='ALL'}>{hazards.map(h=><option key={h[0]} value={h[0]}>{h[1]}</option>)}</select></div>{error&&<div className="error">{error}</div>}<div className="table-wrap"><table><thead><tr><th>ID</th><th>Ward</th><th>Severity</th><th>Status</th><th>Reporter</th><th>Actions</th></tr></thead><tbody>{rows.map(r=><tr key={r.id}><td>{r.id}</td><td>{r.ward}</td><td>{r.severity}</td><td>{r.status}</td><td>{r.reporter}</td><td>{canApprove&&r.status!=='APPROVED'?<div className="actions"><button onClick={()=>action(r.id,'approve')}>Approve</button><button onClick={()=>action(r.id,'reject')}>Reject</button><button onClick={()=>action(r.id,'corrections')}>Corrections</button></div>:user.role==='NATIONAL_USER'?'Read only':'—'}</td></tr>)}</tbody></table></div></div>;
}

function Capture({user}){
  const cfg=hazards.find(h=>h[0]===user.hazard);const [common,setCommon]=useState({occurrenceAt:new Date().toISOString().slice(0,16),severity:'MEDIUM',latitude:'-16.65',longitude:'31.43'});const [specific,setSpecific]=useState({});const [msg,setMsg]=useState('');
  if(!cfg)return <div className="panel">National/admin users should select a hazard under Incidents to test that service.</div>;
  function setC(k,v){setCommon({...common,[k]:v})}function setS(k,v){setSpecific({...specific,[k]:v})}
  async function save(e){e.preventDefault();setMsg('');const body={...common,...specific,occurrenceAt:common.occurrenceAt};for(const x of cfg[3])if(x[2]==='number'&&body[x[0]]!=='')body[x[0]]=Number(body[x[0]]);for(const x of cfg[3])if(x[2]==='checkbox')body[x[0]]=Boolean(body[x[0]]);try{await api(`/${cfg[2]}`,{method:'POST',body:JSON.stringify(body)});setMsg('Incident captured and placed in PENDING status.');setSpecific({});}catch(e){setMsg(e.message)}}
  return <form onSubmit={save}><h2>Capture {cfg[1]} incident</h2><div className="form-grid"><label>Occurrence date/time<input type="datetime-local" value={common.occurrenceAt} onChange={e=>setC('occurrenceAt',e.target.value)}/></label><label>Severity<select value={common.severity} onChange={e=>setC('severity',e.target.value)}><option>LOW</option><option>MEDIUM</option><option>HIGH</option><option>CRITICAL</option></select></label><label>Latitude<input type="number" step="any" value={common.latitude} onChange={e=>setC('latitude',e.target.value)}/></label><label>Longitude<input type="number" step="any" value={common.longitude} onChange={e=>setC('longitude',e.target.value)}/></label>{cfg[3].map(([k,label,type])=><label key={k}>{label}{type==='checkbox'?<input type="checkbox" checked={Boolean(specific[k])} onChange={e=>setS(k,e.target.checked)}/>:<input type={type} value={specific[k]??''} onChange={e=>setS(k,e.target.value)}/>}</label>)}</div>{msg&&<div className="notice">{msg}</div>}<button type="submit">Submit incident</button></form>;
}

function Reports({user}){
  const [hazard,setHazard]=useState('ALL');const [format,setFormat]=useState('PDF');const [busy,setBusy]=useState(false);
  async function download(){setBusy(true);try{const r=await api(`/reports?hazard=${hazard}&format=${format}&approvalStatus=APPROVED`);const blob=await r.blob();const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`dpdms-report.${format.toLowerCase()}`;a.click();URL.revokeObjectURL(url);}catch(e){alert(e.message)}finally{setBusy(false)}}
  return <div><h2>Reports</h2><p>Reports contain approved incidents only and remain subject to backend hazard/ward scope.</p><div className="form-grid"><label>Hazard<select value={hazard} onChange={e=>setHazard(e.target.value)}><option>ALL</option>{hazards.map(h=><option key={h[0]}>{h[0]}</option>)}</select></label><label>Format<select value={format} onChange={e=>setFormat(e.target.value)}><option>PDF</option><option>DOCX</option><option>XLSX</option><option>CSV</option></select></label></div><button onClick={download} disabled={busy}>{busy?'Generating…':'Download report'}</button></div>;
}

function App(){
  const [user,setUser]=useState(()=>JSON.parse(localStorage.getItem('dpdms_user')||'null'));const [tab,setTab]=useState('dashboard');const [summary,setSummary]=useState(null);const [error,setError]=useState('');
  async function loadSummary(){try{setError('');const r=await api('/dashboard/summary');setSummary(await r.json());}catch(e){setError(e.message)}}
  useEffect(()=>{if(user)loadSummary();},[user]);
  if(!user)return <Login onLoggedIn={setUser}/>;
  function logout(){localStorage.clear();setUser(null);setSummary(null)}
  return <div className="app"><header><div><strong>DPDMS</strong><span>{user.role} · {user.hazard} {user.ward&&`· ${user.ward}`}</span></div><button className="ghost" onClick={logout}>Sign out</button></header><nav>{[['dashboard','Dashboard'],['incidents','Incidents'],['capture','Capture'],['reports','Reports']].map(x=><button key={x[0]} className={tab===x[0]?'active':''} onClick={()=>setTab(x[0])}>{x[1]}</button>)}</nav><main>{error&&<div className="error">{error}</div>}{tab==='dashboard'&&<Dashboard summary={summary} onRefresh={loadSummary}/>} {tab==='incidents'&&<Incidents user={user}/>} {tab==='capture'&&<Capture user={user}/>} {tab==='reports'&&<Reports user={user}/>}</main></div>
}

export default App;
