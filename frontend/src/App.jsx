import React, { useEffect, useRef, useState } from 'react';
import L from 'leaflet';

const API_BASE =
  import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/v1';

/* =========================================================
   HAZARD CONFIGURATION
   ========================================================= */

const hazards = [
  [
    'FLOOD',
    'Flood',
    'floods',
    [
      {
        key: 'peakWaterLevelMetres',
        label: 'Peak water level (m)',
        type: 'number',
        min: 0
      },
      {
        key: 'riverBasin',
        label: 'River basin',
        type: 'text'
      },
      {
        key: 'householdsDisplaced',
        label: 'Households displaced',
        type: 'number',
        min: 0
      },
      {
        key: 'areaFloodedHectares',
        label: 'Area flooded (ha)',
        type: 'number',
        min: 0
      },
      {
        key: 'durationDays',
        label: 'Duration (days)',
        type: 'number',
        min: 0
      }
    ]
  ],

  [
    'DROUGHT',
    'Drought',
    'droughts',
    [
      {
        key: 'rainfallDeficitMm',
        label: 'Rainfall deficit (mm)',
        type: 'number',
        min: 0
      },
      {
        key: 'consecutiveDryDays',
        label: 'Consecutive dry days',
        type: 'number',
        min: 0
      },
      {
        key: 'cropFailurePercentage',
        label: 'Crop failure (%)',
        type: 'number',
        min: 0,
        max: 100
      },
      {
        key: 'peopleFacingWaterShortages',
        label: 'People facing water shortages',
        type: 'number',
        min: 0
      },
      {
        key: 'livestockMortalityCount',
        label: 'Livestock mortality count',
        type: 'number',
        min: 0
      }
    ]
  ],

  [
    'FIRE',
    'Fire',
    'fires',
    [
      {
        key: 'areaBurnedHectares',
        label: 'Area burned (ha)',
        type: 'number',
        min: 0
      },
      {
        key: 'suspectedCause',
        label: 'Suspected cause',
        type: 'text'
      },
      {
        key: 'injuriesFatalities',
        label: 'Injuries/fatalities',
        type: 'number',
        min: 0
      },
      {
        key: 'structuresDestroyed',
        label: 'Structures destroyed',
        type: 'number',
        min: 0
      },
      {
        key: 'active',
        label: 'Still active',
        type: 'checkbox'
      }
    ]
  ],

  [
    'ZOONOTIC',
    'Zoonotic disease',
    'zoonotic',
    [
      {
        key: 'pathogenName',
        label: 'Pathogen/disease',
        type: 'text'
      },
      {
        key: 'animalSpecies',
        label: 'Animal species',
        type: 'text'
      },
      {
        key: 'confirmedHumanCases',
        label: 'Confirmed human cases',
        type: 'number',
        min: 0
      },
      {
        key: 'confirmedAnimalCases',
        label: 'Confirmed animal cases',
        type: 'number',
        min: 0
      },
      {
        key: 'eventClassification',
        label: 'Classification (CLUSTER/OUTBREAK)',
        type: 'text'
      }
    ]
  ],

  [
    'MINING',
    'Mining accident',
    'mining-accidents',
    [
      {
        key: 'mineName',
        label: 'Mine name',
        type: 'text'
      },
      {
        key: 'mineType',
        label: 'Mine type',
        type: 'text'
      },
      {
        key: 'accidentType',
        label: 'Accident type',
        type: 'text'
      },
      {
        key: 'trappedOrInjuredMiners',
        label: 'Trapped/injured miners',
        type: 'number',
        min: 0
      },
      {
        key: 'fatalities',
        label: 'Fatalities',
        type: 'number',
        min: 0
      },
      {
        key: 'rescueOngoing',
        label: 'Rescue ongoing',
        type: 'checkbox'
      }
    ]
  ]
];

/* =========================================================
   API HELPER
   ========================================================= */

async function api(path, options = {}) {
  const token = localStorage.getItem('dpdms_token');

  const headers = new Headers(options.headers || {});

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  if (options.body && !(options.body instanceof Blob)) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  });

  if (!response.ok) {
    let message = `HTTP ${response.status}`;

    try {
      const json = await response.json();

      message =
        json.message ||
        json.error ||
        json.detail ||
        message;
    } catch {}

    throw new Error(message);
  }

  return response;
}

/* =========================================================
   LOGIN
   ========================================================= */

