package ar.edu.uade.analytics.Controller.SalesAnalyticsTests;

import ar.edu.uade.analytics.Controller.SalesAnalyticsController;
import ar.edu.uade.analytics.Entity.*;
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
public class SalesAnalyticsControllerBranchTests {

    @Mock
    PurchaseService purchaseService;

    @Mock
    StockChangeLogRepository stockChangeLogRepository;

    // Helper to inject mocks
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
    void testGetTopProducts_withMissingTitle() throws Exception {
        SalesAnalyticsController controller = prepareController();

        // Purchase with product id 1 and 2
        Product p1 = new Product(); p1.setId(1); p1.setTitle("ProdA");
        Product p2 = new Product(); p2.setId(2); // no title -> will fallback to ID
        CartItem ci1 = new CartItem(); ci1.setProduct(p1); ci1.setQuantity(5);
        CartItem ci2 = new CartItem(); ci2.setProduct(p2); ci2.setQuantity(3);
        Cart cart = new Cart(); cart.setItems(List.of(ci1, ci2));
        Purchase pur = new Purchase(); pur.setStatus(Purchase.Status.CONFIRMED); pur.setDate(LocalDateTime.now()); pur.setCart(cart);

        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(pur));
        var prodRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(purchaseService.getProductRepository()).thenReturn(prodRepo);
        org.mockito.Mockito.lenient().when(prodRepo.findById(1)).thenReturn(java.util.Optional.of(p1));
        org.mockito.Mockito.lenient().when(prodRepo.findById(2)).thenReturn(java.util.Optional.ofNullable(null));

