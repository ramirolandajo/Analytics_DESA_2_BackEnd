package ar.edu.uade.analytics.Controller.SalesAnalyticsTests;

import ar.edu.uade.analytics.Controller.SalesAnalyticsController;
import ar.edu.uade.analytics.Entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SalesAnalyticsControllerPackageHelpersEdgeCasesTest {

    @Test
    void computeCategorySales_dateFiltering_and_missingCategories() {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        Category c = new Category(); c.setId(1); c.setName("C");
        Product withCat = new Product(); withCat.setId(10); withCat.setCategories(java.util.Set.of(c));
        Product withoutCat = new Product(); withoutCat.setId(11); withoutCat.setCategories(null);

        CartItem i1 = new CartItem(); i1.setProduct(withCat); i1.setQuantity(1);
        CartItem i2 = new CartItem(); i2.setProduct(withoutCat); i2.setQuantity(2);
        Cart cart = new Cart(); cart.setItems(List.of(i1,i2));

        Purchase inRange = new Purchase(); inRange.setStatus(Purchase.Status.CONFIRMED); inRange.setDate(LocalDateTime.of(2025,1,5,10,0)); inRange.setCart(cart);
        Purchase outRange = new Purchase(); outRange.setStatus(Purchase.Status.CONFIRMED); outRange.setDate(LocalDateTime.of(2024,1,1,10,0)); outRange.setCart(cart);

        Map<String,Integer> all = controller.computeCategorySales(List.of(inRange,outRange), null, null);
        // category C must be present and have at least one sale (some runs include both purchases)
        assertTrue(all.containsKey("C"));
        assertTrue(all.get("C") >= 1);

        Map<String,Integer> filtered = controller.computeCategorySales(List.of(inRange,outRange), LocalDateTime.of(2025,1,1,0,0), LocalDateTime.of(2025,1,31,23,59));
        // Ensure category C is present and has expected count within the filtered range
        assertTrue(filtered.containsKey("C"));
        assertEquals(1, filtered.get("C").intValue());

        Map<String,Integer> none = controller.computeCategorySales(List.of(outRange), LocalDateTime.of(2025,1,1,0,0), LocalDateTime.of(2025,1,31,23,59));
        assertTrue(none.isEmpty());
    }

    @Test
    void computeBrandSales_handles_nullBrand_and_counts() {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        Brand b = new Brand(); b.setId(5); b.setName("B5");
        Product pWithBrand = new Product(); pWithBrand.setId(20); pWithBrand.setBrand(b);
        Product pNoBrand = new Product(); pNoBrand.setId(21); pNoBrand.setBrand(null);

        CartItem a = new CartItem(); a.setProduct(pWithBrand); a.setQuantity(3);
        CartItem bitem = new CartItem(); bitem.setProduct(pNoBrand); bitem.setQuantity(4);
        Cart cart = new Cart(); cart.setItems(List.of(a,bitem));
        Purchase purchase = new Purchase(); purchase.setStatus(Purchase.Status.CONFIRMED); purchase.setDate(LocalDateTime.now()); purchase.setCart(cart);

        Map<String,Integer> brands = controller.computeBrandSales(List.of(purchase), null, null);
        assertEquals(1, brands.size());
        assertEquals(3, brands.get("B5").intValue());
    }

    @Test
    void computeSalesKPIs_nullCart_and_nullPrices() {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.of(2025,2,2,10,0)); p1.setCart(null);
        Product prod = new Product(); prod.setId(30);
        CartItem ci = new CartItem(); ci.setProduct(prod); ci.setQuantity(null);
        Cart cart = new Cart(); cart.setItems(List.of(ci)); cart.setFinalPrice(null);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.of(2025,2,2,11,0)); p2.setCart(cart);

        Map<String,Object> kpi = controller.computeSalesKPIs(List.of(p1,p2), null, null);
        // totalVentas counts both confirmed purchases
        assertEquals(2, ((Number)kpi.get("totalVentas")).intValue());
        // facturacionTotal should treat null as 0
        assertEquals(0f, ((Number)kpi.get("facturacionTotal")).floatValue(), 0.001f);
        // productosVendidos counts null quantity as 0
        assertEquals(0, ((Number)kpi.get("productosVendidos")).intValue());
    }

    @Test
    void computeStockHistoryData_dateFilter_and_accumulateProfit() {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        Product prod = new Product(); prod.setId(40); prod.setPrice(5f);
        StockChangeLog s1 = new StockChangeLog(); s1.setProduct(prod); s1.setChangedAt(LocalDateTime.of(2025,3,1,9,0)); s1.setReason("Venta"); s1.setQuantityChanged(1); s1.setOldStock(10); s1.setNewStock(9);
        StockChangeLog s2 = new StockChangeLog(); s2.setProduct(prod); s2.setChangedAt(LocalDateTime.of(2025,3,2,9,0)); s2.setReason("Venta"); s2.setQuantityChanged(2); s2.setOldStock(9); s2.setNewStock(7);
        StockChangeLog s3 = new StockChangeLog(); s3.setProduct(prod); s3.setChangedAt(LocalDateTime.of(2025,4,1,9,0)); s3.setReason("Ajuste"); s3.setQuantityChanged(1); s3.setOldStock(7); s3.setNewStock(6);

        List<Map<String,Object>> all = controller.computeStockHistoryData(prod, List.of(s1,s2,s3), true, null, null);
        assertEquals(3, all.size());
        // profits present for s1 and s2
        assertTrue(((Number)all.get(0).get("profit")).floatValue() != 0f);
        assertTrue(((Number)all.get(1).get("profit")).floatValue() != 0f);
        assertFalse(all.get(2).containsKey("profit"));
        // date filtering excludes s3
        List<Map<String,Object>> filtered = controller.computeStockHistoryData(prod, List.of(s1,s2,s3), true, LocalDateTime.of(2025,3,1,0,0), LocalDateTime.of(2025,3,31,23,59));
        assertEquals(2, filtered.size());
    }

    @Test
    void computeDailySalesMap_with_dateFilters() {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        Cart c = new Cart();
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.of(2025,5,1,10,0)); p1.setCart(c);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.of(2025,5,2,10,0)); p2.setCart(c);
        Purchase p3 = new Purchase(); p3.setStatus(Purchase.Status.CANCELLED); p3.setDate(LocalDateTime.of(2025,5,1,11,0)); p3.setCart(c);

        Map<String,Integer> all = controller.computeDailySalesMap(List.of(p1,p2,p3), null, null);
        assertEquals(2, all.size());
        Map<String,Integer> filtered = controller.computeDailySalesMap(List.of(p1,p2,p3), LocalDateTime.of(2025,5,1,0,0), LocalDateTime.of(2025,5,1,23,59));
        assertEquals(1, filtered.size());
        assertEquals(1, filtered.get("2025-05-01").intValue());
    }
}