function Login({ onLoggedIn }) {
  const [username, setUsername] = useState('flood.recorder');
  const [password, setPassword] = useState('Password123!');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function login(e) {
    e.preventDefault();

    setBusy(true);
    setError('');

    try {
      const response = await fetch(
        `${API_BASE}/auth/login`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            username,
            password
          })
        }
      );

      const json = await response.json();

      if (!response.ok) {
        throw new Error(
          json.message || 'Login failed'
        );
      }

      localStorage.setItem(
        'dpdms_token',
        json.token
      );

      localStorage.setItem(
        'dpdms_user',
        JSON.stringify(json)
      );

      onLoggedIn(json);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login">
      <div className="login-card">
        <h1>DPDMS</h1>

        <p>
          Rushinga Provincial Disaster Monitoring and Management System
        </p>

        <form onSubmit={login}>
          <label>
            Username
            <input
              value={username}
              onChange={e =>
                setUsername(e.target.value)
              }
            />
          </label>

          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={e =>
                setPassword(e.target.value)
              }
            />
          </label>

          {error && (
            <div className="error">
              {error}
            </div>
          )}

          <button disabled={busy}>
            {busy
              ? 'Signing in...'
              : 'Sign in'}
          </button>
        </form>

        <small>
          Demo password: Password123!
        </small>
      </div>
    </div>
  );
}

/* =========================================================
   MAP
   ========================================================= */

function MapView({ incidents = [] }) {
  const ref = useRef(null);

  useEffect(() => {
    if (!ref.current) {
      return;
    }

    const map =
      L.map(ref.current)
        .setView(
          [-16.65, 31.43],
          10
        );

    L.tileLayer(
      'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      {
        attribution:
          '© OpenStreetMap contributors'
      }
    ).addTo(map);

    incidents.forEach(incident => {
      const latitude =
        Number(incident.latitude);

      const longitude =
        Number(incident.longitude);

      if (
        Number.isFinite(latitude) &&
        Number.isFinite(longitude)
      ) {
        L.circleMarker(
          [latitude, longitude],
          {
            radius: 7
          }
        )
          .addTo(map)
          .bindPopup(
            `<strong>${incident.hazard || ''}</strong><br/>` +
              `${incident.ward || ''}<br/>` +
              `Severity: ${incident.severity || ''}<br/>` +
              `${incident.occurrenceAt || ''}`
          );
      }
    });

    return () => {
      map.remove();
    };
  }, [incidents]);

  return (
    <div
      className="map"
      ref={ref}
    />
  );
}

/* =========================================================
   DASHBOARD
   ========================================================= */

function Dashboard({
  summary,
  onRefresh
}) {
  if (!summary) {
    return (
      <div className="loading">
        Loading dashboard...
      </div>
    );
  }

  return (
    <div>
      <div className="section-head">
        <div>
          <h2>
            Live dashboard
          </h2>

          <p>
            Approved incidents only.
            Last refresh:{' '}
            {summary.lastRefresh || '—'}
          </p>
        </div>

        <button
          onClick={onRefresh}
        >
          Refresh
        </button>
      </div>

      <div className="cards">
        <div className="card">
          <span>
            Total approved
          </span>

          <strong>
            {summary.totalApproved}
          </strong>
        </div>

        {Object.entries(
          summary.countsByHazard || {}
        ).map(
          ([hazard, count]) => (
            <div
              className="card"
              key={hazard}
            >
              <span>
                {hazard}
              </span>

              <strong>
                {count}
              </strong>
            </div>
          )
        )}
      </div>

      <div className="two-col">
        <div className="panel">
          <h3>
            Severity
          </h3>

          {Object.entries(
            summary.countsBySeverity || {}
          ).map(
            ([severity, count]) => (
              <div
                className="bar-row"
                key={severity}
              >
                <span>
                  {severity}
                </span>

                <b
                  style={{
                    width: `${Math.min(
                      100,
                      count * 15 + 10
                    )}%`
                  }}
                >
                  {count}
                </b>
              </div>
            )
          )}
        </div>

        <div className="panel">
          <h3>
            Trend
          </h3>

          {Object.entries(
            summary.trend || {}
          ).map(
            ([date, count]) => (
              <div
                className="trend-row"
                key={date}
              >
                <span>
                  {date}
                </span>

                <b>
                  {count}
                </b>
              </div>
            )
          )}
        </div>
      </div>

      <div className="panel">
        <h3>
          Incident map
        </h3>

        <MapView
          incidents={
            summary.mapIncidents || []
          }
        />
      </div>

      <div className="panel">
        <h3>
          Recent incidents
        </h3>

        <Table
          rows={
            summary.recentIncidents || []
          }
        />
      </div>
    </div>
  );
}

/* =========================================================
   TABLE
   ========================================================= */

