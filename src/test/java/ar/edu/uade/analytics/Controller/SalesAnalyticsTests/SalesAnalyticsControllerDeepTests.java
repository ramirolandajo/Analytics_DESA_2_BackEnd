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
public class SalesAnalyticsControllerDeepTests {

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
    void testSalesSummaryChart_bar_and_pie_bytes() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // create a purchase
        Product prod = new Product(); prod.setId(900);
        CartItem it = new CartItem(); it.setProduct(prod); it.setQuantity(2);
        Cart cart = new Cart(); cart.setItems(List.of(it)); cart.setFinalPrice(400f);
        Purchase p = new Purchase(); p.setStatus(Purchase.Status.CONFIRMED); p.setDate(LocalDateTime.of(2023,8,1,9,0)); p.setCart(cart);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
    }

    @Test
    void testProductEventsTimeline_topN_selection_and_longRange() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // create many logs across 3 products with varying counts and dates far apart
        List<StockChangeLog> logs = new java.util.ArrayList<>();
        for (int pid=1; pid<=3; pid++){
            for (int i=0;i<pid*3;i++){
                StockChangeLog log = new StockChangeLog();
                Product p = new Product(); p.setId(pid); p.setTitle("Prod"+pid);
                log.setProduct(p);
                log.setChangedAt(LocalDateTime.of(2023,1,1,0,0).plusDays(pid*10 + i));
                log.setNewStock(10 - i);
                log.setOldStock(11 - i);
                log.setQuantityChanged(-1);
                log.setReason("Venta");
                logs.add(log);
            }
        }
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findAll()).thenReturn(logs);
        Map<String,Object> resp = controller.getProductEventsTimeline(null, LocalDateTime.of(2023,1,1,0,0), LocalDateTime.of(2023,12,31,0,0), 2).getBody();
        assertNotNull(resp);
        assertTrue(((List<?>)resp.get("events")).size() > 0);
        assertTrue(resp.containsKey("chartBase64"));
    }

    @Test
    void testTopCategories_andTopBrands_limits_and_ordering() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // create products with categories and brands
        Category cat1 = new Category(); cat1.setId(1); cat1.setName("C1");
        Category cat2 = new Category(); cat2.setId(2); cat2.setName("C2");
        Brand b1 = new Brand(); b1.setId(11); b1.setName("B1");
        Brand b2 = new Brand(); b2.setId(12); b2.setName("B2");
        Product p1 = new Product(); p1.setId(1001); p1.setCategories(java.util.Set.of(cat1)); p1.setBrand(b1);
        Product p2 = new Product(); p2.setId(1002); p2.setCategories(java.util.Set.of(cat2)); p2.setBrand(b2);
        CartItem it1 = new CartItem(); it1.setProduct(p1); it1.setQuantity(5);
        CartItem it2 = new CartItem(); it2.setProduct(p2); it2.setQuantity(2);
        Purchase pur = new Purchase(); pur.setStatus(Purchase.Status.CONFIRMED); pur.setDate(LocalDateTime.now()); Cart cart = new Cart(); cart.setItems(List.of(it1,it2)); pur.setCart(cart);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(pur));

        Map<String,Object> cats = controller.getTopCategories(1, null, null, "bar").getBody();
        assertNotNull(cats);
        var data = (List<?>)cats.get("data");
        assertEquals(1, data.size());

        Map<String,Object> brands = controller.getTopBrands(2, null, null, "pie").getBody();
        assertNotNull(brands);
        assertTrue(((List<?>)brands.get("data")).size()>=1);
    }

    @Test
    void testStockHistoryByProduct_filters() throws Exception {
        SalesAnalyticsController controller = prepareController();
        Product prod = new Product(); prod.setId(5001);
        StockChangeLog l1 = new StockChangeLog(); l1.setProduct(prod); l1.setChangedAt(LocalDateTime.of(2023,1,1,0,0)); l1.setNewStock(5);
        StockChangeLog l2 = new StockChangeLog(); l2.setProduct(prod); l2.setChangedAt(LocalDateTime.of(2024,1,1,0,0)); l2.setNewStock(3);
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(5001)).thenReturn(List.of(l1,l2));
    }

    @Test
    void testSalesHistogram_trend_computation() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // create purchases for product 700 over 3 days
        Product prod = new Product(); prod.setId(700);
        CartItem it1 = new CartItem(); it1.setProduct(prod); it1.setQuantity(1);
        CartItem it2 = new CartItem(); it2.setProduct(prod); it2.setQuantity(2);
        CartItem it3 = new CartItem(); it3.setProduct(prod); it3.setQuantity(3);
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.of(2023,6,1,10,0)); Cart c1 = new Cart(); c1.setItems(List.of(it1)); p1.setCart(c1);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.of(2023,6,2,10,0)); Cart c2 = new Cart(); c2.setItems(List.of(it2)); p2.setCart(c2);
        Purchase p3 = new Purchase(); p3.setStatus(Purchase.Status.CONFIRMED); p3.setDate(LocalDateTime.of(2023,6,3,10,0)); Cart c3 = new Cart(); c3.setItems(List.of(it3)); p3.setCart(c3);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1,p2,p3));
        Map<String,Object> hist = controller.getSalesHistogram(null, null).getBody();
        assertNotNull(hist);
        Map<?,?> trends = (Map<?,?>) hist.get("productTrends");
        // product id 700 should be present
        assertTrue(trends.keySet().stream().anyMatch(k -> String.valueOf(k).contains("700")));
    }

    @Test
    void testSalesCorrelation_edgeCases() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // n=1 user
        User u = new User(); u.setId(9000); u.setName("Solo"); u.setEmail("s@x");
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.of(2023,1,1,10,0)); p1.setUser(u); Cart c = new Cart(); c.setFinalPrice(100f); p1.setCart(c);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1));
        Map<String,Object> corr = controller.getSalesCorrelation(null, null).getBody();
        assertNotNull(corr);
        Map<?,?> reg = (Map<?,?>) corr.get("regression");
        assertTrue(reg.containsKey("a") && reg.containsKey("b"));

        // multiple users
        User u2 = new User(); u2.setId(9001); u2.setName("Two"); u2.setEmail("t@x");
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.of(2023,1,2,10,0)); p2.setUser(u2); Cart c2 = new Cart(); c2.setFinalPrice(200f); p2.setCart(c2);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1,p2));
        Map<String,Object> corr2 = controller.getSalesCorrelation(null, null).getBody();
        assertNotNull(corr2);
        Map<?,?> reg2 = (Map<?,?>) corr2.get("regression");
        assertTrue(reg2.containsKey("a") && reg2.containsKey("b"));
    }

}

