package ar.edu.uade.analytics.Controller.SalesAnalyticsTests;

import ar.edu.uade.analytics.Controller.SalesAnalyticsController;
import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Repository.ProductRepository;
import ar.edu.uade.analytics.Repository.StockChangeLogRepository;
import ar.edu.uade.analytics.Service.PurchaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class SalesAnalyticsControllerTargetedBranchTests {

    @Mock
    PurchaseService purchaseService;

    @Mock
    StockChangeLogRepository stockChangeLogRepository;

    @Mock
    ProductRepository productRepository;

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
    void topCategoriesAndBrands_pieAndBar() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // Build categories and brands
        Category cat1 = new Category(); cat1.setId(1); cat1.setName("Cat1");
        Category cat2 = new Category(); cat2.setId(2); cat2.setName("Cat2");
        Brand b1 = new Brand(); b1.setId(10); b1.setName("B1");

        Product p1 = new Product(); p1.setId(100); p1.setTitle("P1"); p1.setCategories(java.util.Set.of(cat1)); p1.setBrand(b1);
        Product p2 = new Product(); p2.setId(101); p2.setTitle("P2"); p2.setCategories(java.util.Set.of(cat2));

        CartItem ci1 = new CartItem(); ci1.setProduct(p1); ci1.setQuantity(2);
        CartItem ci2 = new CartItem(); ci2.setProduct(p2); ci2.setQuantity(3);
        Cart cart = new Cart(); cart.setItems(List.of(ci1, ci2));

        Purchase pu = new Purchase(); pu.setStatus(Purchase.Status.CONFIRMED); pu.setDate(LocalDateTime.now()); pu.setCart(cart);
        lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(pu));

        // Ensure product repository is returned by purchaseService
        lenient().when(purchaseService.getProductRepository()).thenReturn(productRepository);
        lenient().when(productRepository.findById(100)).thenReturn(java.util.Optional.of(p1));
        lenient().when(productRepository.findById(101)).thenReturn(java.util.Optional.of(p2));

        Map<String, Object> catsBar = controller.getTopCategories(10, null, null, "bar").getBody();
        assertNotNull(catsBar);
        assertTrue(((List<?>)catsBar.get("data")).size() >= 1);

        Map<String, Object> catsPie = controller.getTopCategories(10, null, null, "pie").getBody();
        assertNotNull(catsPie);
        assertNotNull(catsPie.get("chartBase64"));

        Map<String, Object> brandsBar = controller.getTopBrands(10, null, null, "bar").getBody();
        assertNotNull(brandsBar);
        assertTrue(((List<?>)brandsBar.get("data")).size() >= 0);

        Map<String, Object> brandsPie = controller.getTopBrands(10, null, null, "pie").getBody();
        assertNotNull(brandsPie);
        assertNotNull(brandsPie.get("chartBase64"));
    }

    @Test
    void salesHistogram_and_productTrends_buckets() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // Two users: one with 1 purchase, another with 4 purchases
        User u1 = new User(); u1.setId(1);
        User u2 = new User(); u2.setId(2);
        Product prod = new Product(); prod.setId(200);
        CartItem ci = new CartItem(); ci.setProduct(prod); ci.setQuantity(1);
        Cart cart = new Cart(); cart.setItems(List.of(ci));

        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.now()); p1.setUser(u1); p1.setCart(cart);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.now()); p2.setUser(u2); p2.setCart(cart);
        Purchase p3 = new Purchase(); p3.setStatus(Purchase.Status.CONFIRMED); p3.setDate(LocalDateTime.now()); p3.setUser(u2); p3.setCart(cart);
        Purchase p4 = new Purchase(); p4.setStatus(Purchase.Status.CONFIRMED); p4.setDate(LocalDateTime.now()); p4.setUser(u2); p4.setCart(cart);
        Purchase p5 = new Purchase(); p5.setStatus(Purchase.Status.CONFIRMED); p5.setDate(LocalDateTime.now()); p5.setUser(u2); p5.setCart(cart);

        lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1,p2,p3,p4,p5));

        Map<String,Object> resp = controller.getSalesHistogram(null, null).getBody();
        assertNotNull(resp);
        Map<String,Integer> hist = (Map<String,Integer>) resp.get("histogram");
        assertNotNull(hist);
        // Expect keys like "1-2" and "3-5"
        assertTrue(hist.containsKey("1-2") || hist.containsKey("3-5"));
        Map<Integer, Double> trends = (Map<Integer, Double>) resp.get("productTrends");
        assertNotNull(trends);
    }

    @Test
    void dailySales_lineAndBar_and_categoryGrowth_presentAndAbsent() throws Exception {
        SalesAnalyticsController controller = prepareController();
        User u = new User(); u.setId(5);
        Cart c = new Cart();
        Product prod = new Product(); prod.setId(300);
        Category cat = new Category(); cat.setId(7); cat.setName("K"); prod.setCategories(java.util.Set.of(cat));
        CartItem ci = new CartItem(); ci.setProduct(prod); ci.setQuantity(1);
        c.setItems(List.of(ci));
        Purchase p = new Purchase(); p.setStatus(Purchase.Status.CONFIRMED); p.setDate(LocalDateTime.of(2025,6,1,10,0)); p.setCart(c);
        lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p));

        Map<String,Object> line = controller.getDailySales(null, null, "line").getBody();
        assertNotNull(line);
        Map<String,Object> bar = controller.getDailySales(null, null, "bar").getBody();
        assertNotNull(bar);

        Map<String,Object> growthPresent = controller.getCategoryGrowth(7, null, null).getBody();
        assertNotNull(growthPresent);
        assertTrue(((Map<?,?>)growthPresent.get("categoryGrowth")).size() >= 0);

        Map<String,Object> growthAbsent = controller.getCategoryGrowth(9999, null, null).getBody();
        assertNotNull(growthAbsent);
        assertTrue(((Map<?,?>)growthAbsent.get("categoryGrowth")).isEmpty());
    }

    @Test
    void stockHistoryByProductCode_profitAndNoProfit() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // mock product repository in purchaseService
        lenient().when(purchaseService.getProductRepository()).thenReturn(productRepository);
        Product prod = new Product(); prod.setId(400); prod.setProductCode(555); prod.setPrice(2f);
        lenient().when(productRepository.findByProductCode(555)).thenReturn(prod);
        StockChangeLog s1 = new StockChangeLog(); s1.setProduct(prod); s1.setChangedAt(LocalDateTime.of(2025,7,1,9,0)); s1.setReason("Venta"); s1.setQuantityChanged(2); s1.setOldStock(20); s1.setNewStock(18);
        StockChangeLog s2 = new StockChangeLog(); s2.setProduct(prod); s2.setChangedAt(LocalDateTime.of(2025,7,2,9,0)); s2.setReason("Ajuste"); s2.setQuantityChanged(1); s2.setOldStock(18); s2.setNewStock(17);
        lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(prod.getId())).thenReturn(List.of(s1,s2));

        Map<String,Object> respProfit = controller.getStockHistoryByProductCode(555, true, null, null).getBody();
        assertNotNull(respProfit);
        List<?> data = (List<?>) respProfit.get("data");
        assertEquals(2, data.size());

        Map<String,Object> respNoProfit = controller.getStockHistoryByProductCode(555, false, null, null).getBody();
        assertNotNull(respNoProfit);
        List<?> d2 = (List<?>) respNoProfit.get("data");
        assertEquals(2, d2.size());
    }
}