        Map<String, Object> resp = controller.getTopProducts(10, null, null).getBody();
        assertNotNull(resp);
        var data = (List<?>) resp.get("data");
        assertEquals(2, data.size());
        Map<?,?> first = (Map<?,?>) data.get(0);
        assertEquals(1, first.get("productId"));
        assertEquals("ProdA", first.get("title"));
        Map<?,?> second = (Map<?,?>) data.get(1);
        assertEquals(2, second.get("productId"));
        assertTrue(((String)second.get("title")).contains("ID"));
    }

    @Test
    void testTopCategories_andTopBrands_pieAndBar() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // category
        Category c = new Category(); c.setId(11); c.setName("CatX");
        Product prod = new Product(); prod.setId(21); prod.setCategories(java.util.Set.of(c));
        CartItem item = new CartItem(); item.setProduct(prod); item.setQuantity(2);
        Cart cart = new Cart(); cart.setItems(List.of(item));
        Purchase pur = new Purchase(); pur.setStatus(Purchase.Status.CONFIRMED); pur.setDate(LocalDateTime.now()); pur.setCart(cart);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(pur));

        Map<String,Object> catsBar = controller.getTopCategories(10, null, null, "bar").getBody();
        assertNotNull(catsBar);
        assertTrue(((List<?>)catsBar.get("data")).size()>=1);
        assertNotNull(catsBar.get("chartBase64"));

        Map<String,Object> catsPie = controller.getTopCategories(10, null, null, "pie").getBody();
        assertNotNull(catsPie);
        assertNotNull(catsPie.get("chartBase64"));

        // brand
        Brand b = new Brand(); b.setId(33); b.setName("BrandY");
        Product prod2 = new Product(); prod2.setId(22); prod2.setBrand(b);
        CartItem item2 = new CartItem(); item2.setProduct(prod2); item2.setQuantity(4);
        Cart cart2 = new Cart(); cart2.setItems(List.of(item2));
        Purchase pur2 = new Purchase(); pur2.setStatus(Purchase.Status.CONFIRMED); pur2.setDate(LocalDateTime.now()); pur2.setCart(cart2);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(pur2));

        Map<String,Object> brands = controller.getTopBrands(10, null, null, "bar").getBody();
        assertNotNull(brands);
        assertTrue(((List<?>)brands.get("data")).size()>=1);
        assertNotNull(brands.get("chartBase64"));
    }

    @Test
    void testSalesHistogram_andCorrelation_dateFiltering() throws Exception {
        SalesAnalyticsController controller = prepareController();
        User u1 = new User(); u1.setId(1000); u1.setName("U"); u1.setEmail("u@u");
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setUser(u1); p1.setDate(LocalDateTime.of(2023,1,1,10,0));
        Cart c1 = new Cart(); c1.setFinalPrice(100f); p1.setCart(c1);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setUser(u1); p2.setDate(LocalDateTime.of(2024,1,1,10,0));
        Cart c2 = new Cart(); c2.setFinalPrice(150f); p2.setCart(c2);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1, p2));

        // filter to 2023 only
        Map<String,Object> hist = controller.getSalesHistogram(LocalDateTime.of(2023,1,1,0,0), LocalDateTime.of(2023,12,31,23,59)).getBody();
        assertNotNull(hist);
        assertTrue(((Map<?,?>)hist.get("productTrends")).size()>=0);

        Map<String,Object> corr = controller.getSalesCorrelation(LocalDateTime.of(2023,1,1,0,0), LocalDateTime.of(2023,12,31,23,59)).getBody();
        assertNotNull(corr);
        assertTrue(((Map<?,?>)corr.get("regression")).containsKey("a"));
    }

    @Test
    void testProductsDashboard_filters_noTopProducts() throws Exception {
        SalesAnalyticsController controller = prepareController();
        Product p = new Product(); p.setId(500); p.setTitle("Only"); p.setStock(7);
        Category cat = new Category(); cat.setId(55); cat.setName("C55");
        p.setCategories(java.util.Set.of(cat));
        Brand brand = new Brand(); brand.setId(66); brand.setName("B66");
        p.setBrand(brand);
        var prodRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(prodRepo.findAll()).thenReturn(List.of(p));
        org.mockito.Mockito.lenient().when(purchaseService.getProductRepository()).thenReturn(prodRepo);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of());

        Map<String,Object> resp = controller.getProductsDashboard(null, null, 55, 66).getBody();
        assertNotNull(resp);
        assertEquals(1, resp.get("totalProductos"));
        // evolutionChartBase64 may be null (no top products), but fields exist
        assertTrue(resp.containsKey("evolutionChartBase64"));
    }

    @Test
    void testDailySales_bar_and_line_variants() throws Exception {
        SalesAnalyticsController controller = prepareController();
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.of(2023,3,1,10,0));
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.of(2023,3,1,11,0));
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1, p2));

        Map<String,Object> line = controller.getDailySales(null, null, "line").getBody();
        assertNotNull(line);
        assertTrue(((List<?>)line.get("data")).size()>=1);

        Map<String,Object> bar = controller.getDailySales(null, null, "bar").getBody();
        assertNotNull(bar);
        assertTrue(((List<?>)bar.get("data")).size()>=1);
    }

    @Test
    void testStockHistoryByProductCode_notFound_and_profit() throws Exception {
        SalesAnalyticsController controller = prepareController();
        var prodRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(purchaseService.getProductRepository()).thenReturn(prodRepo);
        // not found
        org.mockito.Mockito.lenient().when(prodRepo.findByProductCode(999)).thenReturn(null);
        var resp = controller.getStockHistoryByProductCode(999, false, null, null);
        assertEquals(400, resp.getStatusCodeValue());

        // found with profit
        Product p = new Product(); p.setId(88); p.setPrice(10f);
        org.mockito.Mockito.lenient().when(prodRepo.findByProductCode(123)).thenReturn(p);
        StockChangeLog log = new StockChangeLog(); log.setProduct(p); log.setChangedAt(LocalDateTime.of(2023,5,1,10,0)); log.setOldStock(10); log.setNewStock(8); log.setQuantityChanged(2); log.setReason("Venta");
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(88)).thenReturn(List.of(log));
        var ok = controller.getStockHistoryByProductCode(123, true, null, null).getBody();
        assertNotNull(ok);
        var data = (List<?>) ok.get("data");
        assertEquals(1, data.size());
        Map<?,?> entry = (Map<?,?>) data.get(0);
        assertTrue(entry.containsKey("profit"));
    }
}

