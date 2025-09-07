package ar.edu.uade.analytics.Controller.SalesAnalyticsTests;

import ar.edu.uade.analytics.Controller.SalesAnalyticsController;
import ar.edu.uade.analytics.Entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SalesAnalyticsControllerPackageHelpersTest {

    @Test
    void computeCategoryAndBrandSales_basic() {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        Category catA = new Category(); catA.setId(1); catA.setName("A");
        Category catB = new Category(); catB.setId(2); catB.setName("B");
        Brand brandX = new Brand(); brandX.setId(10); brandX.setName("X");

        Product p1 = new Product(); p1.setId(100); p1.setCategories(java.util.Set.of(catA)); p1.setBrand(brandX);
        Product p2 = new Product(); p2.setId(101); p2.setCategories(java.util.Set.of(catB));

        CartItem ci1 = new CartItem(); ci1.setProduct(p1); ci1.setQuantity(2);
        CartItem ci2 = new CartItem(); ci2.setProduct(p2); ci2.setQuantity(3);

        Cart cart = new Cart(); cart.setItems(List.of(ci1, ci2));
        Purchase purchase = new Purchase(); purchase.setStatus(Purchase.Status.CONFIRMED); purchase.setDate(LocalDateTime.now()); purchase.setCart(cart);

        Map<String, Integer> catSales = controller.computeCategorySales(List.of(purchase), null, null);
        // Expect two categories (A and B) accumulated from the cart items
        assertEquals(2, catSales.size());
        assertEquals(2, catSales.get("A").intValue());
        assertEquals(3, catSales.get("B").intValue());

        Map<String, Integer> brandSales = controller.computeBrandSales(List.of(purchase), null, null);
        assertEquals(1, brandSales.size());
        assertEquals(2, brandSales.get("X").intValue());
    }

    @Test
    void computeSalesKPIs_and_dailySalesMap() {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        Product prod = new Product(); prod.setId(200);
        CartItem ci = new CartItem(); ci.setProduct(prod); ci.setQuantity(5);
        Cart cart = new Cart(); cart.setItems(List.of(ci)); cart.setFinalPrice(123.45f);
        Purchase p = new Purchase(); p.setStatus(Purchase.Status.CONFIRMED); p.setDate(LocalDateTime.of(2025,9,1,10,0)); p.setCart(cart);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.of(2025,9,01,11,0)); p2.setCart(cart);

        Map<String, Object> kpi = controller.computeSalesKPIs(List.of(p, p2), null, null);
        assertEquals(2, ((Number)kpi.get("totalVentas")).intValue());
        assertEquals(246.9f, ((Number)kpi.get("facturacionTotal")).floatValue(), 0.1f);
        assertEquals(10, ((Number)kpi.get("productosVendidos")).intValue());

        Map<String, Integer> daily = controller.computeDailySalesMap(List.of(p, p2), null, null);
        assertEquals(1, daily.size());
        assertTrue(daily.values().stream().findFirst().orElse(0) == 2);
    }

    @Test
    void computeStockHistoryData_profitAndNoProfit() {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        Product prod = new Product(); prod.setId(300); prod.setPrice(10f);
        StockChangeLog s1 = new StockChangeLog(); s1.setProduct(prod); s1.setChangedAt(LocalDateTime.of(2025,7,1,9,0)); s1.setReason("Venta"); s1.setQuantityChanged(2); s1.setOldStock(12); s1.setNewStock(10);
        StockChangeLog s2 = new StockChangeLog(); s2.setProduct(prod); s2.setChangedAt(LocalDateTime.of(2025,7,2,9,0)); s2.setReason("Ajuste"); s2.setQuantityChanged(1); s2.setOldStock(10); s2.setNewStock(9);

        List<Map<String, Object>> withProfit = controller.computeStockHistoryData(prod, List.of(s1, s2), true, null, null);
        assertEquals(2, withProfit.size());
        Map<String,Object> first = withProfit.get(0);
        assertTrue(first.containsKey("profit"));
        assertEquals(20f, ((Number)first.get("profit")).floatValue(), 0.001f);
        Map<String,Object> second = withProfit.get(1);
        assertFalse(second.containsKey("profit"));

        List<Map<String, Object>> noProfit = controller.computeStockHistoryData(prod, List.of(s1, s2), false, null, null);
        assertEquals(2, noProfit.size());
        assertFalse(noProfit.get(0).containsKey("profit"));
    }
}