function Table({ rows }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Hazard</th>
            <th>Ward</th>
            <th>District</th>
            <th>Severity</th>
            <th>Status</th>
            <th>Date</th>
          </tr>
        </thead>

        <tbody>
          {rows.map(
            (row, index) => (
              <tr
                key={
                  row.id ||
                  index
                }
              >
                <td>
                  {row.hazard}
                </td>

                <td>
                  {row.ward}
                </td>

                <td>
                  {row.district}
                </td>

                <td>
                  {row.severity}
                </td>

                <td>
                  {row.status ||
                    'APPROVED'}
                </td>

                <td>
                  {row.occurrenceAt}
                </td>
              </tr>
            )
          )}
        </tbody>
      </table>
    </div>
  );
}

/* =========================================================
   INCIDENTS
   ========================================================= */

function Incidents({ user }) {
  const [hazard, setHazard] =
    useState(
      user.hazard === 'ALL'
        ? 'FLOOD'
        : user.hazard
    );

  const [rows, setRows] =
    useState([]);

  const [error, setError] =
    useState('');

  const [editing, setEditing] =
    useState(null);

  const [history, setHistory] =
    useState(null);

  const [historyRows, setHistoryRows] =
    useState([]);

  const config =
    hazards.find(
      hazardConfig =>
        hazardConfig[0] === hazard
    ) || hazards[0];

  async function load() {
    setError('');

    try {
      const response =
        await api(
          `/${config[2]}`
        );

      setRows(
        await response.json()
      );
    } catch (e) {
      setRows([]);
      setError(e.message);
    }
  }

  useEffect(() => {
    setEditing(null);
    setHistory(null);
    setHistoryRows([]);
    load();
  }, [hazard]);

  const canApprove =
    user.role.endsWith(
      '_SUPERVISOR'
    ) ||
    user.role ===
      'PROVINCIAL_ADMIN';

  const isRecorder =
    user.role ===
      `${hazard}_RECORDER`;

  function recorderCanModify(row) {
    return (
      isRecorder &&
      row.reporter ===
        user.username &&
      (
        row.status ===
          'PENDING' ||
        row.status ===
          'CORRECTIONS_REQUESTED'
      )
    );
  }

  async function action(id, type) {
    try {
      let endpoint = '';

      if (type === 'approve') {
        endpoint =
          `/${config[2]}/${id}/approve`;
      } else {
        const reason =
          prompt(
            type === 'reject'
              ? 'Reason for rejection:'
              : 'Reason for requesting corrections:'
          );

        if (
          reason === null ||
          reason.trim() === ''
        ) {
          return;
        }

        const encodedReason =
          encodeURIComponent(
            reason.trim()
          );

        if (type === 'reject') {
          endpoint =
            `/${config[2]}/${id}/reject?reason=${encodedReason}`;
        } else if (
          type === 'corrections'
        ) {
          endpoint =
            `/${config[2]}/${id}/corrections?reason=${encodedReason}`;
        } else {
          throw new Error(
            'Unknown incident action.'
          );
        }
      }

      await api(
        endpoint,
        {
          method: 'POST'
        }
      );

      await load();
    } catch (e) {
      alert(
        `Action failed: ${e.message}`
      );
    }
  }

  async function openHistory(row) {
    try {
      const response =
        await api(
          `/${config[2]}/${row.id}/audit`
        );

      const json =
        await response.json();

      setHistory(row);
      setHistoryRows(json);
    } catch (e) {
      alert(
        `Could not load history: ${e.message}`
      );
    }
  }

  async function startEdit(row) {
    try {
      const response =
        await api(
          `/${config[2]}/${row.id}`
        );

      setEditing(
        await response.json()
      );
    } catch (e) {
      alert(
        `Could not load incident: ${e.message}`
      );
    }
  }

  function setEditValue(
    key,
    value
  ) {
    setEditing(current => ({
      ...current,
      [key]: value
    }));
  }

  async function saveEdit(e) {
    e.preventDefault();

    try {
      const payload = {
        ...editing,

        confirmedHumanCases:
          Number(
            editing.confirmedHumanCases
          ),

        confirmedAnimalCases:
          Number(
            editing.confirmedAnimalCases
          ),

        latitude:
          Number(
            editing.latitude
          ),

        longitude:
          Number(
            editing.longitude
          )
      };

      await api(
        `/${config[2]}/${editing.id}`,
        {
          method: 'PUT',
          body:
            JSON.stringify(
              payload
            )
        }
      );

      setEditing(null);

      await load();

      alert(
        'Incident updated successfully.'
      );
    } catch (e) {
      alert(
        `Update failed: ${e.message}`
      );
    }
  }

  async function deleteIncident(row) {
    const confirmed =
      confirm(
        `Delete incident ${row.id}? This cannot be undone.`
      );

    if (!confirmed) {
      return;
    }

    try {
      await api(
        `/${config[2]}/${row.id}`,
        {
          method: 'DELETE'
        }
      );

      await load();

      alert(
        'Incident deleted successfully.'
      );
    } catch (e) {
      alert(
        `Delete failed: ${e.message}`
      );
    }
  }

  return (
    <div>
      <div className="section-head">
        <div>
          <h2>
            Incidents
          </h2>

          <p>
            Backend scoping is enforced
            per role and hazard.
          </p>
        </div>

        <select
          value={hazard}
          onChange={e =>
            setHazard(
              e.target.value
            )
          }
          disabled={
            user.hazard !== 'ALL'
          }
        >
          {hazards.map(
            hazardConfig => (
              <option
                key={
                  hazardConfig[0]
                }
                value={
                  hazardConfig[0]
                }
              >
                {
                  hazardConfig[1]
                }
              </option>
            )
          )}
        </select>
      </div>

      {error && (
        <div className="error">
          {error}
        </div>
      )}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Ward</th>
              <th>Severity</th>
              <th>Status</th>
              <th>Reporter</th>
              <th>Actions</th>
            </tr>
          </thead>

          <tbody>
            {rows.map(
              row => (
                <tr key={row.id}>
                  <td>
                    {row.id}
                  </td>

                  <td>
                    {row.ward}
                  </td>

                  <td>
                    {row.severity}
                  </td>

                  <td>
                    {row.status}
                  </td>

                  <td>
                    {row.reporter}
                  </td>

                  <td>
                    <div className="actions">
                      {canApprove &&
                        (
                          row.status ===
                            'PENDING' ||
                          row.status ===
                            'CORRECTIONS_REQUESTED'
                        ) && (
                          <>
                            <button
                              onClick={() =>
                                action(
                                  row.id,
                                  'approve'
                                )
                              }
                            >
                              Approve
                            </button>

                            <button
                              onClick={() =>
                                action(
                                  row.id,
                                  'reject'
                                )
                              }
                            >
                              Reject
                            </button>

                            <button
                              onClick={() =>
                                action(
                                  row.id,
                                  'corrections'
                                )
                              }
                            >
                              Corrections
                            </button>
                          </>
                        )}

                      {recorderCanModify(
                        row
                      ) && (
                        <>
                          <button
                            onClick={() =>
                              startEdit(
                                row
                              )
                            }
                          >
                            Edit
                          </button>

                          <button
                            onClick={() =>
                              deleteIncident(
                                row
                              )
                            }
                          >
                            Delete
                          </button>
                        </>
                      )}

                      <button
                        onClick={() =>
                          openHistory(
                            row
                          )
                        }
                      >
                        History
                      </button>
                    </div>
                  </td>
                </tr>
              )
            )}
          </tbody>
        </table>
      </div>

      {editing &&
        hazard ===
          'ZOONOTIC' && (
        <div className="panel">
          <div className="section-head">
            <div>
              <h3>
                Edit Zoonotic Incident #{editing.id}
              </h3>

              <p>
                Only pending or
                correction-requested incidents
                can be edited.
              </p>
            </div>

            <button
              className="ghost"
              onClick={() =>
                setEditing(null)
              }
            >
              Cancel
            </button>
          </div>

          <form
            onSubmit={
              saveEdit
            }
          >
            <div className="form-grid">
              <label>
                Occurrence date/time
                <input
                  type="datetime-local"
                  value={
                    editing.occurrenceAt
                      ? editing.occurrenceAt.slice(
                          0,
                          16
                        )
                      : ''
                  }
                  required
                  onChange={e =>
                    setEditValue(
                      'occurrenceAt',
                      e.target.value
                    )
                  }
                />
              </label>

              <label>
                Severity
                <select
                  value={
                    editing.severity
                  }
                  required
                  onChange={e =>
                    setEditValue(
                      'severity',
                      e.target.value
                    )
                  }
                >
                  <option value="LOW">
                    LOW
                  </option>
                  <option value="MEDIUM">
                    MEDIUM
                  </option>
                  <option value="HIGH">
                    HIGH
                  </option>
                  <option value="CRITICAL">
                    CRITICAL
                  </option>
                </select>
              </label>

              <label>
                Latitude
                <input
                  type="number"
                  step="any"
                  min="-90"
                  max="90"
                  value={
                    editing.latitude ?? ''
                  }
                  required
                  onChange={e =>
                    setEditValue(
                      'latitude',
                      e.target.value
                    )
                  }
                />
              </label>

              <label>
                Longitude
                <input
                  type="number"
                  step="any"
                  min="-180"
                  max="180"
                  value={
                    editing.longitude ?? ''
                  }
                  required
                  onChange={e =>
                    setEditValue(
                      'longitude',
                      e.target.value
                    )
                  }
                />
              </label>

              <label>
                Pathogen/disease
                <input
                  value={
                    editing.pathogenName ??
                    ''
                  }
                  required
                  onChange={e =>
                    setEditValue(
                      'pathogenName',
                      e.target.value
                    )
                  }
                />
              </label>

              <label>
                Animal species
                <input
                  value={
                    editing.animalSpecies ??
                    ''
                  }
                  required
                  onChange={e =>
                    setEditValue(
                      'animalSpecies',
                      e.target.value
                    )
                  }
                />
              </label>

              <label>
                Confirmed human cases
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={
                    editing.confirmedHumanCases ??
                    0
                  }
                  required
                  onChange={e =>
                    setEditValue(
                      'confirmedHumanCases',
                      e.target.value
                    )
                  }
                />
              </label>

              <label>
                Confirmed animal cases
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={
                    editing.confirmedAnimalCases ??
                    0
                  }
                  required
                  onChange={e =>
                    setEditValue(
                      'confirmedAnimalCases',
                      e.target.value
                    )
                  }
                />
              </label>

              <label>
                Classification
                <select
                  value={
                    editing.eventClassification ??
                    ''
                  }
                  required
                  onChange={e =>
                    setEditValue(
                      'eventClassification',
                      e.target.value
                    )
                  }
                >
                  <option value="">
                    Select classification
                  </option>
                  <option value="CLUSTER">
                    CLUSTER
                  </option>
                  <option value="OUTBREAK">
                    OUTBREAK
                  </option>
                </select>
              </label>
            </div>

            <button
              type="submit"
            >
              Save changes
            </button>
          </form>
        </div>
      )}

      {history && (
        <div className="panel">
          <div className="section-head">
            <div>
              <h3>
                Incident #{history.id} History
              </h3>

              <p>
                Audit trail recorded by
                the backend.
              </p>
            </div>

            <button
              className="ghost"
              onClick={() => {
                setHistory(null);
                setHistoryRows([]);
              }}
            >
              Close
            </button>
          </div>

          {historyRows.length ===
          0 ? (
            <p>
              No history entries
              were found.
            </p>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Action</th>
                    <th>Actor</th>
                    <th>Role</th>
                    <th>Old status</th>
                    <th>New status</th>
                    <th>Note</th>
                    <th>Changed</th>
                  </tr>
                </thead>

                <tbody>
                  {historyRows.map(
                    (
                      entry,
                      index
                    ) => (
                      <tr
                        key={
                          entry.id ??
                          index
                        }
                      >
                        <td>
                          {entry.action}
                        </td>

                        <td>
                          {
                            entry.actorUsername
                          }
                        </td>

                        <td>
                          {
                            entry.actorRole
                          }
                        </td>

                        <td>
                          {
                            entry.oldStatus ||
                            '-'
                          }
                        </td>

                        <td>
                          {
                            entry.newStatus ||
                            '-'
                          }
                        </td>

                        <td>
                          {
                            entry.note ||
                            '-'
                          }
                        </td>

                        <td>
                          {
                            entry.changedAt
                          }
                        </td>
                      </tr>
                    )
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}


/* =========================================================
   CAPTURE
   ========================================================= */

function Capture({ user }) {
  const config =
    hazards.find(
      hazardConfig =>
        hazardConfig[0] ===
        user.hazard
    );

  const [common, setCommon] =
    useState({
      occurrenceAt:
        new Date()
          .toISOString()
          .slice(0, 16),

      severity:
        'MEDIUM',

      latitude:
        '-16.65',

      longitude:
        '31.43'
    });

  const [specific, setSpecific] =
    useState({});

  const [msg, setMsg] =
    useState('');

  const [msgType, setMsgType] =
    useState('');

  if (!config) {
    return (
      <div className="panel">
        <h2>
          Capture Incident
        </h2>

        <p>
          National and provincial
          admin users should select
          a hazard under Incidents
          to test that service.
        </p>
      </div>
    );
  }

  function setCommonValue(
    key,
    value
  ) {
    setCommon({
      ...common,
      [key]: value
    });
  }

  function setSpecificValue(
    key,
    value
  ) {
    setSpecific({
      ...specific,
      [key]: value
    });
  }

  /* =======================================================
     FRONTEND VALIDATION
     ======================================================= */

  function validateForm() {
    const errors = [];

    if (!user.ward?.trim()) {
      errors.push(
        'Ward is missing from your user account.'
      );
    }

    if (!user.district?.trim()) {
      errors.push(
        'District is missing from your user account.'
      );
    }

    if (!user.province?.trim()) {
      errors.push(
        'Province is missing from your user account.'
      );
    }

    if (!user.username?.trim()) {
      errors.push(
        'Reporter username is missing from your user account.'
      );
    }

    if (!common.occurrenceAt?.trim()) {
      errors.push(
        'Occurrence date/time is required.'
      );
    }

    if (!common.severity?.trim()) {
      errors.push(
        'Severity is required.'
      );
    }

    const latitude =
      Number(common.latitude);

    if (
      common.latitude === '' ||
      !Number.isFinite(latitude)
    ) {
      errors.push(
        'Latitude is required.'
      );
    } else if (
      latitude < -90 ||
      latitude > 90
    ) {
      errors.push(
        'Latitude must be between -90 and 90.'
      );
    }

    const longitude =
      Number(common.longitude);

    if (
      common.longitude === '' ||
      !Number.isFinite(longitude)
    ) {
      errors.push(
        'Longitude is required.'
      );
    } else if (
      longitude < -180 ||
      longitude > 180
    ) {
      errors.push(
        'Longitude must be between -180 and 180.'
      );
    }

    config[3].forEach(
      field => {
        const value =
          specific[
            field.key
          ];

        if (
          field.type ===
          'checkbox'
        ) {
          return;
        }

        if (
          field.type ===
          'text'
        ) {
          if (
            value ===
              undefined ||
            value ===
              null ||
            String(value).trim() ===
              ''
          ) {
            errors.push(
              `${field.label} is required.`
            );
          }

          return;
        }

        if (
          field.type ===
          'number'
        ) {
          if (
            value ===
              undefined ||
            value ===
              null ||
            value === ''
          ) {
            errors.push(
              `${field.label} is required.`
            );

            return;
          }

          const number =
            Number(value);

          if (
            !Number.isFinite(
              number
            )
          ) {
            errors.push(
              `${field.label} must be a valid number.`
            );

            return;
          }

          if (
            field.min !==
              undefined &&
            number <
              field.min
          ) {
            errors.push(
              `${field.label} cannot be less than ${field.min}.`
            );
          }

          if (
            field.max !==
              undefined &&
            number >
              field.max
          ) {
            errors.push(
              `${field.label} cannot be greater than ${field.max}.`
            );
          }
        }
      }
    );

    return errors;
  }

  /* =======================================================
     SAVE INCIDENT
     ======================================================= */

  async function save(e) {
    e.preventDefault();

    setMsg('');
    setMsgType('');

    const errors =
      validateForm();

    if (
      errors.length > 0
    ) {
      setMsg(
        errors.join(' ')
      );

      setMsgType(
        'error'
      );

      return;
    }

    /*
    Complete request body.

    Shared fields come automatically from the authenticated
    account rather than being manually entered by the user.
    */

    const body = {
      ...common,
      ...specific,

      occurrenceAt:
        common.occurrenceAt,

      ward:
        user.ward,

      district:
        user.district,

      province:
        user.province,

      reporter:
        user.username
    };

    /*
    Convert number and checkbox fields.
    */

    config[3].forEach(
      field => {
        if (
          field.type ===
          'number'
        ) {
          body[field.key] =
            Number(
              body[field.key]
            );
        }

        if (
          field.type ===
          'checkbox'
        ) {
          body[field.key] =
            Boolean(
              body[field.key]
            );
        }

        if (
          field.type ===
          'text'
        ) {
          body[field.key] =
            String(
              body[field.key]
            ).trim();
        }
      }
    );

    try {
      await api(
        `/${config[2]}`,
        {
          method:
            'POST',
          body:
            JSON.stringify(
              body
            )
        }
      );

      setMsg(
        'Incident captured successfully and placed in PENDING status.'
      );

      setMsgType(
        'success'
      );

      setSpecific(
        {}
      );
    } catch (e) {
      setMsg(
        `The incident could not be submitted: ${e.message}`
      );

      setMsgType(
        'error'
      );
    }
  }

  return (
    <form
      onSubmit={save}
    >
      <h2>
        Capture {config[1]} incident
      </h2>

      {/* ===================================================
          LOCATION
          =================================================== */}

      <div className="location-panel">
        <h3>
          Incident location
        </h3>

        <div className="location-grid">
          <div className="location-item">
            <span>
              Ward
            </span>

            <strong>
              {user.ward ||
                'Not assigned'}
            </strong>
          </div>

          <div className="location-item">
            <span>
              District
            </span>

            <strong>
              {user.district ||
                'Not assigned'}
            </strong>
          </div>

          <div className="location-item">
            <span>
              Province
            </span>

            <strong>
              {user.province ||
                'Not assigned'}
            </strong>
          </div>

          <div className="location-item">
            <span>
              Reporter
            </span>

            <strong>
              {user.username ||
                'Not assigned'}
            </strong>
          </div>
        </div>

        <p className="location-note">
          Ward, district, province and
          reporter are automatically
          assigned from your authenticated
          user account.
        </p>
      </div>

      {/* ===================================================
          COMMON FIELDS
          =================================================== */}

      <div className="form-grid">
        <label>
          Occurrence date/time

          <input
            type="datetime-local"
            value={
              common.occurrenceAt
            }
            required
            onChange={e =>
              setCommonValue(
                'occurrenceAt',
                e.target.value
              )
            }
          />
        </label>

        <label>
          Severity

          <select
            value={
              common.severity
            }
            required
            onChange={e =>
              setCommonValue(
                'severity',
                e.target.value
              )
            }
          >
            <option value="">
              Select severity
            </option>

            <option value="LOW">
              LOW
            </option>

            <option value="MEDIUM">
              MEDIUM
            </option>

            <option value="HIGH">
              HIGH
            </option>

            <option value="CRITICAL">
              CRITICAL
            </option>
          </select>
        </label>

        <label>
          Latitude

          <input
            type="number"
            step="any"
            min="-90"
            max="90"
            value={
              common.latitude
            }
            required
            onChange={e =>
              setCommonValue(
                'latitude',
                e.target.value
              )
            }
          />
        </label>

        <label>
          Longitude

          <input
            type="number"
            step="any"
            min="-180"
            max="180"
            value={
              common.longitude
            }
            required
            onChange={e =>
              setCommonValue(
                'longitude',
                e.target.value
              )
            }
          />
        </label>

        {/* =================================================
            HAZARD-SPECIFIC FIELDS
            ================================================= */}

        {config[3].map(
          field => (
            <label
              key={
                field.key
              }
            >
              {field.label}

              {field.type ===
              'checkbox' ? (
                <input
                  type="checkbox"
                  checked={Boolean(
                    specific[
                      field.key
                    ]
                  )}
                  onChange={e =>
                    setSpecificValue(
                      field.key,
                      e.target.checked
                    )
                  }
                />
              ) : (
                <input
                  type={
                    field.type
                  }
                  value={
                    specific[
                      field.key
                    ] ?? ''
                  }
                  min={
                    field.min
                  }
                  max={
                    field.max
                  }
                  required
                  onChange={e =>
                    setSpecificValue(
                      field.key,
                      e.target.value
                    )
                  }
                />
              )}
            </label>
          )
        )}
      </div>

      {msg && (
        <div
          className={
            msgType ===
            'error'
              ? 'error'
              : 'notice'
          }
        >
          {msg}
        </div>
      )}

      <button
        type="submit"
      >
        Submit incident
      </button>
    </form>
  );
}

/* =========================================================
   REPORTS
   ========================================================= */

function Reports({ user }) {
  const [hazard, setHazard] =
    useState('ALL');

  const [format, setFormat] =
    useState('PDF');

  const [busy, setBusy] =
    useState(false);

  async function download() {
    setBusy(true);

    try {
      const response =
        await api(
          `/reports?hazard=${hazard}&format=${format}&approvalStatus=APPROVED`
        );

      const blob =
        await response.blob();

      const url =
        URL.createObjectURL(
          blob
        );

      const anchor =
        document.createElement(
          'a'
        );

      anchor.href =
        url;

      anchor.download =
        `dpdms-report.${format.toLowerCase()}`;

      anchor.click();

      URL.revokeObjectURL(
        url
      );
    } catch (e) {
      alert(
        e.message
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <h2>
        Reports
      </h2>

      <p>
        Reports contain approved
        incidents only and remain
        subject to backend
        hazard/ward scope.
      </p>

      <div className="form-grid">
        <label>
          Hazard

          <select
            value={hazard}
            onChange={e =>
              setHazard(
                e.target.value
              )
            }
          >
            <option value="ALL">
              ALL
            </option>

            {hazards.map(
              hazardConfig => (
                <option
                  key={
                    hazardConfig[0]
                  }
                  value={
                    hazardConfig[0]
                  }
                >
                  {
                    hazardConfig[0]
                  }
                </option>
              )
            )}
          </select>
        </label>

        <label>
          Format

          <select
            value={format}
            onChange={e =>
              setFormat(
                e.target.value
              )
            }
          >
            <option value="PDF">
              PDF
            </option>

            <option value="DOCX">
              DOCX
            </option>

            <option value="XLSX">
              XLSX
            </option>

            <option value="CSV">
              CSV
            </option>
          </select>
        </label>
      </div>

      <button
        onClick={
          download
        }
        disabled={
          busy
        }
      >
        {busy
          ? 'Generating...'
          : 'Download report'}
      </button>
    </div>
  );
}



/* =========================================================
   ALERTS
   ========================================================= */

function Alerts() {
  const [rows, setRows] =
    useState([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState('');

  async function loadAlerts() {
    setLoading(true);
    setError('');

    try {
      const response =
        await api(
          '/alerts/logs'
        );

      setRows(
        await response.json()
      );
    } catch (e) {
      setRows([]);
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAlerts();
  }, []);

  return (
    <div>
      <div className="section-head">
        <div>
          <h2>
            Disaster Alerts
          </h2>

          <p>
            Email and WhatsApp alert
            delivery history.
          </p>
        </div>

        <button
          onClick={loadAlerts}
          disabled={loading}
        >
          {loading
            ? 'Loading...'
            : 'Refresh'}
        </button>
      </div>

      {error && (
        <div className="error">
          {error}
        </div>
      )}

      {!loading &&
        !error &&
        rows.length === 0 && (
          <div className="panel">
            No alerts have been
            generated yet.
          </div>
        )}

      {rows.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Time</th>
                <th>Hazard</th>
                <th>Incident</th>
                <th>Channel</th>
                <th>Recipient</th>
                <th>Status</th>
                <th>Message</th>
              </tr>
            </thead>

            <tbody>
              {rows.map(
                row => (
                  <tr key={row.id}>
                    <td>
                      {row.timestamp}
                    </td>

                    <td>
                      {row.hazard}
                    </td>

                    <td>
                      {row.incidentId}
                    </td>

                    <td>
                      {row.channel}
                    </td>

                    <td>
                      {row.recipient}
                    </td>

                    <td>
                      {row.deliveryStatus}
                    </td>

                    <td>
                      {row.message}
                    </td>
                  </tr>
                )
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
/* =========================================================
   MAIN APP
   ========================================================= */




function App() {
  const [user, setUser] =
    useState(() => {
      try {
        return JSON.parse(
          localStorage.getItem(
            'dpdms_user'
          ) || 'null'
        );
      } catch {
        return null;
      }
    });

  const [tab, setTab] =
    useState(
      'dashboard'
    );

  const [summary, setSummary] =
    useState(null);

  const [error, setError] =
    useState('');

  async function loadSummary() {
    try {
      setError('');

      const response =
        await api(
          '/dashboard/summary'
        );

      setSummary(
        await response.json()
      );
    } catch (e) {
      setError(
        e.message
      );
    }
  }

  useEffect(() => {
    if (user) {
      loadSummary();
    }
  }, [user]);

  if (!user) {
    return (
      <Login
        onLoggedIn={
          setUser
        }
      />
    );
  }

  function logout() {
    localStorage.clear();

    setUser(null);

    setSummary(null);
  }

  return (
    <div className="app">
      <header>
        <div>
          <strong>
            DPDMS
          </strong>

          <span>
            {user.role} ·{' '}
            {user.hazard}{' '}
            {user.ward &&
              `· ${user.ward}`}
          </span>
        </div>

        <button
          className="ghost"
          onClick={
            logout
          }
        >
          Sign out
        </button>
      </header>

      <nav>
        {[
          [
            'dashboard',
            'Dashboard'
          ],
          [
            'incidents',
            'Incidents'
          ],
          [
  'capture',
  'Capture'
],
[
  'reports',
  'Reports'
],
...(
  user.role === 'NATIONAL_USER' ||
  user.role === 'PROVINCIAL_ADMIN'
    ? [
        [
          'alerts',
          'Alerts'
        ]
      ]
    : []
)
        ].map(
          item => (
            <button
              key={
                item[0]
              }
              className={
                tab ===
                item[0]
                  ? 'active'
                  : ''
              }
              onClick={() =>
                setTab(
                  item[0]
                )
              }
            >
              {item[1]}
            </button>
          )
        )}
      </nav>

      <main>
        {error && (
          <div className="error">
            {error}
          </div>
        )}

        {tab ===
          'dashboard' && (
          <Dashboard
            summary={
              summary
            }
            onRefresh={
              loadSummary
            }
          />
        )}

        {tab ===
          'incidents' && (
          <Incidents
            user={
              user
            }
          />
        )}

        {tab ===
          'capture' && (
          <Capture
            user={
              user
            }
          />
        )}

       {tab ===
  'reports' && (
  <Reports
    user={
      user
    }
  />
)}

{tab ===
  'alerts' && (
  <Alerts />
)}
      </main>
    </div>
  );
}

export default App;
