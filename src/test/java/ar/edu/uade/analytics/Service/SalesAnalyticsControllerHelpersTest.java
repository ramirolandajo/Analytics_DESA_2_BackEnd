package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SalesAnalyticsControllerHelpersTest {

    @Test
    void computeHistogramFromUserCounts_basic() {
        SalesAnalyticsController ctrl = new SalesAnalyticsController();
        Map<Integer, Integer> map = Map.of(1, 1, 2, 2, 3, 6, 4, 4);
        Map<String, Integer> hist = ctrl.computeHistogramFromUserCounts(map);
        assertNotNull(hist);
        assertTrue(hist.containsKey("1-2") || hist.containsKey("3-5") || hist.containsKey("6+"));
    }

    @Test
    void computeRegressionFromXY_returnsCoefficients() {
        SalesAnalyticsController ctrl = new SalesAnalyticsController();
        List<Float> x = Arrays.asList(1f, 2f, 3f);
        List<Float> y = Arrays.asList(2f, 4f, 6f); // perfect slope 2
        Map<String, Double> reg = ctrl.computeRegressionFromXY(x, y);
        assertNotNull(reg);
        assertTrue(reg.containsKey("a"));
        assertTrue(reg.containsKey("b"));
        assertEquals(2.0, reg.get("b"), 0.1);
    }

    @Test
    void computeProductTrends_variousLengths() {
        SalesAnalyticsController ctrl = new SalesAnalyticsController();
        Map<Integer, List<Integer>> m = new HashMap<>();
        m.put(10, List.of(1));
        m.put(11, List.of(1, 2, 3, 4));
        Map<Integer, Double> trends = ctrl.computeProductTrends(m);
        assertEquals(2, trends.size());
        assertEquals(0.0, trends.get(10)); // less than 2 points -> 0.0
        assertNotNull(trends.get(11));
    }

    @Test
    void computeProductSalesFromPurchases_appliesFilters() {
        SalesAnalyticsController ctrl = new SalesAnalyticsController();
        Purchase p1 = new Purchase(); p1.setId(1); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.now());
        Cart cart1 = new Cart(); cart1.setItems(new ArrayList<>());
        Product prod1 = new Product(); prod1.setId(100); prod1.setCategories(new HashSet<>()); cart1.getItems().add(new CartItem(){ { setProduct(prod1); setQuantity(2); } });
        p1.setCart(cart1);
        // Different product with category
        Category cat = new Category(); cat.setId(7); cat.setName("C7");
        Product prod2 = new Product(); prod2.setId(101); prod2.setCategories(Set.of(cat));
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.now()); Cart c2 = new Cart(); c2.setItems(new ArrayList<>()); c2.getItems().add(new CartItem(){ { setProduct(prod2); setQuantity(3); } }); p2.setCart(c2);
        List<Purchase> purchases = List.of(p1, p2);
        Map<Integer, Integer> resultAll = ctrl.computeProductSalesFromPurchases(purchases, null, null, null, null);
        assertTrue(resultAll.containsKey(100));
        assertTrue(resultAll.containsKey(101));
        Map<Integer, Integer> resultFiltered = ctrl.computeProductSalesFromPurchases(purchases, null, null, 7, null);
        assertFalse(resultFiltered.containsKey(100));
        assertTrue(resultFiltered.containsKey(101));
    }

    @Test
    void computeStockHistoryData_profitAndBounds() {
        SalesAnalyticsController ctrl = new SalesAnalyticsController();
        Product prod = new Product(); prod.setId(50); prod.setPrice(10f);
        StockChangeLog log1 = new StockChangeLog(); log1.setProduct(prod); log1.setChangedAt(LocalDateTime.now()); log1.setOldStock(10); log1.setNewStock(8); log1.setQuantityChanged(-2); log1.setReason("Venta");
        StockChangeLog log2 = new StockChangeLog(); log2.setProduct(prod); log2.setChangedAt(LocalDateTime.now()); log2.setOldStock(8); log2.setNewStock(6); log2.setQuantityChanged(-2); log2.setReason("Other");
        List<StockChangeLog> logs = List.of(log1, log2);
        List<Map<String,Object>> res = ctrl.computeStockHistoryData(prod, logs, true, null, null);
        assertEquals(2, res.size());
        // first should contain profit keys
        assertTrue(res.get(0).containsKey("profit"));
    }

    @Test
    void buildTimelineFromLogs_basic() {
        SalesAnalyticsController ctrl = new SalesAnalyticsController();
        Product prod = new Product(); prod.setId(77); prod.setTitle("T");
        StockChangeLog log = new StockChangeLog(); log.setProduct(prod); log.setChangedAt(LocalDateTime.now()); log.setOldStock(5); log.setNewStock(3); log.setQuantityChanged(-2); log.setReason("Venta");
        Map<String,Object> resp = ctrl.buildTimelineFromLogs(List.of(log));
        assertTrue(resp.containsKey("events"));
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> events = (List<Map<String,Object>>) resp.get("events");
        assertEquals(1, events.size());
        assertEquals("StockChange", events.get(0).get("type"));
    }
}
