package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.Purchase;
import ar.edu.uade.analytics.Service.PurchaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesAnalyticsControllerTest {

    @Mock
    PurchaseService purchaseService;
    @Mock
    ar.edu.uade.analytics.Repository.CartRepository cartRepository;
    @Mock
    ar.edu.uade.analytics.Repository.ProductRepository productRepository; // reservado para tests futuros
    @Mock
    ar.edu.uade.analytics.Repository.ConsumedEventLogRepository consumedEventLogRepository;
    @Mock
    ar.edu.uade.analytics.Repository.StockChangeLogRepository stockChangeLogRepository;

    SalesAnalyticsController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new SalesAnalyticsController();
        java.lang.reflect.Field f;
        f = SalesAnalyticsController.class.getDeclaredField("purchaseService"); f.setAccessible(true); f.set(controller, purchaseService);
        f = SalesAnalyticsController.class.getDeclaredField("cartRepository"); f.setAccessible(true); f.set(controller, cartRepository);
        f = SalesAnalyticsController.class.getDeclaredField("consumedEventLogRepository"); f.setAccessible(true); f.set(controller, consumedEventLogRepository);
        f = SalesAnalyticsController.class.getDeclaredField("stockChangeLogRepository"); f.setAccessible(true); f.set(controller, stockChangeLogRepository);
        f = SalesAnalyticsController.class.getDeclaredField("objectMapper"); f.setAccessible(true); f.set(controller, new ObjectMapper());
    }

    @Test
    void getSalesSummary_fallbackToCarts() {
        when(purchaseService.getAllPurchases()).thenReturn(List.of());
        Cart c = new Cart(); c.setFinalPrice(123f);
        CartItem it = new CartItem(); it.setQuantity(2); c.setItems(List.of(it));
        when(cartRepository.findAll()).thenReturn(List.of(c));

        ResponseEntity<Map<String, Object>> resp = controller.getSalesSummary(null, null);
        assertEquals(200, resp.getStatusCode().value());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body);
        assertEquals(123f, ((Number) body.get("facturacionTotal")).floatValue(), 0.001);
    }

    @Test
    void getTopProducts_fromCarts() {
        when(purchaseService.getAllPurchases()).thenReturn(List.of());
        Product p = new Product(); p.setId(1); p.setTitle("Prod");
        CartItem it = new CartItem(); it.setProduct(p); it.setQuantity(5);
        Cart c = new Cart(); c.setItems(List.of(it));
        when(cartRepository.findAll()).thenReturn(List.of(c));

        ResponseEntity<Map<String, Object>> resp = controller.getTopProducts(10, null, null);
        assertEquals(200, resp.getStatusCode().value());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body);
        List<?> data = (List<?>) body.get("data");
        assertFalse(data.isEmpty());
    }

    @Test
    void getTrend_accumulatesCurrentAndPreviousPeriods() {
        // Create two purchases: one in current range, one in previous range
        LocalDateTime now = LocalDateTime.now();
        Purchase pCurrent = new Purchase(); pCurrent.setId(1); pCurrent.setStatus(Purchase.Status.CONFIRMED); pCurrent.setDate(now);
        Cart cart1 = new Cart(); cart1.setFinalPrice(100f);
        CartItem it1 = new CartItem(); it1.setQuantity(2);
        cart1.setItems(List.of(it1)); pCurrent.setCart(cart1);

        LocalDateTime prev = now.minusDays(31);
        Purchase pPrev = new Purchase(); pPrev.setId(2); pPrev.setStatus(Purchase.Status.CONFIRMED); pPrev.setDate(prev);
        Cart cart2 = new Cart(); cart2.setFinalPrice(50f);
        CartItem it2 = new CartItem(); it2.setQuantity(1);
        cart2.setItems(List.of(it2)); pPrev.setCart(cart2);

        when(purchaseService.getAllPurchases()).thenReturn(List.of(pCurrent, pPrev));

        ResponseEntity<Map<String, Object>> resp = controller.getTrend(null, null);
        assertEquals(200, resp.getStatusCode().value());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("current") && body.containsKey("previous"));
    }

    @Test
    void getLowStock_returnsProducts_whenRepoAvailable() {
        Product p1 = new Product(); p1.setId(10); p1.setTitle("P1"); p1.setStock(5);
        Product p2 = new Product(); p2.setId(11); p2.setTitle("P2"); p2.setStock(20);
        when(purchaseService.getAllPurchases()).thenReturn(List.of());
        when(purchaseService.getProductRepository()).thenReturn(productRepository);
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        ResponseEntity<Map<String, Object>> resp = controller.getLowStockProducts(10, 10);
        assertEquals(200, resp.getStatusCode().value());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body);
        List<?> data = (List<?>) body.get("data");
        assertEquals(1, data.size());
    }

    @Test
    void getTopCategories_fromPurchases_aggregatesCategories() {
        // Build product with category
        ar.edu.uade.analytics.Entity.Category cat = new ar.edu.uade.analytics.Entity.Category(); cat.setId(1); cat.setName("CatX");
        Product prod = new Product(); prod.setId(99); prod.setCategories(java.util.Set.of(cat));
        CartItem it = new CartItem(); it.setProduct(prod); it.setQuantity(3);
        Cart c = new Cart(); c.setItems(List.of(it));
        Purchase p = new Purchase(); p.setId(5); p.setStatus(Purchase.Status.CONFIRMED); p.setDate(LocalDateTime.now()); p.setCart(c);

        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));

        ResponseEntity<Map<String, Object>> resp = controller.getTopCategories(10, null, null, "bar");
        assertEquals(200, resp.getStatusCode().value());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body);
        List<?> data = (List<?>) body.get("data");
        assertFalse(data.isEmpty());
    }

    @Test
    void getTopBrands_fromCarts_whenNoPurchases() {
        when(purchaseService.getAllPurchases()).thenReturn(List.of());
        Product prod = new Product(); prod.setId(1);
        ar.edu.uade.analytics.Entity.Brand b = new ar.edu.uade.analytics.Entity.Brand(); b.setName("B1");
        prod.setBrand(b);
        CartItem it = new CartItem(); it.setProduct(prod); it.setQuantity(3);
        Cart c = new Cart(); c.setItems(List.of(it));
        when(cartRepository.findAll()).thenReturn(List.of(c));

        ResponseEntity<Map<String, Object>> resp = controller.getTopBrands(10, null, null, "bar");
        assertEquals(200, resp.getStatusCode().value());
        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        List<?> data = (List<?>) body.get("data");
        assertFalse(data.isEmpty());
    }

    @Test
    void getDailySales_fallbackToConsumedEventLogs_parsesPayload() {
        when(purchaseService.getAllPurchases()).thenReturn(List.of());
        // crear consumed event log con payload JSON con cart.finalPrice e items
        ar.edu.uade.analytics.Entity.ConsumedEventLog log = new ar.edu.uade.analytics.Entity.ConsumedEventLog();
        String payload = "{\"timestamp\": 1600000000, \"payload\": {\"cart\": {\"finalPrice\": 99.9, \"items\": [{\"quantity\": 2}]}}}";
        log.setPayloadJson(payload);
        log.setProcessedAt(java.time.OffsetDateTime.now());
        when(consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(any(), any())).thenReturn(List.of(log));

        ResponseEntity<Map<String,Object>> resp = controller.getDailySales(null, null);
        assertEquals(200, resp.getStatusCode().value());
        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        List<?> data = (List<?>) body.get("data");
        assertFalse(data.isEmpty());
    }

    @Test
    void getProductEventsTimeline_filtersByProductCode_inLogs() {
        // preparar log con payload que contiene items con productCode
        ar.edu.uade.analytics.Entity.ConsumedEventLog log = new ar.edu.uade.analytics.Entity.ConsumedEventLog();
        String payload = "{\"payload\": {\"cart\": {\"items\": [{\"productCode\": 555}]}, \"timestamp\": 1600000000}}";
        log.setPayloadJson(payload);
        log.setEventType("Compra confirmada");
        log.setProcessedAt(java.time.OffsetDateTime.now());
        when(consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(any(), any())).thenReturn(List.of(log));

        ResponseEntity<Map<String,Object>> resp = controller.getProductEventsTimeline(555, null, null);
        assertEquals(200, resp.getStatusCode().value());
        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        List<?> data = (List<?>) body.get("data");
        assertFalse(data.isEmpty());
    }

    @Test
    void getHistogram_singleValue_returnsSingleBin() {
        Purchase p = new Purchase(); p.setId(10); p.setStatus(Purchase.Status.CONFIRMED);
        Cart c = new Cart(); c.setFinalPrice(42f);
        p.setCart(c);
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));

        ResponseEntity<Map<String, Object>> resp = controller.getSalesHistogram(5);
        assertEquals(200, resp.getStatusCode().value());
        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        List<?> data = (List<?>) body.get("data");
        assertFalse(data.isEmpty());
    }

    @Test
    void getCorrelation_buildsPoints_usingProductRepository() {
        // Purchase with cart items
        Product prod = new Product(); prod.setId(123); prod.setTitle("X"); prod.setProductCode(999);
        CartItem it = new CartItem(); it.setProduct(prod); it.setQuantity(4);
        Cart c = new Cart(); c.setItems(List.of(it));
        Purchase p = new Purchase(); p.setId(20); p.setStatus(Purchase.Status.CONFIRMED); p.setCart(c);
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        when(purchaseService.getProductRepository()).thenReturn(productRepository);
        when(productRepository.findById(123)).thenReturn(java.util.Optional.of(prod));

        ResponseEntity<Map<String, Object>> resp = controller.getCorrelation();
        assertEquals(200, resp.getStatusCode().value());
        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        List<?> data = (List<?>) body.get("data");
        assertFalse(data.isEmpty());
    }

    @Test
    void getStockHistoryByProductCode_returnsBadRequest_whenProductNotFound() {
        when(purchaseService.getProductRepository()).thenReturn(product_repository_stub());
        when(purchaseService.getProductRepository().findByProductCode(555)).thenReturn(null);

        ResponseEntity<Map<String, Object>> resp = controller.getStockHistoryByProductCode(555, false, null, null);
        assertEquals(400, resp.getStatusCode().value());
        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("error"));
    }

    @Test
    void getStockHistoryByProductCode_withLogs_and_showProfitTrue_calculatesProfit() {
        Product prod = new Product(); prod.setId(50); prod.setPrice(10f);
        when(purchaseService.getProductRepository()).thenReturn(productRepository);
        when(productRepository.findByProductCode(777)).thenReturn(prod);
        ar.edu.uade.analytics.Entity.StockChangeLog scl = new ar.edu.uade.analytics.Entity.StockChangeLog();
        scl.setChangedAt(LocalDateTime.now());
        scl.setOldStock(10); scl.setNewStock(8); scl.setQuantityChanged(-2); scl.setReason("Venta");
        when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(prod.getId())).thenReturn(List.of(scl));

        ResponseEntity<Map<String,Object>> resp = controller.getStockHistoryByProductCode(777, true, null, null);
        assertEquals(200, resp.getStatusCode().value());
        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        List<?> data = (List<?>) body.get("data");
        assertFalse(data.isEmpty());
        Map<?,?> row = (Map<?,?>) data.get(0);
        assertTrue(row.containsKey("profit"));
    }

    @Test
    void getTopCustomers_withPurchases_returnsAggregated() {
        // Two purchases from same user
        ar.edu.uade.analytics.Entity.User u = new ar.edu.uade.analytics.Entity.User(); u.setId(100); u.setName("Juan"); u.setEmail("j@e.com");
        Cart c1 = new Cart(); c1.setFinalPrice(10f);
        Purchase p1 = new Purchase(); p1.setId(1); p1.setStatus(Purchase.Status.CONFIRMED); p1.setUser(u); p1.setCart(c1); p1.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p1));

        ResponseEntity<Map<String,Object>> resp = controller.getTopCustomers(10);
        assertEquals(200, resp.getStatusCode().value());
        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        List<?> data = (List<?>) body.get("data");
        assertFalse(data.isEmpty());
    }

    @Test
    void getSalesSummary_withPurchases_countsConfirmedSales() {
        ar.edu.uade.analytics.Entity.User u = new ar.edu.uade.analytics.Entity.User(); u.setId(200);
        Cart c = new Cart(); c.setFinalPrice(500f);
        Purchase p = new Purchase(); p.setId(2); p.setStatus(Purchase.Status.CONFIRMED); p.setUser(u); p.setCart(c); p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));

        ResponseEntity<Map<String,Object>> resp = controller.getSalesSummary(null, null);
        assertEquals(200, resp.getStatusCode().value());
        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        assertEquals(1, ((Number) body.get("totalVentas")).intValue());
        assertEquals(500f, ((Number) body.get("facturacionTotal")).floatValue(), 0.001);
    }

    @Test
    void getLowStock_handlesNullProductRepository_gracefully() {
         when(purchaseService.getProductRepository()).thenReturn(null);
         ResponseEntity<Map<String, Object>> resp = controller.getLowStockProducts(10, 10);
         assertEquals(200, resp.getStatusCode().value());
         Map<String,Object> body = resp.getBody();
         assertNotNull(body);
     }

    @Test
    void getProductEventsTimeline_emptyWhenNoLogs() {
        when(consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(any(), any())).thenReturn(List.of());
        ResponseEntity<Map<String,Object>> resp = controller.getProductEventsTimeline(999, null, null);
        assertEquals(200, resp.getStatusCode().value());
        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        List<?> data = (List<?>) body.get("data");
        assertTrue(data.isEmpty());
    }

    // helper to provide a lightweight product repository stub when needed
    private ar.edu.uade.analytics.Repository.ProductRepository product_repository_stub() {
        return mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
    }
}
