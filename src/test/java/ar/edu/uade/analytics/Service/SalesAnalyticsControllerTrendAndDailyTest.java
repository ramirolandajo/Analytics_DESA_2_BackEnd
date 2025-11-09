//package ar.edu.uade.analytics.Service;
//
//import ar.edu.uade.analytics.Entity.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class SalesAnalyticsControllerTrendAndDailyTest {
//    SalesAnalyticsController controller;
//    @Mock PurchaseService purchaseService;
//
//    @BeforeEach
//    void setUp() {
//        controller = new SalesAnalyticsController();
//        controller.purchaseService = purchaseService;
//    }
//
//    @Test
//    void getTrend_singleDay_withOnePurchase_countsOne() {
//        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
//        Purchase p = new Purchase(); p.setStatus(Purchase.Status.CONFIRMED); p.setDate(today.plusHours(3));
//        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
//
//        Map<String, Object> res = controller.getTrend(today, today);
//        List<?> current = (List<?>) res.get("current");
//        assertEquals(1, current.size());
//        Map<?,?> row = (Map<?,?>) current.get(0);
//        assertEquals(1, ((Number)row.get("ventas")).intValue());
//    }
//
//    @Test
//    @SuppressWarnings({"unchecked","rawtypes"})
//    void getDailySales_aggregatesUnitsAndRevenue() {
//        LocalDateTime day = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
//        Product prod = new Product(); prod.setId(1);
//        CartItem item = new CartItem(); item.setProduct(prod); item.setQuantity(3);
//        Cart cart = new Cart(); cart.setItems(List.of(item)); cart.setFinalPrice(150f);
//        Purchase purchase = new Purchase(); purchase.setStatus(Purchase.Status.CONFIRMED); purchase.setDate(day.plusHours(5)); purchase.setCart(cart);
//        when(purchaseService.getAllPurchases()).thenReturn(List.of(purchase));
//
//        Map<String, Object> res = controller.getDailySales(day, day);
//        Map<String, Integer> dailyUnits = (Map) res.get("dailyUnits");
//        Map<String, Float> dailyRevenue = (Map) res.get("dailyRevenue");
//        String key = day.toLocalDate().toString();
//        assertEquals(3, dailyUnits.getOrDefault(key, 0).intValue());
//        assertEquals(150f, dailyRevenue.getOrDefault(key, 0f));
//    }
//}
