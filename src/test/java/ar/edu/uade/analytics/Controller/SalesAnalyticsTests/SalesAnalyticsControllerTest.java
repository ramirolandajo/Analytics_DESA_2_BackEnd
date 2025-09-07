package ar.edu.uade.analytics.Controller.SalesAnalyticsTests;

import ar.edu.uade.analytics.Controller.SalesAnalyticsController;
import ar.edu.uade.analytics.Entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import java.util.Collections;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SalesAnalyticsControllerTest {
    private MockMvc mockMvc;

    @Mock
    private ar.edu.uade.analytics.Service.PurchaseService purchaseService;

    @Mock
    private ar.edu.uade.analytics.Repository.StockChangeLogRepository stockChangeLogRepository;

    private SalesAnalyticsController controller;

    @BeforeEach
    void setup() throws Exception {
        controller = new SalesAnalyticsController();
        // inyectar mocks por reflection
        java.lang.reflect.Field f1 = SalesAnalyticsController.class.getDeclaredField("purchaseService");
        f1.setAccessible(true);
        f1.set(controller, purchaseService);
        java.lang.reflect.Field f2 = SalesAnalyticsController.class.getDeclaredField("stockChangeLogRepository");
        f2.setAccessible(true);
        f2.set(controller, stockChangeLogRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void contextLoads() {
        // Test de carga de contexto
    }

    @Test
    void testGetSalesSummary() throws Exception {
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVentas").exists());
    }

    @Test
    void testGetTopProducts() throws Exception {
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/top-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testGetTopCategories() throws Exception {
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/top-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testGetSalesSummaryChart() throws Exception {
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/summary/chart"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTopBrands() throws Exception {
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/top-brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testGetDailySales() throws Exception {
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/daily-sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testGetStockHistoryByProduct() throws Exception {
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(1)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/stock-history?productId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testGetStockHistoryByProductCode() throws Exception {
        // Mock productRepository to return a product with id=1 so controller does not NPE
        ar.edu.uade.analytics.Repository.ProductRepository productRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        ar.edu.uade.analytics.Entity.Product prod = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.Product.class);
        when(prod.getId()).thenReturn(1);
        when(productRepo.findByProductCode(1)).thenReturn(prod);
        when(purchaseService.getProductRepository()).thenReturn(productRepo);
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(1)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/stock-history-by-product-code?productCode=1"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetProductsDashboard() throws Exception {
        // Mock productRepository with empty product list so controller can build response
        ar.edu.uade.analytics.Repository.ProductRepository productRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(productRepo.findAll()).thenReturn(Collections.emptyList());
        when(purchaseService.getProductRepository()).thenReturn(productRepo);
        mockMvc.perform(get("/analytics/sales/products-dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTopCustomers() throws Exception {
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/top-customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testGetHistogram() throws Exception {
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/histogram"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.histogram").exists());
    }

    @Test
    void testGetCorrelation() throws Exception {
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/correlation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chartBase64").exists());
    }

    @Test
    void testGetCategoryGrowth() throws Exception {
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/category-growth?categoryId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryGrowth").exists());
    }

    @Test
    void testGetProductEventsTimeline() throws Exception {
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findAll()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/product-events-timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").exists());
    }

    @Test
    void testGetLowStockProducts() throws Exception {
        // Mock productRepository with empty list so endpoint returns OK
        ar.edu.uade.analytics.Repository.ProductRepository productRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(productRepo.findAll()).thenReturn(Collections.emptyList());
        when(purchaseService.getProductRepository()).thenReturn(productRepo);
        mockMvc.perform(get("/analytics/sales/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testGetSalesSummary_withData() throws Exception {
        Purchase p = new Purchase();
        p.setStatus(Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        Cart c = new Cart();
        c.setFinalPrice(1000f);
        CartItem item = new CartItem();
        item.setQuantity(2);
        Product prod = org.mockito.Mockito.mock(Product.class);
        when(prod.getId()).thenReturn(1);
        item.setProduct(prod);
        c.setItems(List.of(item));
        p.setCart(c);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        mockMvc.perform(get("/analytics/sales/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVentas").value(1))
                .andExpect(jsonPath("$.facturacionTotal").value(1000.0f))
                .andExpect(jsonPath("$.productosVendidos").value(2))
                .andExpect(jsonPath("$.chartBase64").exists());
    }

    @Test
    void testGetTopProducts_withData() throws Exception {
        Purchase p = new Purchase();
        p.setStatus(Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        Cart c = new Cart();
        CartItem item = new CartItem();
        item.setQuantity(3);
        Product prod = org.mockito.Mockito.mock(Product.class);
        when(prod.getId()).thenReturn(10);
        when(prod.getTitle()).thenReturn("TestProd");
        item.setProduct(prod);
        c.setItems(List.of(item));
        p.setCart(c);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        // Mock productRepository
        ar.edu.uade.analytics.Repository.ProductRepository productRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        when(purchaseService.getProductRepository()).thenReturn(productRepo);
        org.mockito.Mockito.lenient().when(productRepo.findById(10)).thenReturn(java.util.Optional.of(prod));
        mockMvc.perform(get("/analytics/sales/top-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productId").value(10))
                .andExpect(jsonPath("$.data[0].title").value("TestProd"))
                .andExpect(jsonPath("$.data[0].cantidadVendida").value(3))
                .andExpect(jsonPath("$.chartBase64").exists());
    }

    @Test
    void testGetTopCategories_withData() throws Exception {
        // Crear purchase con producto y categoria
        Purchase p = new Purchase();
        p.setStatus(Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        Cart c = new Cart();
        CartItem item = new CartItem();
        item.setQuantity(4);
        Product prod = org.mockito.Mockito.mock(Product.class);
        ar.edu.uade.analytics.Entity.Category cat = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.Category.class);
        when(cat.getId()).thenReturn(1);
        when(cat.getName()).thenReturn("CatA");
        when(prod.getCategories()).thenReturn(Set.of(cat));
        item.setProduct(prod);
        c.setItems(List.of(item));
        p.setCart(c);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        mockMvc.perform(get("/analytics/sales/top-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].category").value("CatA"))
                .andExpect(jsonPath("$.data[0].cantidadVendida").value(4))
                .andExpect(jsonPath("$.chartBase64").exists());
    }

    @Test
    void testGetTopBrands_withData() throws Exception {
        Purchase p = new Purchase();
        p.setStatus(Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        Cart c = new Cart();
        CartItem item = new CartItem();
        item.setQuantity(2);
        Product prod = org.mockito.Mockito.mock(Product.class);
        ar.edu.uade.analytics.Entity.Brand brand = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.Brand.class);
        when(brand.getName()).thenReturn("BrandX");
        when(prod.getBrand()).thenReturn(brand);
        item.setProduct(prod);
        c.setItems(List.of(item));
        p.setCart(c);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        mockMvc.perform(get("/analytics/sales/top-brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].brand").value("BrandX"))
                .andExpect(jsonPath("$.data[0].cantidadVendida").value(2))
                .andExpect(jsonPath("$.chartBase64").exists());
    }

    @Test
    void testGetSalesSummaryChart_bytes() throws Exception {
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/summary/chart?type=pie"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void testGetProductEventsTimeline_withLogs() throws Exception {
        ar.edu.uade.analytics.Entity.StockChangeLog log1 = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.StockChangeLog.class);
        ar.edu.uade.analytics.Entity.StockChangeLog log2 = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.StockChangeLog.class);
        Product p1 = org.mockito.Mockito.mock(Product.class);
        when(p1.getId()).thenReturn(1);
        when(p1.getTitle()).thenReturn("P1");
        when(log1.getProduct()).thenReturn(p1);
        when(log1.getChangedAt()).thenReturn(LocalDateTime.of(2023,1,1,10,0));
        when(log1.getNewStock()).thenReturn(5);
        when(log1.getOldStock()).thenReturn(7);
        when(log1.getQuantityChanged()).thenReturn(-2);
        when(log1.getReason()).thenReturn("Ajuste");

        Product p2 = org.mockito.Mockito.mock(Product.class);
        when(p2.getId()).thenReturn(2);
        when(p2.getTitle()).thenReturn("P2");
        when(log2.getProduct()).thenReturn(p2);
        when(log2.getChangedAt()).thenReturn(LocalDateTime.of(2023,1,2,10,0));
        when(log2.getNewStock()).thenReturn(3);
        when(log2.getOldStock()).thenReturn(6);
        when(log2.getQuantityChanged()).thenReturn(-3);
        when(log2.getReason()).thenReturn("Venta");

        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findAll()).thenReturn(List.of(log1, log2));
        mockMvc.perform(get("/analytics/sales/product-events-timeline?topN=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events[0].productTitle").exists())
                .andExpect(jsonPath("$.chartBase64").exists());
    }

    @Test
    void testGetStockHistoryByProductCode_withProfit() throws Exception {
        ar.edu.uade.analytics.Repository.ProductRepository productRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        Product prod = org.mockito.Mockito.mock(Product.class);
        when(prod.getId()).thenReturn(1);
        when(prod.getPrice()).thenReturn(50f);
        when(productRepo.findByProductCode(1)).thenReturn(prod);
        when(purchaseService.getProductRepository()).thenReturn(productRepo);
        ar.edu.uade.analytics.Entity.StockChangeLog log = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.StockChangeLog.class);
        when(log.getChangedAt()).thenReturn(LocalDateTime.of(2023,1,3,10,0));
        when(log.getNewStock()).thenReturn(8);
        when(log.getOldStock()).thenReturn(10);
        when(log.getQuantityChanged()).thenReturn(2);
        when(log.getReason()).thenReturn("Venta");
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(1)).thenReturn(List.of(log));
        mockMvc.perform(get("/analytics/sales/stock-history-by-product-code?productCode=1&showProfit=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].profit").exists());
    }

    @Test
    void testGetProductsDashboard_withFilters() throws Exception {
        Product p = org.mockito.Mockito.mock(Product.class);
        when(p.getId()).thenReturn(5);
        when(p.getTitle()).thenReturn("ProdX");
        when(p.getStock()).thenReturn(3);
        ar.edu.uade.analytics.Entity.Category cat = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.Category.class);
        when(cat.getId()).thenReturn(7);
        when(p.getCategories()).thenReturn(Set.of(cat));
        ar.edu.uade.analytics.Entity.Brand brand = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.Brand.class);
        when(brand.getId()).thenReturn(9);
        when(p.getBrand()).thenReturn(brand);
        ar.edu.uade.analytics.Repository.ProductRepository productRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(productRepo.findAll()).thenReturn(List.of(p));
        when(purchaseService.getProductRepository()).thenReturn(productRepo);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/analytics/sales/products-dashboard?categoryId=7&brandId=9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductos").value(1))
                .andExpect(jsonPath("$.stockTotal").value(3))
                .andExpect(jsonPath("$.stockChartBase64").exists());
    }

    @Test
    void testGetHistogram_andCorrelation() throws Exception {
        // Crear compras con usuarios y cart
        Purchase p1 = new Purchase();
        p1.setStatus(Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.of(2023,1,1,10,0));
        ar.edu.uade.analytics.Entity.User u1 = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.User.class);
        when(u1.getId()).thenReturn(100);
        when(u1.getName()).thenReturn("U1");
        when(u1.getEmail()).thenReturn("u1@example.com");
        p1.setUser(u1);
        Cart c1 = new Cart();
        c1.setFinalPrice(200f);
        p1.setCart(c1);

        Purchase p2 = new Purchase();
        p2.setStatus(Purchase.Status.CONFIRMED);
        p2.setDate(LocalDateTime.of(2023,1,2,10,0));
        ar.edu.uade.analytics.Entity.User u2 = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.User.class);
        when(u2.getId()).thenReturn(101);
        when(u2.getName()).thenReturn("U2");
        when(u2.getEmail()).thenReturn("u2@example.com");
        p2.setUser(u2);
        Cart c2 = new Cart();
        c2.setFinalPrice(50f);
        p2.setCart(c2);

        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1, p2));
        mockMvc.perform(get("/analytics/sales/histogram"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.histogram").exists())
                .andExpect(jsonPath("$.chartBase64").exists());
        mockMvc.perform(get("/analytics/sales/correlation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chartBase64").exists())
                .andExpect(jsonPath("$.regression").exists());
    }

    @Test
    void testGetSalesHistogram_withTrends() throws Exception {
        // Mismo producto vendido varios días para generar tendencia
        Purchase p1 = new Purchase();
        p1.setStatus(Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.of(2023,1,1,10,0));
        Cart c1 = new Cart();
        CartItem i1 = new CartItem();
        Product prod = org.mockito.Mockito.mock(Product.class);
        when(prod.getId()).thenReturn(100);
        i1.setProduct(prod);
        i1.setQuantity(1);
        c1.setItems(List.of(i1));
        p1.setCart(c1);

        Purchase p2 = new Purchase();
        p2.setStatus(Purchase.Status.CONFIRMED);
        p2.setDate(LocalDateTime.of(2023,1,2,10,0));
        Cart c2 = new Cart();
        CartItem i2 = new CartItem();
        i2.setProduct(prod);
        i2.setQuantity(2);
        c2.setItems(List.of(i2));
        p2.setCart(c2);

        Purchase p3 = new Purchase();
        p3.setStatus(Purchase.Status.CONFIRMED);
        p3.setDate(LocalDateTime.of(2023,1,3,10,0));
        Cart c3 = new Cart();
        CartItem i3 = new CartItem();
        i3.setProduct(prod);
        i3.setQuantity(3);
        c3.setItems(List.of(i3));
        p3.setCart(c3);

        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1, p2, p3));
        mockMvc.perform(get("/analytics/sales/histogram"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productTrends['100']").exists())
                .andExpect(jsonPath("$.chartBase64").exists());
    }

    @Test
    void testGetProductsDashboard_withEvolution_andFilters() throws Exception {
        Product p = org.mockito.Mockito.mock(Product.class);
        when(p.getId()).thenReturn(200);
        when(p.getTitle()).thenReturn("EvoProd");
        when(p.getStock()).thenReturn(5);
        when(p.getCategories()).thenReturn(Set.of());
        when(p.getBrand()).thenReturn(null);
        ar.edu.uade.analytics.Repository.ProductRepository productRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(productRepo.findAll()).thenReturn(List.of(p));
        when(purchaseService.getProductRepository()).thenReturn(productRepo);
        // Crear compras que hagan que el producto 200 sea top
        Purchase p1 = new Purchase();
        p1.setStatus(Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.of(2023,1,1,10,0));
        Cart c1 = new Cart();
        CartItem it = new CartItem();
        it.setProduct(p);
        it.setQuantity(5);
        c1.setItems(List.of(it));
        p1.setCart(c1);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1));
        // Mock logs para evolución
        ar.edu.uade.analytics.Entity.StockChangeLog log1 = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.StockChangeLog.class);
        when(log1.getProduct()).thenReturn(p);
        when(log1.getChangedAt()).thenReturn(LocalDateTime.of(2023,1,1,9,0));
        when(log1.getNewStock()).thenReturn(10);
        ar.edu.uade.analytics.Entity.StockChangeLog log2 = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.StockChangeLog.class);
        when(log2.getProduct()).thenReturn(p);
        when(log2.getChangedAt()).thenReturn(LocalDateTime.of(2023,1,2,9,0));
        when(log2.getNewStock()).thenReturn(7);
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(200)).thenReturn(List.of(log1, log2));
        mockMvc.perform(get("/analytics/sales/products-dashboard?categoryId=&brandId="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductos").value(1))
                .andExpect(jsonPath("$.evolutionChartBase64").exists());
    }

    @Test
    void testGetSalesCorrelation_withDataProducesRegression() throws Exception {
        Purchase p1 = new Purchase();
        p1.setStatus(Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.of(2023,1,1,10,0));
        ar.edu.uade.analytics.Entity.User u1 = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.User.class);
        when(u1.getId()).thenReturn(300);
        when(u1.getName()).thenReturn("UserA");
        when(u1.getEmail()).thenReturn("a@example.com");
        p1.setUser(u1);
        Cart c1 = new Cart(); c1.setFinalPrice(120f); p1.setCart(c1);

        Purchase p2 = new Purchase();
        p2.setStatus(Purchase.Status.CONFIRMED);
        p2.setDate(LocalDateTime.of(2023,1,2,11,0));
        p2.setUser(u1);
        Cart c2 = new Cart(); c2.setFinalPrice(80f); p2.setCart(c2);

        Purchase p3 = new Purchase();
        p3.setStatus(Purchase.Status.CONFIRMED);
        p3.setDate(LocalDateTime.of(2023,1,2,12,0));
        ar.edu.uade.analytics.Entity.User u2 = org.mockito.Mockito.mock(ar.edu.uade.analytics.Entity.User.class);
        when(u2.getId()).thenReturn(301);
        when(u2.getName()).thenReturn("UserB");
        when(u2.getEmail()).thenReturn("b@example.com");
        p3.setUser(u2);
        Cart c3 = new Cart(); c3.setFinalPrice(50f); p3.setCart(c3);

        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1, p2, p3));
        mockMvc.perform(get("/analytics/sales/correlation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regression.a").exists())
                .andExpect(jsonPath("$.regression.b").exists())
                .andExpect(jsonPath("$.chartBase64").exists());
    }
}
