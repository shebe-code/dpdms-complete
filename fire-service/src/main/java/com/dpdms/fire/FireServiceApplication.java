package com.dpdms.fire;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableAsync
public class FireServiceApplication {
    public static void main(String[] args) { SpringApplication.run(FireServiceApplication.class, args); }

    @Bean
    CommandLineRunner seed(IncidentRepository repository) {
        return args -> {
            if (repository.count() > 0) return;
            HazardIncident i = new HazardIncident();
            i.setWard("Ward 3"); i.setDistrict("Rushinga"); i.setProvince("Mashonaland Central");
            i.setOccurrenceAt(LocalDateTime.now().minusDays(2));
            i.setReporter("fire.recorder"); i.setSeverity(Severity.HIGH);
            i.setLatitude(-16.72); i.setLongitude(31.36);
            i.setAreaBurnedHectares(12.5); i.setSuspectedCause("ACCIDENTAL"); i.setInjuriesFatalities(2); i.setStructuresDestroyed(3); i.setActive(true);
            i.setStatus(IncidentStatus.APPROVED);
            repository.save(i);
        };
    }
}

enum IncidentStatus { PENDING, APPROVED, REJECTED, CORRECTIONS_REQUESTED }
enum Severity { LOW, MEDIUM, HIGH, CRITICAL }

@MappedSuperclass
abstract class BaseIncident {
    @NotBlank private String ward;
    @NotBlank private String district;
    @NotBlank private String province;
    @NotNull private LocalDateTime occurrenceAt;
    @NotBlank private String reporter;
    @NotNull @Enumerated(EnumType.STRING) private Severity severity;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") private Double latitude;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private Double longitude;

    public String getWard(){return ward;} public void setWard(String v){ward=v;}
    public String getDistrict(){return district;} public void setDistrict(String v){district=v;}
    public String getProvince(){return province;} public void setProvince(String v){province=v;}
    public LocalDateTime getOccurrenceAt(){return occurrenceAt;} public void setOccurrenceAt(LocalDateTime v){occurrenceAt=v;}
    public String getReporter(){return reporter;} public void setReporter(String v){reporter=v;}
    public Severity getSeverity(){return severity;} public void setSeverity(Severity v){severity=v;}
    public Double getLatitude(){return latitude;} public void setLatitude(Double v){latitude=v;}
    public Double getLongitude(){return longitude;} public void setLongitude(Double v){longitude=v;}
}

