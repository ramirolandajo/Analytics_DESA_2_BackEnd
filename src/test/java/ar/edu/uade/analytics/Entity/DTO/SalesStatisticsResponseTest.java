package ar.edu.uade.analytics.Entity.DTO;

import ar.edu.uade.analytics.Controller.SalesStatisticsResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SalesStatisticsResponseTest {
    @Test
    void testSettersAndGetters() {
        SalesStatisticsResponse resp = new SalesStatisticsResponse();
        resp.setTotalVentas(10);
        resp.setFacturacionTotal(123.45f);
        resp.setProductosVendidos(7);
        resp.setChartBase64("abc123");
        assertEquals(10, resp.getTotalVentas());
        assertEquals(123.45f, resp.getFacturacionTotal());
        assertEquals(7, resp.getProductosVendidos());
        assertEquals("abc123", resp.getChartBase64());
    }

    @Test
    void testNoArgsConstructor() {
        SalesStatisticsResponse resp = new SalesStatisticsResponse();
        assertNull(resp.getTotalVentas());
        assertNull(resp.getFacturacionTotal());
        assertNull(resp.getProductosVendidos());
        assertNull(resp.getChartBase64());
    }
}

