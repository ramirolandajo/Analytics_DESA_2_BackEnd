package ar.edu.uade.analytics.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class KafkaControllerMalformedTest {
    KafkaController ctrl;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        ctrl = new KafkaController();
        Field f = KafkaController.class.getDeclaredField("objectMapper"); f.setAccessible(true); f.set(ctrl, mapper);
    }

    @Test
    void receiveEvent_invalidJson_returnsBadRequest() {
        String bad = "{ invalid json";
        var resp = ctrl.receiveEvent(bad);
        assertEquals(400, resp.getStatusCode().value());
        String body = resp.getBody();
        assertNotNull(body);
        assertTrue(body.contains("Error procesando evento"));
    }
}