@Entity
@Table(name="fire_incidents")
/*Fields are private (ward, district, province, etc.). Only accessible via public getters/setters. Subclasses can't touch the raw fields directly*/
/*HazardIncident is a BaseIncident. It inherits ward, district, province, occurrenceAt, reporter, severity, latitude, longitude plus their getters/setters.*/    
class HazardIncident extends BaseIncident {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @NotNull @Enumerated(EnumType.STRING) private IncidentStatus status = IncidentStatus.PENDING;
    @NotNull @DecimalMin("0.0")
    private Double areaBurnedHectares;
    @NotBlank
    private String suspectedCause;
    @NotNull @Min(0)
    private Integer injuriesFatalities;
    @NotNull @Min(0)
    private Integer structuresDestroyed;
    @NotNull
    private Boolean active;
    private LocalDateTime createdAt, updatedAt;
    protected HazardIncident() {}
    @PrePersist void onCreate(){createdAt=LocalDateTime.now();updatedAt=createdAt;}
    @PreUpdate void onUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public IncidentStatus getStatus(){return status;} public void setStatus(IncidentStatus v){status=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
    public Double getAreaBurnedHectares(){return areaBurnedHectares;}
    public void setAreaBurnedHectares(Double v){areaBurnedHectares=v;}
    public String getSuspectedCause(){return suspectedCause;}
    public void setSuspectedCause(String v){suspectedCause=v;}
    public Integer getInjuriesFatalities(){return injuriesFatalities;}
    public void setInjuriesFatalities(Integer v){injuriesFatalities=v;}
    public Integer getStructuresDestroyed(){return structuresDestroyed;}
    public void setStructuresDestroyed(Integer v){structuresDestroyed=v;}
    public Boolean getActive(){return active;}
    public void setActive(Boolean v){active=v;}
    void copyEditableFrom(HazardIncident source){
        setWard(source.getWard()); setDistrict(source.getDistrict()); setProvince(source.getProvince()); setOccurrenceAt(source.getOccurrenceAt());
        setSeverity(source.getSeverity()); setLatitude(source.getLatitude()); setLongitude(source.getLongitude());
        areaBurnedHectares=source.areaBurnedHectares;
        suspectedCause=source.suspectedCause;
        injuriesFatalities=source.injuriesFatalities;
        structuresDestroyed=source.structuresDestroyed;
        active=source.active;
    }
}

@Entity
@Table(name="fire_audit")
/*All fields private; only getters exposed; constructor is package-private*/
class AuditEntry {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long incidentId;
    private String actorUsername, actorRole, action, oldStatus, newStatus, note;
    private LocalDateTime changedAt;
    protected AuditEntry(){}
    AuditEntry(Long incidentId,String actorUsername,String actorRole,String action,String oldStatus,String newStatus,String note){
        this.incidentId=incidentId;this.actorUsername=actorUsername;this.actorRole=actorRole;this.action=action;this.oldStatus=oldStatus;this.newStatus=newStatus;this.note=note;this.changedAt=LocalDateTime.now();
    }
    public Long getId(){return id;} public Long getIncidentId(){return incidentId;} public String getActorUsername(){return actorUsername;}
    public String getActorRole(){return actorRole;} public String getAction(){return action;} public String getOldStatus(){return oldStatus;} public String getNewStatus(){return newStatus;}
    public String getNote(){return note;} public LocalDateTime getChangedAt(){return changedAt;}
}

@Repository interface IncidentRepository extends org.springframework.data.jpa.repository.JpaRepository<HazardIncident,Long>{}
@Repository interface AuditRepository extends org.springframework.data.jpa.repository.JpaRepository<AuditEntry,Long>{List<AuditEntry> findByIncidentIdOrderByChangedAtAsc(Long incidentId);}
/*This is the strongest example. IncidentService is injected with a HazardRules reference. If you add class FloodRules implements HazardRules, the service code never changes — it just calls rules.triggersAlert(i) and Java dispatches to the right implementation at runtime*/
interface HazardRules {
    String hazard();
    boolean triggersAlert(HazardIncident incident);
    String alertReason(HazardIncident incident);
}
@Component
    /*hazard rules uses abstraction Declares what a hazard does (hazard(), triggersAlert(), alertReason()) without saying how. The service only knows it has "rules," not fire-specific logic*/
class FireServiceRules implements HazardRules {
    public String hazard(){return "FIRE";}
    public boolean triggersAlert(HazardIncident i){return Boolean.TRUE.equals(i.getActive())&&((i.getInjuriesFatalities()!=null&&i.getInjuriesFatalities()>0)||(i.getAreaBurnedHectares()!=null&&i.getAreaBurnedHectares()>=10.0));}
    public String alertReason(HazardIncident i){return "Fire alert: the fire remains active and meets the defined casualty or area threshold.";}
}

final class SecurityScope {
    private SecurityScope(){}
    static String norm(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT);}
    static boolean national(String role){return norm(role).equals("NATIONAL_USER");}
    static boolean admin(String role){return norm(role).equals("PROVINCIAL_ADMIN");}
    static boolean recorder(String role,String hazard){return norm(role).equals(norm(hazard)+"_RECORDER");}
    static boolean supervisor(String role,String hazard){return norm(role).equals(norm(hazard)+"_SUPERVISOR");}
    static boolean reader(String role,String userHazard,String hazard){return national(role)||admin(role)||(recorder(role,hazard)||supervisor(role,hazard))&&norm(userHazard).equals(norm(hazard));}
    static void requireGateway(String actual,String expected){if(!Objects.equals(actual,expected))throw new ForbiddenException("Direct service access is not permitted.");}
    static void requireReader(String role,String uh,String hazard){if(!reader(role,uh,hazard))throw new ForbiddenException("You are not authorised for the FIRE service.");}
    static void requireRecorder(String role,String uh,String hazard){if(!(admin(role)||recorder(role,hazard))||(!admin(role)&&!norm(uh).equals(norm(hazard))))throw new ForbiddenException("Only the authorised FIRE recorder or provincial administrator may write.");}
    static void requireSupervisor(String role,String uh,String hazard){if(!(admin(role)||supervisor(role,hazard))||(!admin(role)&&!norm(uh).equals(norm(hazard))))throw new ForbiddenException("Only the FIRE supervisor or provincial administrator may approve.");}
}
class ForbiddenException extends RuntimeException{ForbiddenException(String message){super(message);}}
class InvalidWorkflowException extends RuntimeException{InvalidWorkflowException(String message){super(message);}}

