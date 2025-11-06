package ar.edu.uade.analytics.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaMockServiceTest {
    @Test
    void saleEventMock_buildsNonNull() {
        KafkaMockService s = new KafkaMockService();
        var m = s.getSaleEventMock();
        assertNotNull(m);
        assertNotNull(m.payload);
        assertEquals("StockConfirmed_CartPurchase", m.type);
    }
}

