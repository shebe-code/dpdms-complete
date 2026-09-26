package com.dpdms.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
public class ReportServiceApplication { public static void main(String[] args){SpringApplication.run(ReportServiceApplication.class,args);} }

enum ReportFormat { CSV, XLSX, DOCX, PDF }
enum ReportTemplateRegistry { INSTANCE; String title(){return "DPDMS Approved Incident Report";} }
record ReportRequest(String hazard,String ward,String district,String severity,String from,String to,ReportFormat format){}
interface ReportGenerator { byte[] generate(List<Map<String,Object>> rows,ReportRequest request) throws Exception; String contentType(); String extension(); }

@Component class CsvGenerator implements ReportGenerator {
    public byte[] generate(List<Map<String,Object>> rows,ReportRequest request){
        LinkedHashSet<String> keys=keys(rows);StringBuilder b=new StringBuilder();b.append(keys.stream().map(this::escape).collect(Collectors.joining(","))).append("\n");
        for(Map<String,Object> row:rows)b.append(keys.stream().map(k->escape(Objects.toString(row.get(k),""))).collect(Collectors.joining(","))).append("\n");
        return b.toString().getBytes(StandardCharsets.UTF_8);
    }
    private LinkedHashSet<String> keys(List<Map<String,Object>> rows){LinkedHashSet<String> k=new LinkedHashSet<>(List.of("hazard","id","ward","district","province","occurrenceAt","reporter","severity","status","latitude","longitude"));for(Map<String,Object>m:rows)k.addAll(m.keySet());return k;}
    private String escape(String x){String q=String.valueOf((char)34);return q+x.replace(q,q+q)+q;}
    public String contentType(){return "text/csv";}public String extension(){return "csv";}
}
@Component class ExcelGenerator implements ReportGenerator {
    public byte[] generate(List<Map<String,Object>> rows,ReportRequest request)throws Exception{try(XSSFWorkbook workbook=new XSSFWorkbook();ByteArrayOutputStream out=new ByteArrayOutputStream()){var sheet=workbook.createSheet("Incidents");LinkedHashSet<String>keys=new LinkedHashSet<>(List.of("hazard","id","ward","district","province","occurrenceAt","reporter","severity","status","latitude","longitude"));for(Map<String,Object>m:rows)keys.addAll(m.keySet());String[] cols=keys.toArray(String[]::new);var head=sheet.createRow(0);for(int i=0;i<cols.length;i++)head.createCell(i).setCellValue(cols[i]);int row=1;for(Map<String,Object>m:rows){var rr=sheet.createRow(row++);for(int i=0;i<cols.length;i++)rr.createCell(i).setCellValue(Objects.toString(m.get(cols[i]),""));}workbook.write(out);return out.toByteArray();}}
    public String contentType(){return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";}public String extension(){return "xlsx";}
}
@Component class WordGenerator implements ReportGenerator {
    public byte[] generate(List<Map<String,Object>> rows,ReportRequest request)throws Exception{try(XWPFDocument doc=new XWPFDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){doc.createParagraph().createRun().setText(ReportTemplateRegistry.INSTANCE.title());String[]cols={"hazard","ward","district","occurrenceAt","reporter","severity","status"};var table=doc.createTable(rows.size()+1,cols.length);for(int i=0;i<cols.length;i++)table.getRow(0).getCell(i).setText(cols[i]);for(int r=0;r<rows.size();r++)for(int c=0;c<cols.length;c++)table.getRow(r+1).getCell(c).setText(Objects.toString(rows.get(r).get(cols[c]),""));doc.write(out);return out.toByteArray();}}
    public String contentType(){return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";}public String extension(){return "docx";}
}
@Component class PdfGenerator implements ReportGenerator {
    public byte[] generate(List<Map<String,Object>> rows,ReportRequest request)throws Exception{try(PDDocument doc=new PDDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){PDPage page=new PDPage();doc.addPage(page);try(PDPageContentStream stream=new PDPageContentStream(doc,page)){String[]cols={"hazard","ward","district","severity","status","reporter"};float y=760;write(stream,40,y,ReportTemplateRegistry.INSTANCE.title(),14,true);y-=24;write(stream,40,y,String.join(" | ",cols),9,true);y-=16;for(Map<String,Object>m:rows.stream().limit(45).toList()){String line=Arrays.stream(cols).map(c->trim(Objects.toString(m.get(c),""),15)).collect(Collectors.joining(" | "));write(stream,40,y,line,8,false);y-=12;if(y<40)break;}}doc.save(out);return out.toByteArray();}}
    private void write(PDPageContentStream s,float x,float y,String text,float size,boolean bold)throws IOException{s.beginText();s.setFont(new PDType1Font(bold?Standard14Fonts.FontName.HELVETICA_BOLD:Standard14Fonts.FontName.HELVETICA),size);s.setTextMatrix(new Matrix(1,0,0,1,x,y));s.showText(text.replaceAll("[^\\x20-\\x7E]"," "));s.endText();}
    private String trim(String s,int n){return s.length()>n?s.substring(0,n):s;}
    public String contentType(){return "application/pdf";}public String extension(){return "pdf";}
}
@Component class ReportGeneratorFactory { private final Map<ReportFormat,ReportGenerator> map=new EnumMap<>(ReportFormat.class);ReportGeneratorFactory(List<ReportGenerator> list){for(ReportGenerator g:list){String n=g.getClass().getSimpleName();if(n.contains("Csv"))map.put(ReportFormat.CSV,g);if(n.contains("Excel"))map.put(ReportFormat.XLSX,g);if(n.contains("Word"))map.put(ReportFormat.DOCX,g);if(n.contains("Pdf"))map.put(ReportFormat.PDF,g);}}ReportGenerator get(ReportFormat f){return map.get(f);} }
@Component class HazardDataClient {
    private final ObjectProvider<DiscoveryClient> provider;private final RestClient.Builder builder;private final String gatewaySecret;
    HazardDataClient(ObjectProvider<DiscoveryClient>provider,RestClient.Builder builder,@Value("${dpdms.gateway-secret}")String gatewaySecret){this.provider=provider;this.builder=builder;this.gatewaySecret=gatewaySecret;}
    List<Map<String,Object>> fetch(String service,Map<String,String> headers,ReportRequest request){DiscoveryClient d=provider.getIfAvailable();if(d==null)throw new IllegalStateException("Discovery unavailable");List<ServiceInstance>instances=d.getInstances(service);if(instances.isEmpty())throw new IllegalStateException(service+" unavailable");String path=switch(service){case "FLOOD-SERVICE"->"/api/v1/floods/approved";case "DROUGHT-SERVICE"->"/api/v1/droughts/approved";case "FIRE-SERVICE"->"/api/v1/fires/approved";case "ZOONOTIC-SERVICE"->"/api/v1/zoonotic/approved";default->"/api/v1/mining-accidents/approved";};List<String>query=new ArrayList<>();if(request.district()!=null&&!request.district().isBlank())query.add("district="+enc(request.district()));if(request.severity()!=null&&!request.severity().isBlank())query.add("severity="+enc(request.severity()));if(request.from()!=null&&!request.from().isBlank())query.add("from="+enc(request.from()));if(request.to()!=null&&!request.to().isBlank())query.add("to="+enc(request.to()));String url=instances.get(0).getUri()+path+(query.isEmpty()?"":"?"+String.join("&",query));HttpHeaders h=new HttpHeaders();h.set("X-Gateway-Secret",gatewaySecret);headers.forEach(h::set);ResponseEntity<List<Map<String,Object>>>r=builder.build().get().uri(url).headers(x->x.addAll(h)).retrieve().toEntity(new ParameterizedTypeReference<List<Map<String,Object>>>(){});return r.getBody()==null?List.of():r.getBody();}
    private String enc(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
}
@RestController @RequestMapping("/api/v1/reports") class ReportController {
    private final HazardDataClient client;private final ReportGeneratorFactory factory;private final String gatewaySecret;ReportController(HazardDataClient client,ReportGeneratorFactory factory,@Value("${dpdms.gateway-secret}")String gatewaySecret){this.client=client;this.factory=factory;this.gatewaySecret=gatewaySecret;}
    @GetMapping ResponseEntity<byte[]> report(@RequestHeader("X-Gateway-Secret")String gatewayHeader,@RequestHeader("X-User-Role")String role,@RequestHeader("X-User-Hazard")String uh,@RequestHeader("X-User-Username")String user,@RequestHeader(value="X-User-Ward",required=false)String ward,@RequestHeader(value="X-User-District",required=false)String dh,@RequestHeader(value="X-User-Province",required=false)String province,@RequestParam(defaultValue="ALL")String hazard,@RequestParam(required=false)String district,@RequestParam(required=false)String severity,@RequestParam(required=false)String from,@RequestParam(required=false)String to,@RequestParam(defaultValue="APPROVED")String approvalStatus,@RequestParam(defaultValue="PDF")ReportFormat format){
        if(!Objects.equals(gatewayHeader,gatewaySecret))return ResponseEntity.status(403).build();
        if(!"APPROVED".equalsIgnoreCase(approvalStatus))return ResponseEntity.status(403).build();
        Map<String,String>headers=Map.of("X-User-Role",role,"X-User-Hazard",uh,"X-User-Username",user,"X-User-Ward",Objects.toString(ward,""),"X-User-District",Objects.toString(dh,""),"X-User-Province",Objects.toString(province,""));
        ReportRequest request=new ReportRequest(hazard,ward,district,severity,from,to,format);List<Map<String,Object>>rows=new ArrayList<>();
        for(String service:List.of("FLOOD-SERVICE","DROUGHT-SERVICE","FIRE-SERVICE","ZOONOTIC-SERVICE","MINING-SERVICE")){String serviceHazard=service.replace("-SERVICE","");if(!hazard.equalsIgnoreCase("ALL")&&!hazard.equalsIgnoreCase(serviceHazard))continue;try{for(Map<String,Object>m:client.fetch(service,headers,request)){if(ward!=null&&!ward.isBlank()&&!ward.equalsIgnoreCase(Objects.toString(m.get("ward"),"")))continue;Map<String,Object>copy=new LinkedHashMap<>(m);copy.put("hazard",serviceHazard);rows.add(copy);}}catch(Exception ignored){}}
        try{ReportGenerator generator=factory.get(format);byte[]body=generator.generate(rows,request);HttpHeaders response=new HttpHeaders();response.setContentType(MediaType.parseMediaType(generator.contentType()));response.setContentDisposition(ContentDisposition.attachment().filename("dpdms-report."+generator.extension()).build());return new ResponseEntity<>(body,response,HttpStatus.OK);}catch(Exception e){return ResponseEntity.internalServerError().build();}
    }
}
