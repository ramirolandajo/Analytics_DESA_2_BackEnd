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
public class SalesAnalyticsControllerMoreBranchesTest {

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
    void testProductEventsTimeline_minDateEqualsMaxDate_and_shortRange() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // Create logs all on same date -> minDate == maxDate branch
        Product p = new Product(); p.setId(300); p.setTitle("SameDay");
        StockChangeLog lg = new StockChangeLog(); lg.setProduct(p); lg.setChangedAt(LocalDateTime.of(2023,7,7,10,0)); lg.setNewStock(5); lg.setOldStock(7); lg.setQuantityChanged(-2); lg.setReason("Ajuste");
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findAll()).thenReturn(List.of(lg));
        Map<String, Object> resp = controller.getProductEventsTimeline(null, null, null, 5).getBody();
        assertNotNull(resp);
        assertTrue(((List<?>)resp.get("events")).size()>=1);
        assertTrue(resp.containsKey("chartBase64"));

        // Now create logs within a short range (<10 days) to trigger the other expansion branch
        StockChangeLog lg2 = new StockChangeLog(); lg2.setProduct(p); lg2.setChangedAt(LocalDateTime.of(2023,7,10,10,0)); lg2.setNewStock(6); lg2.setOldStock(5); lg2.setQuantityChanged(1); lg2.setReason("Ajuste");
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findAll()).thenReturn(List.of(lg, lg2));
        Map<String, Object> resp2 = controller.getProductEventsTimeline(null, null, null, 5).getBody();
        assertNotNull(resp2);
        assertTrue(((List<?>)resp2.get("events")).size()>=2);
    }

    @Test
    void testLowStockProducts_threshold_and_limit() throws Exception {
        SalesAnalyticsController controller = prepareController();
        Product p1 = new Product(); p1.setId(601); p1.setTitle("A"); p1.setStock(2);
        Product p2 = new Product(); p2.setId(602); p2.setTitle("B"); p2.setStock(6);
        Product p3 = new Product(); p3.setId(603); p3.setTitle("C"); p3.setStock(1);
        var repo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(repo.findAll()).thenReturn(List.of(p1,p2,p3));
        org.mockito.Mockito.lenient().when(purchaseService.getProductRepository()).thenReturn(repo);
        Map<String,Object> resp = controller.getLowStockProducts(5, 2).getBody();
        assertNotNull(resp);
        var data = (List<?>) resp.get("data");
        // should only include p1 and p3 (stock <=5) and limited to 2
        assertEquals(2, data.size());
    }

    @Test
    void testProductsDashboard_withTopProducts_evolutionFilled() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // two products, purchases that make both top
        Product p1 = new Product(); p1.setId(701); p1.setTitle("P701");
        Product p2 = new Product(); p2.setId(702); p2.setTitle("P702");
        var prodRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(prodRepo.findAll()).thenReturn(List.of(p1,p2));
        org.mockito.Mockito.lenient().when(purchaseService.getProductRepository()).thenReturn(prodRepo);
        // purchases
        CartItem it1 = new CartItem(); it1.setProduct(p1); it1.setQuantity(3);
        CartItem it2 = new CartItem(); it2.setProduct(p2); it2.setQuantity(4);
        Cart cart1 = new Cart(); cart1.setItems(List.of(it1));
        Cart cart2 = new Cart(); cart2.setItems(List.of(it2));
        Purchase pu1 = new Purchase(); pu1.setStatus(Purchase.Status.CONFIRMED); pu1.setDate(LocalDateTime.of(2023,1,1,10,0)); pu1.setCart(cart1);
        Purchase pu2 = new Purchase(); pu2.setStatus(Purchase.Status.CONFIRMED); pu2.setDate(LocalDateTime.of(2023,1,2,10,0)); pu2.setCart(cart2);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(pu1, pu2));
        // logs for evolution
        StockChangeLog log1 = new StockChangeLog(); log1.setProduct(p1); log1.setChangedAt(LocalDateTime.of(2023,1,1,9,0)); log1.setNewStock(10);
        StockChangeLog log2 = new StockChangeLog(); log2.setProduct(p2); log2.setChangedAt(LocalDateTime.of(2023,1,2,9,0)); log2.setNewStock(8);
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(701)).thenReturn(List.of(log1));
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(702)).thenReturn(List.of(log2));

        Map<String,Object> resp = controller.getProductsDashboard(null, null, null, null).getBody();
        assertNotNull(resp);
        assertTrue(resp.containsKey("evolutionChartBase64"));
    }

    @Test
    void testSalesSummary_filters_and_chartTypes() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // purchase inside date range
        CartItem it = new CartItem(); Product p = new Product(); p.setId(801); it.setProduct(p); it.setQuantity(2);
        Cart cart = new Cart(); cart.setFinalPrice(250f); cart.setItems(List.of(it));
        Purchase pur = new Purchase(); pur.setStatus(Purchase.Status.CONFIRMED); pur.setDate(LocalDateTime.of(2023,4,4,10,0)); pur.setCart(cart);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(pur));

        Map<String,Object> respBar = controller.getSalesSummary(LocalDateTime.of(2023,4,1,0,0), LocalDateTime.of(2023,4,30,23,59), "bar").getBody();
        assertNotNull(respBar);
        assertTrue(respBar.containsKey("chartBase64"));

        Map<String,Object> respPie = controller.getSalesSummary(LocalDateTime.of(2023,4,1,0,0), LocalDateTime.of(2023,4,30,23,59), "pie").getBody();
        assertNotNull(respPie);
        assertTrue(respPie.containsKey("chartBase64"));
    }
}

