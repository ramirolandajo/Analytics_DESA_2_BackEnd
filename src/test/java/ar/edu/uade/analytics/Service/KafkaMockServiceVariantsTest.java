package ar.edu.uade.analytics.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaMockServiceVariantsTest {

    @Test
    void multipleSaleMocks_existAndHavePayloads() {
        KafkaMockService s = new KafkaMockService();
        var m1 = s.getSaleEventMock();
        var m2 = s.getSaleEventMock14();
        assertNotNull(m1);
        assertNotNull(m2);
        assertNotNull(m1.type);
        assertNotNull(m2.type);
        assertNotNull(m1.payload);
        assertNotNull(m2.payload);
    }
}

