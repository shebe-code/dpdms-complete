package com.dpdms.alert;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootApplication @EnableAsync
public class AlertServiceApplication { public static void main(String[] args){SpringApplication.run(AlertServiceApplication.class,args);} }
@Entity @Table(name="alert_logs") class AlertLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long incidentId;private String hazard,channel,recipient,deliveryStatus,message;private LocalDateTime timestamp;
    protected AlertLog(){}
    AlertLog(Long incidentId,String hazard,String channel,String recipient,String deliveryStatus,String message){this.incidentId=incidentId;this.hazard=hazard;this.channel=channel;this.recipient=recipient;this.deliveryStatus=deliveryStatus;this.message=message;this.timestamp=LocalDateTime.now();}
    public Long getId(){return id;}public Long getIncidentId(){return incidentId;}public String getHazard(){return hazard;}public String getChannel(){return channel;}public String getRecipient(){return recipient;}public String getDeliveryStatus(){return deliveryStatus;}public String getMessage(){return message;}public LocalDateTime getTimestamp(){return timestamp;}
}
@Repository interface AlertLogRepository extends org.springframework.data.jpa.repository.JpaRepository<AlertLog,Long>{}
class AlertRequest { public Long incidentId;public String hazard;public String severity;public String message;public List<String> emailRecipients=new ArrayList<>();public List<String> whatsappRecipients=new ArrayList<>(); }
interface AlertChannel { String name(); String send(String recipient,String message); }
@Component class EmailAlertChannel implements AlertChannel {
    private final JavaMailSender mail;private final String from;
    EmailAlertChannel(JavaMailSender mail,@Value("${dpdms.alert-email-from:dpdms@example.com}")String from){this.mail=mail;this.from=from;}
    public String name(){return "EMAIL";}
    public String send(String recipient,String message){try{if(System.getenv("SMTP_HOST")==null||System.getenv("SMTP_HOST").isBlank())return "SIMULATED";SimpleMailMessage mailMessage=new SimpleMailMessage();mailMessage.setFrom(from);mailMessage.setTo(recipient);mailMessage.setSubject("DPDMS Disaster Alert");mailMessage.setText(message);mail.send(mailMessage);return "DELIVERED";}catch(Exception e){return "FAILED";}}
}
@Component class WhatsAppAlertChannel implements AlertChannel {
    private final RestClient.Builder builder;private final String token,phone,version;
    WhatsAppAlertChannel(RestClient.Builder builder,@Value("${WHATSAPP_TOKEN:}")String token,@Value("${WHATSAPP_PHONE_NUMBER_ID:}")String phone,@Value("${WHATSAPP_GRAPH_VERSION:v23.0}")String version){this.builder=builder;this.token=token;this.phone=phone;this.version=version;}
    public String name(){return "WHATSAPP";}
    public String send(String recipient,String message){try{if(token.isBlank()||phone.isBlank())return "SIMULATED";String url="https://graph.facebook.com/"+version+"/"+phone+"/messages";Map<String,Object>body=Map.of("messaging_product","whatsapp","to",recipient,"type","text","text",Map.of("body",message));builder.build().post().uri(url).headers(h->h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();return "DELIVERED";}catch(Exception e){return "FAILED";}}
}
@Component class AlertChannelFactory { private final Map<String,AlertChannel> channels=new HashMap<>();AlertChannelFactory(List<AlertChannel> list){for(AlertChannel c:list)channels.put(c.name(),c);}AlertChannel get(String name){return Optional.ofNullable(channels.get(name.toUpperCase(Locale.ROOT))).orElseThrow(()->new IllegalArgumentException("Unsupported alert channel."));} }
@Service class AlertDispatcher {
    private final AlertLogRepository repository;private final AlertChannelFactory factory;private final List<String> defaultEmails,defaultWhatsapp;
    AlertDispatcher(AlertLogRepository repository,AlertChannelFactory factory,@Value("${dpdms.default-email-recipients:demo@example.com}")String emails,@Value("${dpdms.default-whatsapp-recipients:+263700000000}")String whatsapp){this.repository=repository;this.factory=factory;this.defaultEmails=split(emails);this.defaultWhatsapp=split(whatsapp);}
    private List<String> split(String s){return Arrays.stream(s.split(",")).map(String::trim).filter(x->!x.isBlank()).toList();}
    @Async public void dispatch(AlertRequest request){for(String r:(request.emailRecipients==null||request.emailRecipients.isEmpty()?defaultEmails:request.emailRecipients))send(request,"EMAIL",r);for(String r:(request.whatsappRecipients==null||request.whatsappRecipients.isEmpty()?defaultWhatsapp:request.whatsappRecipients))send(request,"WHATSAPP",r);}
    private void send(AlertRequest request,String channel,String recipient){String status=factory.get(channel).send(recipient,request.message);repository.save(new AlertLog(request.incidentId,request.hazard,channel,recipient,status,request.message));}
}
@RestController @RequestMapping("/api/v1/alerts") class AlertController {
    private final AlertDispatcher dispatcher;private final AlertLogRepository repository;private final String secret;
    AlertController(AlertDispatcher dispatcher,AlertLogRepository repository,@Value("${dpdms.gateway-secret}")String secret){this.dispatcher=dispatcher;this.repository=repository;this.secret=secret;}
    private void check(String supplied){if(!Objects.equals(supplied,secret))throw new IllegalStateException("Forbidden");}
    @PostMapping ResponseEntity<?> create(@RequestHeader("X-Gateway-Secret")String supplied,@RequestBody AlertRequest request){check(supplied);dispatcher.dispatch(request);return ResponseEntity.accepted().body(Map.of("status","ACCEPTED"));}
    @GetMapping("/logs") List<AlertLog> logs(@RequestHeader("X-Gateway-Secret")String supplied,@RequestHeader("X-User-Role")String role){check(supplied);if(!role.equalsIgnoreCase("NATIONAL_USER")&&!role.equalsIgnoreCase("PROVINCIAL_ADMIN"))throw new IllegalStateException("Forbidden");return repository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"timestamp"));}
}
@RestControllerAdvice class AlertErrors { @ExceptionHandler(IllegalStateException.class)ResponseEntity<?>forbidden(IllegalStateException e){return ResponseEntity.status(403).body(Map.of("error","FORBIDDEN","message",e.getMessage()));} }
