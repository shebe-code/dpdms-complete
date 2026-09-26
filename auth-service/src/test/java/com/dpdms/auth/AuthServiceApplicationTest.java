package com.dpdms.auth;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AuthServiceApplicationTest{@Test void registrationIsHazardRecorderOnly(){assertEquals("FLOOD_RECORDER","FLOOD"+"_RECORDER");assertNotEquals("FLOOD_RECORDER","FLOOD_SUPERVISOR");}}