final class WorkflowPolicy {
    private WorkflowPolicy(){}
    static IncidentStatus next(IncidentStatus current,String action,String note){
        String reason=note==null?"":note.trim();
        if(current!=IncidentStatus.PENDING&&current!=IncidentStatus.CORRECTIONS_REQUESTED)throw new InvalidWorkflowException("Only pending or correction-requested incidents may change workflow state.");
        return switch(action.toUpperCase(Locale.ROOT)){
            case "APPROVE" -> IncidentStatus.APPROVED;
            case "REJECT" -> {if(reason.isBlank())throw new InvalidWorkflowException("A rejection reason is required.");yield IncidentStatus.REJECTED;}
            case "REQUEST_CORRECTIONS" -> {if(reason.isBlank())throw new InvalidWorkflowException("A correction request reason is required.");yield IncidentStatus.CORRECTIONS_REQUESTED;}
            default -> throw new InvalidWorkflowException("Unknown workflow action.");
        };
    }
}

@Component
class AlertNotifier {
    private final ObjectProvider<DiscoveryClient> provider;
    private final RestClient.Builder builder;
    private final String gatewaySecret;
    AlertNotifier(ObjectProvider<DiscoveryClient> provider,RestClient.Builder builder,@Value("${dpdms.gateway-secret}")String gatewaySecret){this.provider=provider;this.builder=builder;this.gatewaySecret=gatewaySecret;}
    @Async
    void notifyIfNeeded(HazardIncident incident,String reason){
        try{
            DiscoveryClient discovery=provider.getIfAvailable();
            if(discovery==null)return;
            List<ServiceInstance> instances=discovery.getInstances("ALERT-SERVICE");
            if(instances.isEmpty())return;
            Map<String,Object> body=Map.of("incidentId",incident.getId(),"hazard","FIRE","severity",incident.getSeverity().name(),"message",reason);
            builder.build().post().uri(instances.get(0).getUri()+"/api/v1/alerts").header("X-Gateway-Secret",gatewaySecret)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        }catch(Exception ignored){}
    }
}

@Service
class IncidentService {
    /*uses abstraction the query implementation is entirely hidden*/
    private final IncidentRepository repository; private final AuditRepository audit; private final HazardRules rules; private final AlertNotifier notifier;
    IncidentService(IncidentRepository repository,AuditRepository audit,HazardRules rules,AlertNotifier notifier){this.repository=repository;this.audit=audit;this.rules=rules;this.notifier=notifier;}

    HazardIncident create(HazardIncident incoming,String role,String userHazard,String username,String ward,String district,String province){
        SecurityScope.requireRecorder(role,userHazard,rules.hazard());
        if(!SecurityScope.admin(role)){incoming.setWard(ward);incoming.setDistrict(district);incoming.setProvince(province);}
        incoming.setReporter(username);incoming.setStatus(IncidentStatus.PENDING);
        HazardIncident saved=repository.save(incoming);
        audit.save(new AuditEntry(saved.getId(),username,role,"CREATED",null,"PENDING","Incident captured at ward level."));
        return saved;
    }

