package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesAnalyticsControllerTrendTest {
    @Mock PurchaseService purchaseService;
    SalesAnalyticsController ctrl;

    @BeforeEach
    void setUp() throws Exception {
        ctrl = new SalesAnalyticsController();
        java.lang.reflect.Field f = SalesAnalyticsController.class.getDeclaredField("purchaseService"); f.setAccessible(true); f.set(ctrl, purchaseService);
    }

    @Test
    void getTrend_computesCurrentAndPrevious() {
        // create purchases: one in current range, one in previous
        LocalDateTime now = LocalDateTime.now();
        Purchase pCurr = new Purchase(); pCurr.setStatus(Purchase.Status.CONFIRMED); pCurr.setDate(now);
        Purchase pPrev = new Purchase(); pPrev.setStatus(Purchase.Status.CONFIRMED); pPrev.setDate(now.minusDays(31));
        when(purchaseService.getAllPurchases()).thenReturn(List.of(pCurr, pPrev));

        Map<String,Object> res = ctrl.getTrend(now.minusDays(29), now);
        assertTrue(res.containsKey("current"));
        assertTrue(res.containsKey("previous"));

        @SuppressWarnings("unchecked")
        var current = (List<Map<String,Object>>) res.get("current");
        // current should have at least one day with ventas>0
        boolean hasSales = current.stream().anyMatch(m -> ((Number)m.get("ventas")).intValue() > 0);
        assertTrue(hasSales);
    }
}
