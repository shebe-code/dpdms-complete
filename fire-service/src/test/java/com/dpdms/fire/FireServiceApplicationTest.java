package com.dpdms.fire;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.http.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FireServiceApplicationTest {
    @Autowired TestRestTemplate http;
    @Test void healthEndpointWorks() { var response=http.getForEntity("/actuator/health",String.class); assertEquals(HttpStatus.OK,response.getStatusCode()); }
    @Test void directServiceAccessIsForbidden() { var h=new HttpHeaders();h.set("X-Gateway-Secret","wrong-secret");h.set("X-User-Role","NATIONAL_USER");h.set("X-User-Hazard","FIRE");h.set("X-User-Username","test-user");var response=http.exchange("/api/v1/fires",HttpMethod.GET,new HttpEntity<Void>(h),String.class);assertEquals(HttpStatus.FORBIDDEN,response.getStatusCode()); }
    @Test void wrongHazardRecorderIsDenied() { assertFalse(SecurityScope.recorder("FLOOD_RECORDER","FIRE")); }
    @Test void wrongHazardSupervisorIsDenied() { assertFalse(SecurityScope.supervisor("FLOOD_SUPERVISOR","FIRE")); }
    @Test void nationalIsNotAWriter() { assertFalse(SecurityScope.recorder("NATIONAL_USER","FIRE")); assertFalse(SecurityScope.supervisor("NATIONAL_USER","FIRE")); }
    @Test void approvalPolicyWorks() { assertEquals(IncidentStatus.APPROVED,WorkflowPolicy.next(IncidentStatus.PENDING,"APPROVE","")); assertEquals(IncidentStatus.REJECTED,WorkflowPolicy.next(IncidentStatus.PENDING,"REJECT","insufficient evidence")); assertEquals(IncidentStatus.CORRECTIONS_REQUESTED,WorkflowPolicy.next(IncidentStatus.PENDING,"REQUEST_CORRECTIONS","fix coordinates")); }
    @Test void rejectionRequiresReason() { assertThrows(InvalidWorkflowException.class,()->WorkflowPolicy.next(IncidentStatus.PENDING,"REJECT","")); }
}
