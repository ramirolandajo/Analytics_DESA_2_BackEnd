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
public class SalesAnalyticsControllerCombinatorialTests {

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
    void combinatorial_exercise_many_branches() throws Exception {
        SalesAnalyticsController controller = prepareController();

        // Prepare a base set of purchases and products to reuse across combinations
        Product prodA = new Product(); prodA.setId(10001); prodA.setTitle("A");
        Product prodB = new Product(); prodB.setId(10002); // no title to cover fallback

        CartItem ciA = new CartItem(); ciA.setProduct(prodA); ciA.setQuantity(2);
        CartItem ciB = new CartItem(); ciB.setProduct(prodB); ciB.setQuantity(1);

        Cart cart1 = new Cart(); cart1.setItems(List.of(ciA)); cart1.setFinalPrice(200f);
        Cart cart2 = new Cart(); cart2.setItems(List.of(ciB)); cart2.setFinalPrice(50f);

        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.of(2023,5,1,10,0)); p1.setCart(cart1);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED);
        p2.setDate(LocalDateTime.of(2023,6,1,11,0)); p2.setCart(cart2);

        // Product repository mock
        var prodRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(purchaseService.getProductRepository()).thenReturn(prodRepo);
        org.mockito.Mockito.lenient().when(prodRepo.findById(10001)).thenReturn(java.util.Optional.of(prodA));
        org.mockito.Mockito.lenient().when(prodRepo.findById(10002)).thenReturn(java.util.Optional.empty());
        org.mockito.Mockito.lenient().when(prodRepo.findByProductCode(12345)).thenReturn(prodA);

        // Stock change logs for various products
        StockChangeLog s1 = new StockChangeLog(); s1.setProduct(prodA); s1.setChangedAt(LocalDateTime.of(2023,5,1,9,0)); s1.setNewStock(10); s1.setOldStock(12); s1.setQuantityChanged(-2); s1.setReason("Venta");
        StockChangeLog s2 = new StockChangeLog(); s2.setProduct(prodB); s2.setChangedAt(LocalDateTime.of(2023,6,1,9,0)); s2.setNewStock(5); s2.setOldStock(6); s2.setQuantityChanged(-1); s2.setReason("Ajuste");
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findAll()).thenReturn(List.of(s1, s2));
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(10001)).thenReturn(List.of(s1));
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(10002)).thenReturn(List.of(s2));

        // Vary parameters to exercise many internal branches
        String[] chartTypes = new String[]{"bar", "pie", "line"};
        int[] topNs = new int[]{-1, 1, 5, 20};
        boolean[] profitFlags = new boolean[]{false, true};

        // Set purchases for scenarios
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1, p2));

        for (String chart : chartTypes) {

            // top categories/brands/chart types
            Map<String, Object> cats = controller.getTopCategories(10, null, null, chart).getBody();
            assertNotNull(cats);
            Map<String, Object> brands = controller.getTopBrands(10, null, null, chart).getBody();
            assertNotNull(brands);
        }

        // Test top-products with and without date filtering
        Map<String, Object> topAll = controller.getTopProducts(10, null, null).getBody();
        assertNotNull(topAll);
        Map<String, Object> topFiltered = controller.getTopProducts(10, LocalDateTime.of(2023,5,1,0,0), LocalDateTime.of(2023,5,2,0,0)).getBody();
        assertNotNull(topFiltered);

        // Test stock history by code with profit flag combos
        for (boolean pf : profitFlags) {
            var res = controller.getStockHistoryByProductCode(12345, pf, null, null).getBody();
            assertNotNull(res);
            assertTrue(res.containsKey("data"));
        }

        // product events timeline with various topN values
        for (int tn : topNs) {
            Map<String, Object> t = controller.getProductEventsTimeline(null, LocalDateTime.of(2023,1,1,0,0), LocalDateTime.of(2023,12,31,0,0), tn).getBody();
            assertNotNull(t);
            assertTrue(t.containsKey("events"));
        }



        // Category growth and top customers
        Map<String, Object> catGrowth = controller.getCategoryGrowth(1, null, null).getBody();
        assertNotNull(catGrowth);
        Map<String, Object> topCust = controller.getTopCustomers(5, null, null).getBody();
        assertNotNull(topCust);

        // Products dashboard with combinations of filters
        Map<String, Object> pd1 = controller.getProductsDashboard(null, null, null, null).getBody();
        assertNotNull(pd1);
        Map<String, Object> pd2 = controller.getProductsDashboard(null, null,  null, null).getBody();
        assertNotNull(pd2);
    }
}

