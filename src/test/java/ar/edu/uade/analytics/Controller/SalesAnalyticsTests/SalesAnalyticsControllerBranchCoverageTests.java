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
public class SalesAnalyticsControllerBranchCoverageTests {

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
    void salesSummaryChart_types_and_dateFilter() throws Exception {
        SalesAnalyticsController controller = prepareController();
        CartItem it = new CartItem(); Product p = new Product(); p.setId(9001); it.setProduct(p); it.setQuantity(2);
        Cart cart = new Cart(); cart.setItems(List.of(it)); cart.setFinalPrice(300f);
        Purchase pur = new Purchase(); pur.setStatus(Purchase.Status.CONFIRMED); pur.setDate(LocalDateTime.of(2025,1,10,10,0)); pur.setCart(cart);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(pur));

        byte[] bar = controller.getSalesSummaryChart("bar", LocalDateTime.of(2025,1,1,0,0), LocalDateTime.of(2025,1,31,23,59)).getBody();
        assertNotNull(bar);
        byte[] pie = controller.getSalesSummaryChart("pie", LocalDateTime.of(2025,1,1,0,0), LocalDateTime.of(2025,1,31,23,59)).getBody();
        assertNotNull(pie);
    }

    @Test
    void topCategories_and_brands_tie_and_chartTypes() throws Exception {
        SalesAnalyticsController controller = prepareController();
        Category c1 = new Category(); c1.setId(11); c1.setName("C1");
        Category c2 = new Category(); c2.setId(12); c2.setName("C2");
        Brand b1 = new Brand(); b1.setId(21); b1.setName("B1");
        Brand b2 = new Brand(); b2.setId(22); b2.setName("B2");
        Product prod1 = new Product(); prod1.setId(101); prod1.setCategories(java.util.Set.of(c1)); prod1.setBrand(b1);
        Product prod2 = new Product(); prod2.setId(102); prod2.setCategories(java.util.Set.of(c2)); prod2.setBrand(b2);
        CartItem it1 = new CartItem(); it1.setProduct(prod1); it1.setQuantity(3);
        CartItem it2 = new CartItem(); it2.setProduct(prod2); it2.setQuantity(3);
        Cart cart = new Cart(); cart.setItems(List.of(it1, it2));
        Purchase p = new Purchase(); p.setStatus(Purchase.Status.CONFIRMED); p.setDate(LocalDateTime.now()); p.setCart(cart);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p));

        Map<String,Object> catsBar = controller.getTopCategories(10, null, null, "bar").getBody();
        assertNotNull(catsBar); assertTrue(((List<?>)catsBar.get("data")).size()>=1);
        Map<String,Object> catsPie = controller.getTopCategories(10, null, null, "pie").getBody();
        assertNotNull(catsPie); assertNotNull(catsPie.get("chartBase64"));

        Map<String,Object> brandsBar = controller.getTopBrands(10, null, null, "bar").getBody();
        assertNotNull(brandsBar); assertTrue(((List<?>)brandsBar.get("data")).size()>=1);
        Map<String,Object> brandsPie = controller.getTopBrands(10, null, null, "pie").getBody();
        assertNotNull(brandsPie); assertNotNull(brandsPie.get("chartBase64"));
    }

    @Test
    void topCustomers_ordering_and_lambdaComparator() throws Exception {
        SalesAnalyticsController controller = prepareController();
        User u1 = new User(); u1.setId(1); u1.setName("A"); u1.setEmail("a@x");
        User u2 = new User(); u2.setId(2); u2.setName("B"); u2.setEmail("b@x");
        Cart c1 = new Cart(); c1.setFinalPrice(500f); Cart c2 = new Cart(); c2.setFinalPrice(100f);
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setUser(u1); p1.setDate(LocalDateTime.of(2025,6,1,10,0)); p1.setCart(c1);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setUser(u2); p2.setDate(LocalDateTime.of(2025,6,2,10,0)); p2.setCart(c2);
        Purchase p3 = new Purchase(); p3.setStatus(Purchase.Status.CONFIRMED); p3.setUser(u1); p3.setDate(LocalDateTime.of(2025,6,3,10,0)); p3.setCart(c1);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1,p2,p3));

        Map<String,Object> resp = controller.getTopCustomers(10, null, null).getBody();
        assertNotNull(resp);
        List<?> data = (List<?>) resp.get("data");
        assertTrue(data.size()>=1);
        // first should be user 1 (higher gasto)
        Map<?,?> first = (Map<?,?>) data.get(0);
        assertEquals(1, ((Number)first.get("userId")).intValue());
    }

    @Test
    void stockHistoryByProductCode_profitBranch_and_nonVenta() throws Exception {
        SalesAnalyticsController controller = prepareController();
        var prodRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        Product p = new Product(); p.setId(321); p.setPrice(20f);
        org.mockito.Mockito.lenient().when(prodRepo.findByProductCode(321)).thenReturn(p);
        org.mockito.Mockito.lenient().when(purchaseService.getProductRepository()).thenReturn(prodRepo);
        StockChangeLog l1 = new StockChangeLog(); l1.setProduct(p); l1.setChangedAt(LocalDateTime.of(2025,3,1,9,0)); l1.setReason("Venta"); l1.setQuantityChanged(2); l1.setNewStock(8); l1.setOldStock(10);
        StockChangeLog l2 = new StockChangeLog(); l2.setProduct(p); l2.setChangedAt(LocalDateTime.of(2025,3,2,9,0)); l2.setReason("Ajuste"); l2.setQuantityChanged(1); l2.setNewStock(9); l2.setOldStock(8);
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(321)).thenReturn(List.of(l1,l2));

        Map<String,Object> resp = controller.getStockHistoryByProductCode(321, true, null, null).getBody();
        assertNotNull(resp);
        List<?> data = (List<?>) resp.get("data");
        assertEquals(2, data.size());
        Map<?,?> first = (Map<?,?>) data.get(0);
        assertTrue(first.containsKey("profit"));
        Map<?,?> second = (Map<?,?>) data.get(1);
        assertFalse(second.containsKey("profit"));
    }

    @Test
    void salesHistogram_and_productTrends() throws Exception {
        SalesAnalyticsController controller = prepareController();
        Product prod = new Product(); prod.setId(700);
        CartItem it1 = new CartItem(); it1.setProduct(prod); it1.setQuantity(1);
        CartItem it2 = new CartItem(); it2.setProduct(prod); it2.setQuantity(2);
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.of(2025,6,1,10,0)); Cart c1 = new Cart(); c1.setItems(List.of(it1)); p1.setCart(c1);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.of(2025,6,2,10,0)); Cart c2 = new Cart(); c2.setItems(List.of(it2)); p2.setCart(c2);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1,p2));

        Map<String,Object> hist = controller.getSalesHistogram(null, null).getBody();
        assertNotNull(hist);
        Map<?,?> trends = (Map<?,?>) hist.get("productTrends");
        assertTrue(trends.containsKey(700));
    }

    @Test
    void dailySales_bar_and_line_render() throws Exception {
        SalesAnalyticsController controller = prepareController();
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.of(2025,7,1,10,0));
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.of(2025,7,2,11,0));
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1,p2));

        Map<String,Object> line = controller.getDailySales(null, null, "line").getBody();
        assertNotNull(line);
        Map<String,Object> bar = controller.getDailySales(null, null, "bar").getBody();
        assertNotNull(bar);
    }

    @Test
    void productEventsTimeline_topN_selection() throws Exception {
        SalesAnalyticsController controller = prepareController();
        List<StockChangeLog> logs = new java.util.ArrayList<>();
        for (int pid=1; pid<=5; pid++){
            for (int j=0;j<pid;j++){
                StockChangeLog log = new StockChangeLog(); Product pr = new Product(); pr.setId(pid); pr.setTitle("P"+pid);
                log.setProduct(pr); log.setChangedAt(LocalDateTime.of(2025,1,1,9,0).plusDays(j)); log.setNewStock(10);
                logs.add(log);
            }
        }
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findAll()).thenReturn(logs);
        Map<String,Object> resp = controller.getProductEventsTimeline(null, LocalDateTime.of(2025,1,1,0,0), LocalDateTime.of(2025,12,31,0,0), 3).getBody();
        assertNotNull(resp);
        assertTrue(((List<?>)resp.get("events")).size() > 0);
    }

    @Test
    void topProducts_titleFallback() throws Exception {
        SalesAnalyticsController controller = prepareController();
        Product p1 = new Product(); p1.setId(1); p1.setTitle("T1");
        Product p2 = new Product(); p2.setId(2); // no title
        CartItem it1 = new CartItem(); it1.setProduct(p1); it1.setQuantity(2);
        CartItem it2 = new CartItem(); it2.setProduct(p2); it2.setQuantity(1);
        Cart cart = new Cart(); cart.setItems(List.of(it1,it2));
        Purchase pur = new Purchase(); pur.setStatus(Purchase.Status.CONFIRMED); pur.setDate(LocalDateTime.now()); pur.setCart(cart);
        var prodRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(purchaseService.getProductRepository()).thenReturn(prodRepo);
        org.mockito.Mockito.lenient().when(prodRepo.findById(1)).thenReturn(java.util.Optional.of(p1));
        org.mockito.Mockito.lenient().when(prodRepo.findById(2)).thenReturn(java.util.Optional.empty());
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(pur));

        Map<String,Object> resp = controller.getTopProducts(10, null, null).getBody();
        assertNotNull(resp);
        List<?> data = (List<?>) resp.get("data");
        assertEquals(2, data.size());
        Map<?,?> second = (Map<?,?>) data.get(1);
        assertTrue(((String)second.get("title")).contains("ID"));
    }
}