    List<HazardIncident> list(String role,String userHazard,String username,String ward,String province,String status,String severity,String district,LocalDateTime from,LocalDateTime to){
        SecurityScope.requireReader(role,userHazard,rules.hazard());
        IncidentStatus requestedStatus=status==null||status.isBlank()?null:IncidentStatus.valueOf(status.toUpperCase(Locale.ROOT));
        Severity requestedSeverity=severity==null||severity.isBlank()?null:Severity.valueOf(severity.toUpperCase(Locale.ROOT));
        return repository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"occurrenceAt")).stream()
            .filter(i->{
                if(SecurityScope.national(role)) return i.getStatus()==IncidentStatus.APPROVED;
                if(SecurityScope.admin(role)) return Objects.equals(i.getProvince(),province);
                if(SecurityScope.recorder(role,rules.hazard())) return Objects.equals(i.getReporter(),username)&&Objects.equals(i.getWard(),ward)&&Objects.equals(i.getProvince(),province);
                return Objects.equals(i.getProvince(),province);
            })
            .filter(i->requestedStatus==null||i.getStatus()==requestedStatus)
            .filter(i->requestedSeverity==null||i.getSeverity()==requestedSeverity)
            .filter(i->district==null||district.isBlank()||Objects.equals(i.getDistrict(),district))
            .filter(i->from==null||!i.getOccurrenceAt().isBefore(from))
            .filter(i->to==null||!i.getOccurrenceAt().isAfter(to))
            .collect(Collectors.toList());
    }

    HazardIncident get(Long id,String role,String userHazard,String username,String ward,String province){
        return list(role,userHazard,username,ward,province,null,null,null,null,null).stream().filter(i->Objects.equals(i.getId(),id)).findFirst()
            .orElseThrow(()->new NoSuchElementException("Incident not found."));
    }

    HazardIncident update(Long id,HazardIncident incoming,String role,String userHazard,String username,String ward,String province){
        HazardIncident current=get(id,role,userHazard,username,ward,province);SecurityScope.requireRecorder(role,userHazard,rules.hazard());
        if(!SecurityScope.admin(role)&&!Objects.equals(current.getReporter(),username))throw new ForbiddenException("You may only edit incidents that you created.");
        if(!SecurityScope.admin(role)&&current.getStatus()!=IncidentStatus.PENDING&&current.getStatus()!=IncidentStatus.CORRECTIONS_REQUESTED)throw new InvalidWorkflowException("Only pending or correction-requested incidents may be edited.");
        IncidentStatus old=current.getStatus();current.copyEditableFrom(incoming);HazardIncident saved=repository.save(current);
        audit.save(new AuditEntry(saved.getId(),username,role,"UPDATED",old.name(),old.name(),"Incident details updated."));return saved;
    }

    void delete(Long id,String role,String userHazard,String username,String ward,String province){
        HazardIncident current=get(id,role,userHazard,username,ward,province);SecurityScope.requireRecorder(role,userHazard,rules.hazard());
        if(!SecurityScope.admin(role)&&!Objects.equals(current.getReporter(),username))throw new ForbiddenException("You may only delete incidents that you created.");
        if(!SecurityScope.admin(role)&&current.getStatus()!=IncidentStatus.PENDING&&current.getStatus()!=IncidentStatus.CORRECTIONS_REQUESTED)throw new InvalidWorkflowException("Only pending or correction-requested incidents may be deleted.");
        repository.delete(current);audit.save(new AuditEntry(id,username,role,"DELETED",current.getStatus().name(),null,"Incident deleted."));
    }

    HazardIncident transition(Long id,String action,String note,String role,String userHazard,String username,String ward,String province){
        HazardIncident current=get(id,role,userHazard,username,ward,province);SecurityScope.requireSupervisor(role,userHazard,rules.hazard());
        IncidentStatus old=current.getStatus();String reason=note==null?"":note.trim();IncidentStatus next=WorkflowPolicy.next(old,action,reason);
        current.setStatus(next);HazardIncident saved=repository.save(current);audit.save(new AuditEntry(saved.getId(),username,role,action,old.name(),next.name(),reason));
        if(next==IncidentStatus.APPROVED&&rules.triggersAlert(saved))notifier.notifyIfNeeded(saved,rules.alertReason(saved));return saved;
    }

    List<AuditEntry> auditTrail(Long id,String role,String userHazard,String username,String ward,String province){get(id,role,userHazard,username,ward,province);return audit.findByIncidentIdOrderByChangedAtAsc(id);}
}

