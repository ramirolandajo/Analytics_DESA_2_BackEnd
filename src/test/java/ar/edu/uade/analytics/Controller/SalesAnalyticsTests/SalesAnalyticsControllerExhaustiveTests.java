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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SalesAnalyticsControllerExhaustiveTests {

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
    void productsDashboard_manyProducts_manyLogs_longRange_evolution() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // create several products with categories/brands and many logs across months
        List<Product> products = new ArrayList<>();
        List<StockChangeLog> allLogs = new ArrayList<>();
        for (int i=1;i<=6;i++){
            Product p = new Product(); p.setId(1000+i); p.setTitle("Prod"+i); p.setStock(10+i);
            Category cat = new Category(); cat.setId(200+i); cat.setName("Cat"+(i%3));
            p.setCategories(java.util.Set.of(cat));
            Brand b = new Brand(); b.setId(300+i); b.setName("Brand"+(i%2));
            p.setBrand(b);
            products.add(p);
            // create logs spread over 40 days to force dateLabels loop to iterate many times
            for (int d=0; d<40; d+=5) {
                StockChangeLog log = new StockChangeLog();
                log.setProduct(p);
                log.setChangedAt(LocalDateTime.of(2023,1,1,9,0).plusDays(d).plusDays(i));
                log.setOldStock(20-d);
                log.setNewStock(20-d-1);
                log.setQuantityChanged(-1);
                log.setReason(d%2==0?"Venta":"Ajuste");
                allLogs.add(log);
            }
        }
        var prodRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(prodRepo.findAll()).thenReturn(products);
        org.mockito.Mockito.lenient().when(purchaseService.getProductRepository()).thenReturn(prodRepo);
        // create purchases that make some top products
        List<Purchase> purchases = new ArrayList<>();
        for (int i=1;i<=6;i++){
            CartItem it = new CartItem(); it.setProduct(products.get(i-1)); it.setQuantity(i);
            Cart cart = new Cart(); cart.setItems(List.of(it)); cart.setFinalPrice(10f*i);
            Purchase pur = new Purchase(); pur.setStatus(Purchase.Status.CONFIRMED); pur.setDate(LocalDateTime.of(2023,1,i,10,0)); pur.setCart(cart);
            purchases.add(pur);
        }
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(purchases);
        // stub logs per product id
        for (Product p : products) {
            org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(p.getId())).thenReturn(allLogs.stream().filter(l->l.getProduct().getId().equals(p.getId())).toList());
        }
        // for timeline without productId, return all logs
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findAll()).thenReturn(allLogs);

        Map<String,Object> resp = controller.getProductsDashboard(LocalDateTime.of(2023,1,1,0,0), LocalDateTime.of(2023,12,31,23,59), null, null).getBody();
        assertNotNull(resp);
        assertTrue(((Number)resp.get("totalProductos")).intValue()>=6);
        // evolutionChartBase64 should exist (since we have top products with logs)
        assertTrue(resp.containsKey("evolutionChartBase64"));
    }

    @Test
    void productEventsTimeline_topProduct_selection() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // create logs for many products with differing frequencies
        List<StockChangeLog> logs = new ArrayList<>();
        for (int pid=1; pid<=8; pid++){
            int freq = pid; // different frequencies
            for (int j=0;j<freq;j++){
                StockChangeLog log = new StockChangeLog();
                Product p = new Product(); p.setId(pid); p.setTitle("P"+pid);
                log.setProduct(p);
                log.setChangedAt(LocalDateTime.of(2023,2,1,9,0).plusDays(j));
                log.setNewStock(50-j);
                log.setOldStock(51-j);
                log.setQuantityChanged(-1);
                log.setReason("Venta");
                logs.add(log);
            }
        }
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findAll()).thenReturn(logs);
        // request topN = 3 should pick products with highest event counts
        Map<String,Object> resp = controller.getProductEventsTimeline(null, LocalDateTime.of(2023,1,1,0,0), LocalDateTime.of(2023,12,31,23,59), 3).getBody();
        assertNotNull(resp);
        List<?> events = (List<?>) resp.get("events");
        assertTrue(events.size()>0);
        assertTrue(resp.containsKey("chartBase64"));
    }

    @Test
    void topCategories_tie_and_chartTypes() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // two categories with equal counts
        Category c1 = new Category(); c1.setId(11); c1.setName("C11");
        Category c2 = new Category(); c2.setId(12); c2.setName("C12");
        Product p1 = new Product(); p1.setId(201); p1.setCategories(java.util.Set.of(c1));
        Product p2 = new Product(); p2.setId(202); p2.setCategories(java.util.Set.of(c2));
        CartItem it1 = new CartItem(); it1.setProduct(p1); it1.setQuantity(3);
        CartItem it2 = new CartItem(); it2.setProduct(p2); it2.setQuantity(3);
        Purchase pur = new Purchase(); pur.setStatus(Purchase.Status.CONFIRMED); pur.setDate(LocalDateTime.now()); Cart cart = new Cart(); cart.setItems(List.of(it1,it2)); pur.setCart(cart);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(pur));
        var resBar = controller.getTopCategories(10, null, null, "bar").getBody();
        assertNotNull(resBar);
        var resPie = controller.getTopCategories(10, null, null, "pie").getBody();
        assertNotNull(resPie);
    }

}

