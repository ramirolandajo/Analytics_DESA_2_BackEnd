package ar.edu.uade.analytics.Entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    @Test
    void constructorSetsTypeAndPayloadAndTimestamp() {
        Event e = new Event("TYP", "PAYLOAD");
        assertEquals("TYP", e.getType());
        assertEquals("PAYLOAD", e.getPayload());
        assertNotNull(e.getTimestamp());
        assertTrue(e.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void toStringContainsTypePayloadTimestamp() {
        Event e = new Event("T", "P");
        String s = e.toString();
        assertTrue(s.contains("T"));
        assertTrue(s.contains("P"));
        assertTrue(s.contains("timestamp="));
    }

    @Test
    void defaultConstructorAllowsSetters() {
        Event e = new Event();
        e.setType("X");
        e.setPayload("Y");
        e.setTimestamp(LocalDateTime.of(2020,1,1,0,0));
        assertEquals("X", e.getType());
        assertEquals("Y", e.getPayload());
        assertEquals(LocalDateTime.of(2020,1,1,0,0), e.getTimestamp());
    }
}

