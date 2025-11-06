package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.Brand;
import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.Purchase;
import ar.edu.uade.analytics.Entity.Category;
import ar.edu.uade.analytics.Entity.ConsumedEventLog;
import ar.edu.uade.analytics.Entity.StockChangeLog;
import ar.edu.uade.analytics.Entity.User;
import ar.edu.uade.analytics.Service.PurchaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class SalesAnalyticsControllerMoreTest {

    @Mock
    private PurchaseService purchaseService;

    @Mock
    private ar.edu.uade.analytics.Repository.CartRepository cartRepository;

    @Mock
    private ar.edu.uade.analytics.Repository.ConsumedEventLogRepository consumedEventLogRepository;

    @Mock
    private ar.edu.uade.analytics.Repository.StockChangeLogRepository stockChangeLogRepository;

    @Mock
    private ar.edu.uade.analytics.Repository.ProductRepository productRepository;

    private SalesAnalyticsController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new SalesAnalyticsController();
        java.lang.reflect.Field f;
        f = SalesAnalyticsController.class.getDeclaredField("purchaseService");
        f.setAccessible(true);
        f.set(controller, purchaseService);
        f = SalesAnalyticsController.class.getDeclaredField("cartRepository");
        f.setAccessible(true);
        f.set(controller, cartRepository);
        f = SalesAnalyticsController.class.getDeclaredField("consumedEventLogRepository");
        f.setAccessible(true);
        f.set(controller, consumedEventLogRepository);
        f = SalesAnalyticsController.class.getDeclaredField("stockChangeLogRepository");
        f.setAccessible(true);
        f.set(controller, stockChangeLogRepository);
        f = SalesAnalyticsController.class.getDeclaredField("objectMapper");
        f.setAccessible(true);
        f.set(controller, new ObjectMapper());
    }

    @Test
    void getSalesSummary_whenPurchasesAreNull_fallsBackToCarts() {
        when(purchaseService.getAllPurchases()).thenReturn(null);
        Cart cart = new Cart();
        cart.setFinalPrice(100f);
        when(cartRepository.findAll()).thenReturn(Collections.singletonList(cart));

        ResponseEntity<Map<String, Object>> response = controller.getSalesSummary(null, null);

        assertNotNull(response.getBody());
        assertEquals(100f, ((Number) response.getBody().get("facturacionTotal")).floatValue());
    }

    @Test
    void getSalesSummary_withNonConfirmedPurchases_returnsZeroAndDoesNotFallback() {
        Purchase purchase = new Purchase();
        purchase.setStatus(Purchase.Status.PENDING);
        purchase.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(Collections.singletonList(purchase));

        ResponseEntity<Map<String, Object>> response = controller.getSalesSummary(null, null);

        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().get("totalVentas"));
        assertEquals(0f, ((Number) response.getBody().get("facturacionTotal")).floatValue());
    }

    @Test
    void getSalesSummary_withDateRange_filtersPurchases() {
        Purchase purchase = new Purchase();
        purchase.setStatus(Purchase.Status.CONFIRMED);
        purchase.setDate(LocalDateTime.now().minusDays(5));
        Cart cart = new Cart();
        cart.setFinalPrice(100f);
        purchase.setCart(cart);

        when(purchaseService.getAllPurchases()).thenReturn(Collections.singletonList(purchase));

        // Date range that excludes the purchase
        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now();

        ResponseEntity<Map<String, Object>> response = controller.getSalesSummary(startDate, endDate);

        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().get("totalVentas"));
    }

    @Test
    void getTopProducts_whenNoPurchases_fallsBackToCarts() {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        Product p1 = new Product();
        p1.setId(1);
        CartItem item1 = new CartItem();
        item1.setProduct(p1);
        item1.setQuantity(5);
        Cart cart = new Cart();
        cart.setItems(Collections.singletonList(item1));
        when(cartRepository.findAll()).thenReturn(Collections.singletonList(cart));

        ResponseEntity<Map<String, Object>> response = controller.getTopProducts(10, null, null);

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(1, data.size());
        assertEquals(1, data.get(0).get("productId"));
    }

    @Test
    void getTopProducts_withLimit_returnsLimitedResults() {
        Product p1 = new Product();
        p1.setId(1);
        Product p2 = new Product();
        p2.setId(2);
        CartItem item1 = new CartItem();
        item1.setProduct(p1);
        item1.setQuantity(10);
        CartItem item2 = new CartItem();
        item2.setProduct(p2);
        item2.setQuantity(20);
        Cart cart = new Cart();
        cart.setItems(Arrays.asList(item1, item2));
        Purchase purchase = new Purchase();
        purchase.setStatus(Purchase.Status.CONFIRMED);
        purchase.setDate(LocalDateTime.now());
        purchase.setCart(cart);
        when(purchaseService.getAllPurchases()).thenReturn(Collections.singletonList(purchase));

        ResponseEntity<Map<String, Object>> response = controller.getTopProducts(1, null, null);

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(1, data.size());
        assertEquals(2, data.get(0).get("productId")); // p2 has more sales
    }

    @Test
    void getTopCategories_whenNoPurchases_fallsBackToCarts() {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        Category cat1 = new Category();
        cat1.setName("Electronics");
        Product p1 = new Product();
        p1.setCategories(Set.of(cat1));
        CartItem item1 = new CartItem();
        item1.setProduct(p1);
        item1.setQuantity(5);
        Cart cart = new Cart();
        cart.setItems(Collections.singletonList(item1));
        when(cartRepository.findAll()).thenReturn(Collections.singletonList(cart));

        ResponseEntity<Map<String, Object>> response = controller.getTopCategories(10, null, null, "bar");

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(1, data.size());
        assertEquals("Electronics", data.get(0).get("category"));
    }

    @Test
    void getTopCategories_withProductWithoutCategory_usesOtros() {
        Product p1 = new Product(); // No category
        CartItem item1 = new CartItem();
        item1.setProduct(p1);
        item1.setQuantity(5);
        Cart cart = new Cart();
        cart.setItems(Collections.singletonList(item1));
        Purchase purchase = new Purchase();
        purchase.setStatus(Purchase.Status.CONFIRMED);
        purchase.setDate(LocalDateTime.now());
        purchase.setCart(cart);
        when(purchaseService.getAllPurchases()).thenReturn(Collections.singletonList(purchase));

        ResponseEntity<Map<String, Object>> response = controller.getTopCategories(10, null, null, "bar");

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(1, data.size());
        assertEquals("Otros", data.get(0).get("category"));
    }

    @Test
    void getTopBrands_whenNoPurchases_fallsBackToCarts() {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        Brand brand1 = new Brand();
        brand1.setName("BrandX");
        Product p1 = new Product();
        p1.setBrand(brand1);
        CartItem item1 = new CartItem();
        item1.setProduct(p1);
        item1.setQuantity(3);
        Cart cart = new Cart();
        cart.setItems(Collections.singletonList(item1));
        when(cartRepository.findAll()).thenReturn(Collections.singletonList(cart));

        ResponseEntity<Map<String, Object>> response = controller.getTopBrands(10, null, null, "bar");

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(1, data.size());
        assertEquals("BrandX", data.get(0).get("brand"));
    }

    @Test
    void getTopBrands_withProductWithoutBrand_usesOtros() {
        Product p1 = new Product(); // No brand
        CartItem item1 = new CartItem();
        item1.setProduct(p1);
        item1.setQuantity(2);
        Cart cart = new Cart();
        cart.setItems(Collections.singletonList(item1));
        Purchase purchase = new Purchase();
        purchase.setStatus(Purchase.Status.CONFIRMED);
        purchase.setDate(LocalDateTime.now());
        purchase.setCart(cart);
        when(purchaseService.getAllPurchases()).thenReturn(Collections.singletonList(purchase));

        ResponseEntity<Map<String, Object>> response = controller.getTopBrands(10, null, null, "bar");

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(1, data.size());
        assertEquals("Otros", data.get(0).get("brand"));
    }

    @Test
    void getDailySales_fallbackToConsumedEventLogs_withDateRange() {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());

        ConsumedEventLog log1 = new ConsumedEventLog();
        log1.setPayloadJson("{\"timestamp\": 1672531200, \"payload\": {\"cart\": {\"finalPrice\": 10.0, \"items\": [{\"quantity\": 1}]}}}"); // Jan 1, 2023
        log1.setProcessedAt(OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        log1.setStatus(ConsumedEventLog.Status.PROCESSED);
        log1.setEventType("Compra confirmada");

        ConsumedEventLog log2 = new ConsumedEventLog();
        log2.setPayloadJson("{\"timestamp\": 1672617600, \"payload\": {\"cart\": {\"finalPrice\": 20.0, \"items\": [{\"quantity\": 2}]}}}"); // Jan 2, 2023
        log2.setProcessedAt(OffsetDateTime.of(2023, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC));
        log2.setStatus(ConsumedEventLog.Status.PROCESSED);
        log2.setEventType("Compra confirmada");

        when(consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseAndProcessedAtBetweenOrderByProcessedAtAsc(
                any(ConsumedEventLog.Status.class), any(String.class), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(Arrays.asList(log1, log2));

        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 1, 2, 23, 59);

        ResponseEntity<Map<String, Object>> response = controller.getDailySales(startDate, endDate);

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(2, data.size());
        assertEquals("2023-01-01", data.get(0).get("date"));
        assertEquals(1, data.get(0).get("ventas"));
        assertEquals(10.0f, ((Number) data.get(0).get("facturacion")).floatValue());
        assertEquals(1, data.get(0).get("unidades"));
        assertEquals("2023-01-02", data.get(1).get("date"));
        assertEquals(1, data.get(1).get("ventas"));
        assertEquals(20.0f, ((Number) data.get(1).get("facturacion")).floatValue());
        assertEquals(2, data.get(1).get("unidades"));
    }

    @Test
    void getStockHistoryByProduct_filtersByDateRange() {
        StockChangeLog log1 = new StockChangeLog();
        log1.setChangedAt(LocalDateTime.now().minusDays(5));
        log1.setOldStock(10); log1.setNewStock(8); log1.setQuantityChanged(-2); log1.setReason("Venta");
        StockChangeLog log2 = new StockChangeLog();
        log2.setChangedAt(LocalDateTime.now().minusDays(1));
        log2.setOldStock(8); log2.setNewStock(10); log2.setQuantityChanged(2); log2.setReason("Reposicion");

        when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(any(Integer.class)))
                .thenReturn(Arrays.asList(log1, log2));

        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now();

        ResponseEntity<Map<String, Object>> response = controller.getStockHistoryByProduct(1, startDate, endDate);

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(1, data.size());
        assertEquals(log2.getNewStock(), data.get(0).get("newStock"));
    }

    @Test
    void getLowStockProducts_returnsEmptyList_whenNoLowStockProducts() {
        Product p1 = new Product(); p1.setId(1); p1.setStock(20);
        Product p2 = new Product(); p2.setId(2); p2.setStock(15);
        when(purchaseService.getProductRepository()).thenReturn(productRepository);
        when(productRepository.findAll()).thenReturn(Arrays.asList(p1, p2));

        ResponseEntity<Map<String, Object>> response = controller.getLowStockProducts(10, 10);

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertTrue(data.isEmpty());
    }

    @Test
    void getLowStockProducts_returnsLimitedResults() {
        Product p1 = new Product(); p1.setId(1); p1.setStock(5); p1.setTitle("Product A");
        Product p2 = new Product(); p2.setId(2); p2.setStock(8); p2.setTitle("Product B");
        Product p3 = new Product(); p3.setId(3); p3.setStock(2); p3.setTitle("Product C");
        when(purchaseService.getProductRepository()).thenReturn(productRepository);
        when(productRepository.findAll()).thenReturn(Arrays.asList(p1, p2, p3));

        ResponseEntity<Map<String, Object>> response = controller.getLowStockProducts(10, 2);

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(2, data.size());
        assertEquals("Product C", data.get(0).get("title")); // Product C has lowest stock
        assertEquals("Product A", data.get(1).get("title"));
    }

    @Test
    void getStockHistoryByProductCode_calculatesProfit_forSalesReason() {
        Product product = new Product();
        product.setId(1);
        product.setProductCode(123);
        product.setPrice(10.0f);

        StockChangeLog log1 = new StockChangeLog();
        log1.setChangedAt(LocalDateTime.now());
        log1.setOldStock(10);
        log1.setNewStock(8);
        log1.setQuantityChanged(-2);
        log1.setReason("Venta");

        StockChangeLog log2 = new StockChangeLog();
        log2.setChangedAt(LocalDateTime.now());
        log2.setOldStock(8);
        log2.setNewStock(10);
        log2.setQuantityChanged(2);
        log2.setReason("Reposicion");

        when(purchaseService.getProductRepository()).thenReturn(productRepository);
        when(productRepository.findByProductCode(123)).thenReturn(product);
        when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(1)).thenReturn(Arrays.asList(log1, log2));

        ResponseEntity<Map<String, Object>> response = controller.getStockHistoryByProductCode(123, true, null, null);

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(2, data.size());
        assertEquals(20.0f, ((Number) data.get(0).get("profit")).floatValue()); // 2 * 10.0f
        assertEquals(20.0f, ((Number) data.get(0).get("profitAccumulated")).floatValue());
        assertEquals(0.0f, ((Number) data.get(1).get("profit")).floatValue());
        assertEquals(20.0f, ((Number) data.get(1).get("profitAccumulated")).floatValue());
    }

    @Test
    void getTopCustomers_whenNoPurchases_fallsBackToCarts() {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());

        User user = new User();
        user.setId(1);
        user.setName("Test User");
        user.setEmail("test@example.com");

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setFinalPrice(50.0f);

        when(cartRepository.findAll()).thenReturn(Collections.singletonList(cart));

        ResponseEntity<Map<String, Object>> response = controller.getTopCustomers(10);

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(1, data.size());
        assertEquals(1, data.get(0).get("userId"));
        assertEquals("Test User", data.get(0).get("name"));
        assertEquals(50.0f, ((Number) data.get(0).get("totalSpent")).floatValue());
    }

    @Test
    void getSalesHistogram_withMultipleBins_returnsCorrectCounts() {
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); Cart c1 = new Cart(); c1.setFinalPrice(10f); p1.setCart(c1);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); Cart c2 = new Cart(); c2.setFinalPrice(25f); p2.setCart(c2);
        Purchase p3 = new Purchase(); p3.setStatus(Purchase.Status.CONFIRMED); Cart c3 = new Cart(); c3.setFinalPrice(40f); p3.setCart(c3);
        when(purchaseService.getAllPurchases()).thenReturn(Arrays.asList(p1, p2, p3));

        ResponseEntity<Map<String, Object>> response = controller.getSalesHistogram(3);

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(3, data.size());
        assertEquals(1, data.get(0).get("count")); // 10f
        assertEquals(1, data.get(1).get("count")); // 25f
        assertEquals(1, data.get(2).get("count")); // 40f
    }

    @Test
    void getCorrelation_whenNoPurchases_fallsBackToCarts() {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());

        Product product = new Product();
        product.setId(1);
        product.setProductCode(100);
        product.setTitle("Test Product");
        product.setPrice(15.0f);

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(3);

        Cart cart = new Cart();
        cart.setItems(Collections.singletonList(item));

        when(cartRepository.findAll()).thenReturn(Collections.singletonList(cart));
        when(purchaseService.getProductRepository()).thenReturn(productRepository);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        ResponseEntity<Map<String, Object>> response = controller.getCorrelation();

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(1, data.size());
        assertEquals(1, data.get(0).get("productId"));
        assertEquals(3, data.get(0).get("unitsSold"));
        assertEquals(15.0f, ((Number) data.get(0).get("price")).floatValue());
    }

    @Test
    void getCategoryGrowth_whenNoPurchases_fallsBackToConsumedEvents() {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());

        Category category = new Category();
        category.setName("Books");

        Product product = new Product();
        product.setProductCode(101);
        product.setCategories(Set.of(category));

        ConsumedEventLog log = new ConsumedEventLog();
        log.setPayloadJson("{\"payload\": {\"cart\": {\"items\": [{\"productCode\": 101, \"quantity\": 2}]}}}");
        log.setStatus(ConsumedEventLog.Status.PROCESSED);
        log.setEventType("Compra confirmada");

        when(consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(any(), any()))
                .thenReturn(Collections.singletonList(log));
        when(purchaseService.getProductRepository()).thenReturn(productRepository);
        when(productRepository.findByProductCode(101)).thenReturn(product);

        ResponseEntity<Map<String, Object>> response = controller.getCategoryGrowth(null, null);

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(1, data.size());
        assertEquals("Books", data.get(0).get("category"));
        assertEquals(2, data.get(0).get("unidades"));
    }

    @Test
    void getProductEventsTimeline_filtersByProductCode() {
        ConsumedEventLog log1 = new ConsumedEventLog();
        log1.setPayloadJson("{\"payload\": {\"cart\": {\"items\": [{\"productCode\": 111}]}}}");
        log1.setEventType("Compra confirmada");
        log1.setProcessedAt(OffsetDateTime.now());
        log1.setStatus(ConsumedEventLog.Status.PROCESSED);

        ConsumedEventLog log2 = new ConsumedEventLog();
        log2.setPayloadJson("{\"payload\": {\"cart\": {\"items\": [{\"productCode\": 222}]}}}");
        log2.setEventType("Producto visto");
        log2.setProcessedAt(OffsetDateTime.now());
        log2.setStatus(ConsumedEventLog.Status.PROCESSED);

        when(consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(any(), any()))
                .thenReturn(Arrays.asList(log1, log2));

        ResponseEntity<Map<String, Object>> response = controller.getProductEventsTimeline(111, null, null);

        assertNotNull(response.getBody());
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertEquals(1, data.size());
        assertEquals("Compra confirmada", data.get(0).get("eventType"));
    }
}