@RestController
@RequestMapping("/api/v1/fires")
class IncidentController {
    private final IncidentService service;private final String secret;
    IncidentController(IncidentService service,@Value("${dpdms.gateway-secret}")String secret){this.service=service;this.secret=secret;}
    private void check(String actual){SecurityScope.requireGateway(actual,secret);}
    private String text(String value){return value==null?"":value;}
    @PostMapping ResponseEntity<HazardIncident> create(@RequestHeader("X-Gateway-Secret")String gs,@RequestHeader("X-User-Role")String role,@RequestHeader("X-User-Hazard")String uh,@RequestHeader("X-User-Username")String user,@RequestHeader(value="X-User-Ward",required=false)String ward,@RequestHeader(value="X-User-District",required=false)String district,@RequestHeader(value="X-User-Province",required=false)String province,@RequestBody @jakarta.validation.Valid HazardIncident request){check(gs);return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request,role,uh,user,text(ward),text(district),text(province)));}
    @GetMapping List<HazardIncident> list(@RequestHeader("X-Gateway-Secret")String gs,@RequestHeader("X-User-Role")String role,@RequestHeader("X-User-Hazard")String uh,@RequestHeader("X-User-Username")String user,@RequestHeader(value="X-User-Ward",required=false)String ward,@RequestHeader(value="X-User-Province",required=false)String province,@RequestParam(required=false)String status,@RequestParam(required=false)String severity,@RequestParam(required=false)String district,@RequestParam(required=false)LocalDateTime from,@RequestParam(required=false)LocalDateTime to){check(gs);return service.list(role,uh,user,text(ward),text(province),status,severity,district,from,to);}
    @GetMapping("/approved") List<HazardIncident> approved(@RequestHeader("X-Gateway-Secret")String gs,@RequestHeader("X-User-Role")String role,@RequestHeader("X-User-Hazard")String uh,@RequestHeader("X-User-Username")String user,@RequestHeader(value="X-User-Ward",required=false)String ward,@RequestHeader(value="X-User-Province",required=false)String province){check(gs);return service.list(role,uh,user,text(ward),text(province),"APPROVED",null,null,null,null);}
    @GetMapping("/{id}") HazardIncident get(@RequestHeader("X-Gateway-Secret")String gs,@RequestHeader("X-User-Role")String role,@RequestHeader("X-User-Hazard")String uh,@RequestHeader("X-User-Username")String user,@RequestHeader(value="X-User-Ward",required=false)String ward,@RequestHeader(value="X-User-Province",required=false)String province,@PathVariable Long id){check(gs);return service.get(id,role,uh,user,text(ward),text(province));}
    @PutMapping("/{id}") HazardIncident update(@RequestHeader("X-Gateway-Secret")String gs,@RequestHeader("X-User-Role")String role,@RequestHeader("X-User-Hazard")String uh,@RequestHeader("X-User-Username")String user,@RequestHeader(value="X-User-Ward",required=false)String ward,@RequestHeader(value="X-User-Province",required=false)String province,@PathVariable Long id,@RequestBody @jakarta.validation.Valid HazardIncident request){check(gs);return service.update(id,request,role,uh,user,text(ward),text(province));}
    @DeleteMapping("/{id}") ResponseEntity<Void> delete(@RequestHeader("X-Gateway-Secret")String gs,@RequestHeader("X-User-Role")String role,@RequestHeader("X-User-Hazard")String uh,@RequestHeader("X-User-Username")String user,@RequestHeader(value="X-User-Ward",required=false)String ward,@RequestHeader(value="X-User-Province",required=false)String province,@PathVariable Long id){check(gs);service.delete(id,role,uh,user,text(ward),text(province));return ResponseEntity.noContent().build();}
    @PostMapping("/{id}/approve") HazardIncident approve(@RequestHeader("X-Gateway-Secret")String gs,@RequestHeader("X-User-Role")String role,@RequestHeader("X-User-Hazard")String uh,@RequestHeader("X-User-Username")String user,@RequestHeader(value="X-User-Ward",required=false)String ward,@RequestHeader(value="X-User-Province",required=false)String province,@PathVariable Long id){check(gs);return service.transition(id,"APPROVE","",role,uh,user,text(ward),text(province));}
    @PostMapping("/{id}/reject") HazardIncident reject(@RequestHeader("X-Gateway-Secret")String gs,@RequestHeader("X-User-Role")String role,@RequestHeader("X-User-Hazard")String uh,@RequestHeader("X-User-Username")String user,@RequestHeader(value="X-User-Ward",required=false)String ward,@RequestHeader(value="X-User-Province",required=false)String province,@PathVariable Long id,@RequestParam String reason){check(gs);return service.transition(id,"REJECT",reason,role,uh,user,text(ward),text(province));}
    @PostMapping("/{id}/corrections") HazardIncident corrections(@RequestHeader("X-Gateway-Secret")String gs,@RequestHeader("X-User-Role")String role,@RequestHeader("X-User-Hazard")String uh,@RequestHeader("X-User-Username")String user,@RequestHeader(value="X-User-Ward",required=false)String ward,@RequestHeader(value="X-User-Province",required=false)String province,@PathVariable Long id,@RequestParam String reason){check(gs);return service.transition(id,"REQUEST_CORRECTIONS",reason,role,uh,user,text(ward),text(province));}
    @GetMapping("/{id}/audit") List<AuditEntry> audit(@RequestHeader("X-Gateway-Secret")String gs,@RequestHeader("X-User-Role")String role,@RequestHeader("X-User-Hazard")String uh,@RequestHeader("X-User-Username")String user,@RequestHeader(value="X-User-Ward",required=false)String ward,@RequestHeader(value="X-User-Province",required=false)String province,@PathVariable Long id){check(gs);return service.auditTrail(id,role,uh,user,text(ward),text(province));}
}

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(ForbiddenException.class) ResponseEntity<?> forbidden(ForbiddenException e){return ResponseEntity.status(403).body(Map.of("error","FORBIDDEN","message",e.getMessage()));}
    @ExceptionHandler({InvalidWorkflowException.class,IllegalArgumentException.class}) ResponseEntity<?> bad(RuntimeException e){return ResponseEntity.badRequest().body(Map.of("error","BAD_REQUEST","message",e.getMessage()));}
    @ExceptionHandler(NoSuchElementException.class) ResponseEntity<?> missing(NoSuchElementException e){return ResponseEntity.status(404).body(Map.of("error","NOT_FOUND","message",e.getMessage()));}
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){
        String message=e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).findFirst().orElse("Invalid request.");
        return ResponseEntity.badRequest().body(Map.of("error","VALIDATION_ERROR","message",message));
    }
    @ExceptionHandler(Exception.class) ResponseEntity<?> generic(Exception e){return ResponseEntity.status(500).body(Map.of("error","INTERNAL_ERROR","message","Unexpected server error."));}
}
