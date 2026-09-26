package com.dpdms.mining;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.http.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class MiningServiceApplicationTest {
    @Autowired TestRestTemplate http;
    @Test void healthEndpointWorks() { var response=http.getForEntity("/actuator/health",String.class); assertEquals(HttpStatus.OK,response.getStatusCode()); }
    @Test void directServiceAccessIsForbidden() { var h=new HttpHeaders();h.set("X-Gateway-Secret","wrong-secret");h.set("X-User-Role","NATIONAL_USER");h.set("X-User-Hazard","MINING");h.set("X-User-Username","test-user");var response=http.exchange("/api/v1/mining-accidents",HttpMethod.GET,new HttpEntity<Void>(h),String.class);assertEquals(HttpStatus.FORBIDDEN,response.getStatusCode()); }
    @Test void wrongHazardRecorderIsDenied() { assertFalse(SecurityScope.recorder("FLOOD_RECORDER","MINING")); }
    @Test void wrongHazardSupervisorIsDenied() { assertFalse(SecurityScope.supervisor("FLOOD_SUPERVISOR","MINING")); }
    @Test void nationalIsNotAWriter() { assertFalse(SecurityScope.recorder("NATIONAL_USER","MINING")); assertFalse(SecurityScope.supervisor("NATIONAL_USER","MINING")); }
    @Test void approvalPolicyWorks() { assertEquals(IncidentStatus.APPROVED,WorkflowPolicy.next(IncidentStatus.PENDING,"APPROVE","")); assertEquals(IncidentStatus.REJECTED,WorkflowPolicy.next(IncidentStatus.PENDING,"REJECT","insufficient evidence")); assertEquals(IncidentStatus.CORRECTIONS_REQUESTED,WorkflowPolicy.next(IncidentStatus.PENDING,"REQUEST_CORRECTIONS","fix coordinates")); }
    @Test void rejectionRequiresReason() { assertThrows(InvalidWorkflowException.class,()->WorkflowPolicy.next(IncidentStatus.PENDING,"REJECT","")); }
}
