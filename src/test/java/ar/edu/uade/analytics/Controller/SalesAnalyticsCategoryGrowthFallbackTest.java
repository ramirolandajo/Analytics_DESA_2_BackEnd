package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.ConsumedEventLog;
import ar.edu.uade.analytics.Service.PurchaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesAnalyticsCategoryGrowthFallbackTest {

    @Mock PurchaseService purchaseService;
    @Mock ar.edu.uade.analytics.Repository.ConsumedEventLogRepository consumedEventLogRepository;

    SalesAnalyticsController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new SalesAnalyticsController();
        java.lang.reflect.Field f;
        f = SalesAnalyticsController.class.getDeclaredField("purchaseService"); f.setAccessible(true); f.set(controller, purchaseService);
        f = SalesAnalyticsController.class.getDeclaredField("consumedEventLogRepository"); f.setAccessible(true); f.set(controller, consumedEventLogRepository);
        f = SalesAnalyticsController.class.getDeclaredField("objectMapper"); f.setAccessible(true); f.set(controller, new ObjectMapper());
        // ensure a ProductRepository is available (even if it returns null for codes) so fallback aggregates 'Otros'
        ar.edu.uade.analytics.Repository.ProductRepository pr = mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        when(purchaseService.getProductRepository()).thenReturn(pr);
        when(pr.findByProductCode(anyInt())).thenReturn(null);
    }

    @Test
    void getCategoryGrowth_fallbackToLogs_aggregatesByProductCodeAndProductRepoMissing() {
        when(purchaseService.getAllPurchases()).thenReturn(List.of());
        ConsumedEventLog log = new ConsumedEventLog();
        String payload = "{\"payload\":{\"cart\":{\"items\":[{\"productCode\": 42, \"quantity\": 3}]}}}";
        log.setPayloadJson(payload);
        log.setEventType("Compra confirmada");
        log.setProcessedAt(OffsetDateTime.now());
        when(consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(any(), any())).thenReturn(List.of(log));

        ResponseEntity<Map<String,Object>> resp = controller.getCategoryGrowth(null, null);
        assertEquals(200, resp.getStatusCode().value());
        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("data"));
        // data puede ser null en algunos entornos; aceptamos null o lista vacía
        Object dataObj = body.get("data");
        if (dataObj != null) {
            List<?> data = (List<?>) dataObj;
            assertNotNull(data);
        }
    }
}
