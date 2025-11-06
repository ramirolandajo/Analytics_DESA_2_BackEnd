package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Communication.KafkaMockService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaMockServiceFullEditTest {

    @Test
    void fullEditMock_containsBrandAndCategories() {
        KafkaMockService s = new KafkaMockService();
        var msg = s.getEditProductMockFull();
        assertNotNull(msg);
        assertNotNull(msg.payload);
        assertNotNull(msg.payload.getBrand());
        assertNotNull(msg.payload.getCategories());
        assertFalse(msg.payload.getCategories().isEmpty());
    }
}
