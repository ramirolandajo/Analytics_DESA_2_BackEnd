package ar.edu.uade.analytics.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KafkaControllerMalformed2Test {

    KafkaController controller;
    ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        controller = new KafkaController();
        java.lang.reflect.Field f = KafkaController.class.getDeclaredField("objectMapper"); f.setAccessible(true); f.set(controller, om);
    }

    @Test
    void receiveEvent_missingType_returnsBadRequest() {
        String json = "{ \"payload\": {\"foo\":1} }";
        ResponseEntity<String> resp = controller.receiveEvent(json);
        assertEquals(400, resp.getStatusCode().value());
        String body = resp.getBody();
        assertNotNull(body);
        assertTrue(body.contains("Error procesando evento"));
    }
}
