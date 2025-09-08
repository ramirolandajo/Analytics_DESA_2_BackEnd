package ar.edu.uade.analytics.Controller.SalesAnalyticsTests;

import ar.edu.uade.analytics.Controller.SalesAnalyticsController;
import ar.edu.uade.analytics.Entity.*;
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

@ExtendWith(MockitoExtension.class)
public class SalesAnalyticsControllerBruteForceTests {

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

//    @Test
//    void bruteForce_exercise_many_branches() throws Exception {
//        SalesAnalyticsController controller = prepareController();
//
//        // Products with various categories/brands
//        Category catA = new Category(); catA.setId(10); catA.setName("CatA");
//        Category catB = new Category(); catB.setId(11); catB.setName("CatB");
//        Brand brandX = new Brand(); brandX.setId(100); brandX.setName("BrandX");
//
//        Product pa = new Product(); pa.setId(201); pa.setTitle("PA"); pa.setCategories(java.util.Set.of(catA)); pa.setBrand(brandX); pa.setStock(5);
//        Product pb = new Product(); pb.setId(202); pb.setTitle(null); pb.setCategories(java.util.Set.of(catB)); pb.setBrand(null); pb.setStock(2);
//        Product pc = new Product(); pc.setId(203); pc.setTitle("PC"); pc.setCategories(java.util.Set.of(catA, catB)); pc.setBrand(brandX); pc.setStock(0);
//
//        var prodRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
//        org.mockito.Mockito.lenient().when(prodRepo.findAll()).thenReturn(List.of(pa,pb,pc));
//        org.mockito.Mockito.lenient().when(purchaseService.getProductRepository()).thenReturn(prodRepo);
//        org.mockito.Mockito.lenient().when(prodRepo.findById(201)).thenReturn(java.util.Optional.of(pa));
//        org.mockito.Mockito.lenient().when(prodRepo.findById(202)).thenReturn(java.util.Optional.of(pb));
//        org.mockito.Mockito.lenient().when(prodRepo.findById(203)).thenReturn(java.util.Optional.of(pc));
//
//        // Purchases that contribute to categories/brands and customers
//        User u1 = new User(); u1.setId(1); u1.setName("User1"); u1.setEmail("u1@x");
//        User u2 = new User(); u2.setId(2); u2.setName("User2"); u2.setEmail("u2@x");
//
//        CartItem ci1 = new CartItem(); ci1.setProduct(pa); ci1.setQuantity(2);
//        CartItem ci2 = new CartItem(); ci2.setProduct(pb); ci2.setQuantity(3);
//        CartItem ci3 = new CartItem(); ci3.setProduct(pc); ci3.setQuantity(1);
//
//        Cart cart1 = new Cart(); cart1.setItems(List.of(ci1)); cart1.setFinalPrice(200f);
//        Cart cart2 = new Cart(); cart2.setItems(List.of(ci2,ci3)); cart2.setFinalPrice(150f);
//
//        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.of(2025,8,1,10,0)); p1.setUser(u1); p1.setCart(cart1);
//        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.of(2025,8,2,11,0)); p2.setUser(u2); p2.setCart(cart2);
//        Purchase p3 = new Purchase(); p3.setStatus(Purchase.Status.CANCELLED); p3.setDate(LocalDateTime.of(2025,8,3,12,0)); p3.setUser(u1);
//
//        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1,p2,p3));
//
//        // Top categories & brands with both chart types
//        Map<String,Object> catsBar = controller.getTopCategories(10, null, null, "bar").getBody();
//        assertNotNull(catsBar); assertTrue(((List<?>)catsBar.get("data")).size()>=1);
//        Map<String,Object> catsPie = controller.getTopCategories(10, null, null, "pie").getBody();
//        assertNotNull(catsPie); assertNotNull(catsPie.get("chartBase64"));
//
//        Map<String,Object> brandsBar = controller.getTopBrands(10, null, null, "bar").getBody();
//        assertNotNull(brandsBar); assertTrue(((List<?>)brandsBar.get("data")).size()>=0);
//        Map<String,Object> brandsPie = controller.getTopBrands(10, null, null, "pie").getBody();
//        assertNotNull(brandsPie); assertNotNull(brandsPie.get("chartBase64"));
//
//        // Category growth: present and absent
//        Map<String,Object> growthA = controller.getCategoryGrowth(10, null, null).getBody();
//        assertNotNull(growthA); assertTrue(growthA.containsKey("categoryGrowth"));
//        Map<String,Object> growthNot = controller.getCategoryGrowth(9999, null, null).getBody();
//        assertNotNull(growthNot); assertTrue(((Map<?,?>)growthNot.get("categoryGrowth")).isEmpty());
//
//        // Sales summary and chart
//        Map<String,Object> summary = controller.getSalesSummary(null, null, "bar").getBody();
//        assertNotNull(summary); assertTrue(summary.containsKey("totalVentas"));
//        byte[] summaryChart = controller.getSalesSummaryChart("bar", null, null).getBody();
//        assertNotNull(summaryChart);
//
//        // Top products (title fallback for pb)
//        Map<String,Object> topProds = controller.getTopProducts(5, null, null).getBody();
//        assertNotNull(topProds);
//
//        // Sales correlation: one user and two users
//        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1));
//        Map<String,Object> corr1 = controller.getSalesCorrelation(null, null).getBody();
//        assertNotNull(corr1); assertTrue(((Map<?,?>)corr1.get("regression")).containsKey("a"));
//        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1,p2));
//        Map<String,Object> corr2 = controller.getSalesCorrelation(null, null).getBody();
//        assertNotNull(corr2); assertTrue(((Map<?,?>)corr2.get("regression")).containsKey("a"));
//
//        // Stock history by product code profit and non-profit
//        org.mockito.Mockito.lenient().when(prodRepo.findByProductCode(201)).thenReturn(pa);
//        StockChangeLog sl1 = new StockChangeLog(); sl1.setProduct(pa); sl1.setChangedAt(LocalDateTime.of(2025,8,1,9,0)); sl1.setReason("Venta"); sl1.setQuantityChanged(1); sl1.setOldStock(6); sl1.setNewStock(5);
//        StockChangeLog sl2 = new StockChangeLog(); sl2.setProduct(pa); sl2.setChangedAt(LocalDateTime.of(2025,8,2,9,0)); sl2.setReason("Ajuste"); sl2.setQuantityChanged(1); sl2.setOldStock(5); sl2.setNewStock(4);
//        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(pa.getId())).thenReturn(List.of(sl1,sl2));
//        Map<String,Object> stockResp = controller.getStockHistoryByProductCode(pa.getId(), true, null, null).getBody();
//        assertNotNull(stockResp);
//
//        // Daily sales
//        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1,p2));
//        Map<String,Object> dsLine = controller.getDailySales(null, null, "line").getBody(); assertNotNull(dsLine);
//        Map<String,Object> dsBar = controller.getDailySales(null, null, "bar").getBody(); assertNotNull(dsBar);
//
//        // Ensure no exceptions and reasonable outputs
//    }
}

