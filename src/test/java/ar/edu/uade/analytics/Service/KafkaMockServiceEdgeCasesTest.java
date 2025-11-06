package ar.edu.uade.analytics.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaMockServiceEdgeCasesTest {

    @Test
    void saleEventMock_hasCartAndItems() {
        KafkaMockService s = new KafkaMockService();
        var msg = s.getSaleEventMock();
        // Basic structural checks
        assertNotNull(msg.payload);
        assertNotNull(msg.payload.cart);
        assertNotNull(msg.payload.cart.items);
        assertTrue(msg.payload.cart.items.size() > 0);
    }
}
