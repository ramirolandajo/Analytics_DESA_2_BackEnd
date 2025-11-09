package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Service.SalesAnalyticsController;
import ar.edu.uade.analytics.Service.PurchaseService;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalesAnalyticsControllerUnitTest {

    @Test
    void getSalesSummary_emptyPurchases_returnsZeros() {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        PurchaseService purchaseService = mock(PurchaseService.class);
        when(purchaseService.getAllPurchases()).thenReturn(List.of());
        setPurchaseService(controller, purchaseService);

        var start = java.time.LocalDateTime.now().minusDays(1);
        var end = java.time.LocalDateTime.now();
        Map<String, Object> resp = controller.getSalesSummary(start, end);
        assertNotNull(resp);
        Object total = resp.get("totalVentas"); // key as implemented in controller
        int totalVentas = total == null ? 0 : ((Number) total).intValue();
        assertEquals(0, totalVentas);
    }

    @Test
    void getTrend_handlesEmptyPurchases() {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        PurchaseService purchaseService = mock(PurchaseService.class);
        when(purchaseService.getAllPurchases()).thenReturn(List.of());
        setPurchaseService(controller, purchaseService);

        var start = java.time.LocalDateTime.now().minusDays(1);
        var end = java.time.LocalDateTime.now();
        var resp = controller.getTrend(start, end);
        assertNotNull(resp);
    }

    // helper specific to this test file (avoids analyzer warning about constant parameter)
    static void setPurchaseService(Object target, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField("purchaseService");
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
