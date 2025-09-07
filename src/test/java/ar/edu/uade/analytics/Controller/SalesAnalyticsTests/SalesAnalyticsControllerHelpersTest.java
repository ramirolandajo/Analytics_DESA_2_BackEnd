package ar.edu.uade.analytics.Controller.SalesAnalyticsTests;

import ar.edu.uade.analytics.Controller.SalesAnalyticsController;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.StockChangeLog;
import ar.edu.uade.analytics.Service.PurchaseService;
import ar.edu.uade.analytics.Repository.StockChangeLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SalesAnalyticsControllerHelpersTest {

    @Mock
    PurchaseService purchaseService;

    @Mock
    StockChangeLogRepository stockChangeLogRepository;

    private SalesAnalyticsController prepareController() throws Exception {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        var f1 = SalesAnalyticsController.class.getDeclaredField("purchaseService");
        f1.setAccessible(true);
        f1.set(controller, purchaseService);
        var f2 = SalesAnalyticsController.class.getDeclaredField("stockChangeLogRepository");
        f2.setAccessible(true);
        f2.set(controller, stockChangeLogRepository);
        return controller;
    }

    @Test
    void buildTimelineFromLogs_empty_returnsEmptyEventsAndNullChart() throws Exception {
        SalesAnalyticsController controller = prepareController();
        Map<String, Object> resp = controller.buildTimelineFromLogs(List.of());
        assertNotNull(resp);
        assertTrue(resp.containsKey("events"));
        assertTrue(((List<?>) resp.get("events")).isEmpty());
        assertNull(resp.get("chartBase64"));
    }

    @Test
    void buildTimelineFromLogs_sameDay_expandsRange_and_generatesChart() throws Exception {
        SalesAnalyticsController controller = prepareController();
        Product p = new Product(); p.setId(123); p.setTitle("P123");
        StockChangeLog log = new StockChangeLog();
        log.setProduct(p);
        log.setChangedAt(LocalDateTime.of(2023, 8, 10, 10, 0));
        log.setOldStock(10);
        log.setNewStock(9);
        log.setQuantityChanged(-1);
        log.setReason("Ajuste");
        Map<String, Object> resp = controller.buildTimelineFromLogs(List.of(log));
        assertNotNull(resp);
        assertTrue(((List<?>) resp.get("events")).size() == 1);
        // chartBase64 should be present (non-null) when there is at least one event
        assertNotNull(resp.get("chartBase64"));
    }

    @Test
    void buildTimelineFromLogs_shortRange_and_multipleProducts_generatesChart() throws Exception {
        SalesAnalyticsController controller = prepareController();
        Product p1 = new Product(); p1.setId(201); p1.setTitle("P1");
        Product p2 = new Product(); p2.setId(202); p2.setTitle("P2");
        StockChangeLog l1 = new StockChangeLog(); l1.setProduct(p1); l1.setChangedAt(LocalDateTime.of(2023,9,1,9,0)); l1.setNewStock(5); l1.setOldStock(6); l1.setQuantityChanged(-1); l1.setReason("Venta");
        StockChangeLog l2 = new StockChangeLog(); l2.setProduct(p2); l2.setChangedAt(LocalDateTime.of(2023,9,3,9,0)); l2.setNewStock(7); l2.setOldStock(8); l2.setQuantityChanged(-1); l2.setReason("Venta");
        Map<String, Object> resp = controller.buildTimelineFromLogs(List.of(l1, l2));
        assertNotNull(resp);
        assertTrue(((List<?>) resp.get("events")).size() == 2);
        assertNotNull(resp.get("chartBase64"));
    }

    @Test
    void buildEvolutionChartBase64_withLogs_returnsNonNull() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // create logs for product 500 with two dates
        Product p = new Product(); p.setId(500); p.setTitle("Prod500");
        StockChangeLog l1 = new StockChangeLog(); l1.setProduct(p); l1.setChangedAt(LocalDateTime.of(2023,1,1,9,0)); l1.setNewStock(10);
        StockChangeLog l2 = new StockChangeLog(); l2.setProduct(p); l2.setChangedAt(LocalDateTime.of(2023,2,1,9,0)); l2.setNewStock(8);
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(500)).thenReturn(List.of(l1, l2));
        String chart = controller.buildEvolutionChartBase64(List.of(500), null, null);
        assertNotNull(chart);
        assertTrue(chart.length() > 0);
    }
}

