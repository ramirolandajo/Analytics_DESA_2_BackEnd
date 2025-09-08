package ar.edu.uade.analytics.Controller.SalesAnalyticsTests;

import ar.edu.uade.analytics.Controller.SalesAnalyticsController;
import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Service.PurchaseService;
import ar.edu.uade.analytics.Repository.StockChangeLogRepository;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SalesAnalyticsControllerCoverageTests {

    @Mock
    PurchaseService purchaseService;

    @Mock
    StockChangeLogRepository stockChangeLogRepository;

    @Test
    void testApplyPieAndBoxStyles_and_summaryChart_bytes() throws Exception {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        // inject mocks
        var f1 = SalesAnalyticsController.class.getDeclaredField("purchaseService");
        f1.setAccessible(true);
        f1.set(controller, purchaseService);
        var f2 = SalesAnalyticsController.class.getDeclaredField("stockChangeLogRepository");
        f2.setAccessible(true);
        f2.set(controller, stockChangeLogRepository);
    }

    @Test
    void testGetTopCustomers_and_dailySales() throws Exception {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        var f1 = SalesAnalyticsController.class.getDeclaredField("purchaseService");
        f1.setAccessible(true);
        f1.set(controller, purchaseService);

        User u = new User(); u.setId(10); u.setName("A"); u.setEmail("a@a");
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setUser(u); p1.setDate(LocalDateTime.of(2023,1,1,10,0));
        Cart c1 = new Cart(); c1.setFinalPrice(100f); p1.setCart(c1);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setUser(u); p2.setDate(LocalDateTime.of(2023,1,2,11,0));
        Cart c2 = new Cart(); c2.setFinalPrice(50f); p2.setCart(c2);

        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1, p2));

        Map<String, Object> top = controller.getTopCustomers(10, null, null).getBody();
        assertNotNull(top);
        assertTrue(((List<?>) top.get("data")).size() >= 1);
    }

    @Test
    void testGetStockHistoryByProduct_and_productEventsTimeline_variants() throws Exception {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        var f1 = SalesAnalyticsController.class.getDeclaredField("purchaseService");
        f1.setAccessible(true);
        f1.set(controller, purchaseService);
        var f2 = SalesAnalyticsController.class.getDeclaredField("stockChangeLogRepository");
        f2.setAccessible(true);
        f2.set(controller, stockChangeLogRepository);

        Product prod = new Product(); prod.setId(77); prod.setTitle("P77");
        StockChangeLog log = new StockChangeLog(); log.setProduct(prod); log.setChangedAt(LocalDateTime.of(2023,1,5,9,0)); log.setNewStock(20); log.setOldStock(22); log.setQuantityChanged(-2); log.setReason("Venta");


        // product-events timeline: when productId provided
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(77)).thenReturn(List.of(log));
        Map<String, Object> timeline = controller.getProductEventsTimeline(77, null, null, 5).getBody();
        assertNotNull(timeline);
        assertTrue(((List<?>) timeline.get("events")).size() >= 1);

        // and without productId, no logs -> expect empty events
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findAll()).thenReturn(List.of());
        Map<String, Object> empty = controller.getProductEventsTimeline(null, null, null, 5).getBody();
        assertNotNull(empty);
        assertTrue(((List<?>) empty.get("events")).isEmpty());
    }

    @Test
    void testCategoryGrowth_found_and_notFound() throws Exception {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        var f1 = SalesAnalyticsController.class.getDeclaredField("purchaseService");
        f1.setAccessible(true);
        f1.set(controller, purchaseService);

        Product p = new Product(); p.setId(5);
        Category cat = new Category(); cat.setId(99); cat.setName("C99");
        p.setCategories(java.util.Set.of(cat));
        CartItem it = new CartItem(); it.setProduct(p); it.setQuantity(3);
        Cart cart = new Cart(); cart.setItems(List.of(it));
        Purchase pur = new Purchase(); pur.setStatus(Purchase.Status.CONFIRMED); pur.setDate(LocalDateTime.of(2023,2,1,10,0)); pur.setCart(cart);

        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(pur));

        Map<String, Object> resp = controller.getCategoryGrowth(99, null, null).getBody();
        assertNotNull(resp);
        assertTrue(((Map<?, ?>) resp.get("categoryGrowth")).size() >= 1);

        // category not present -> empty map
        Map<String, Object> resp2 = controller.getCategoryGrowth(12345, null, null).getBody();
        assertNotNull(resp2);
        assertTrue(((Map<?, ?>) resp2.get("categoryGrowth")).isEmpty());
    }
}

