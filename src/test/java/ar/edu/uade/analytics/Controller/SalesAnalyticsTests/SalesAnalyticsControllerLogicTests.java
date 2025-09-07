package ar.edu.uade.analytics.Controller.SalesAnalyticsTests;

import ar.edu.uade.analytics.Controller.SalesAnalyticsController;
import ar.edu.uade.analytics.Entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SalesAnalyticsControllerLogicTests {

    private SalesAnalyticsController controller() {
        return new SalesAnalyticsController();
    }

    @Test
    void computeProductSalesFromPurchases_filtersByDateAndCategoryAndBrand() throws Exception {
        SalesAnalyticsController controller = controller();
        // Product with category 10 and brand 20
        Product prod1 = new Product(); prod1.setId(1);
        Category cat = new Category(); cat.setId(10); cat.setName("C10");
        prod1.setCategories(java.util.Set.of(cat));
        Brand brand = new Brand(); brand.setId(20); brand.setName("B20");
        prod1.setBrand(brand);
        CartItem item1 = new CartItem(); item1.setProduct(prod1); item1.setQuantity(2);
        Cart cart1 = new Cart(); cart1.setItems(List.of(item1)); cart1.setFinalPrice(100f);
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.of(2023,5,2,10,0)); p1.setCart(cart1);

        // Product without matching category/brand
        Product prod2 = new Product(); prod2.setId(2);
        CartItem item2 = new CartItem(); item2.setProduct(prod2); item2.setQuantity(5);
        Cart cart2 = new Cart(); cart2.setItems(List.of(item2)); cart2.setFinalPrice(50f);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.of(2023,5,3,10,0)); p2.setCart(cart2);

        // Purchase outside date range
        Product prod3 = new Product(); prod3.setId(3);
        CartItem item3 = new CartItem(); item3.setProduct(prod3); item3.setQuantity(7);
        Cart cart3 = new Cart(); cart3.setItems(List.of(item3));
        Purchase p3 = new Purchase(); p3.setStatus(Purchase.Status.CONFIRMED); p3.setDate(LocalDateTime.of(2022,1,1,10,0)); p3.setCart(cart3);

        List<Purchase> purchases = List.of(p1, p2, p3);
        // filter to 2023-05-01..2023-05-31 and categoryId=10 and brandId=20 -> only prod1 counted
        Map<Integer, Integer> result = controller.computeProductSalesFromPurchases(purchases, LocalDateTime.of(2023,5,1,0,0), LocalDateTime.of(2023,5,31,23,59), 10, 20);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(1));
        assertEquals(2, result.get(1));

        // Without filters should include prod1 and prod2 (p3 out of range)
        Map<Integer, Integer> all = controller.computeProductSalesFromPurchases(purchases, LocalDateTime.of(2023,5,1,0,0), LocalDateTime.of(2023,5,31,23,59), null, null);
        assertEquals(2, all.size());
        assertEquals(2, all.get(1));
        assertEquals(5, all.get(2));
    }

    @Test
    void computeHistogramFromUserCounts_buildsBucketsCorrectly() throws Exception {
        SalesAnalyticsController controller = controller();
        Map<Integer, Integer> userCounts = new HashMap<>();
        userCounts.put(1, 1); // 1-2
        userCounts.put(2, 2); // 1-2
        userCounts.put(3, 3); // 3-5
        userCounts.put(4, 4); // 3-5
        userCounts.put(5, 6); // 6+
        userCounts.put(6, 10); // 6+

        Map<String, Integer> histogram = controller.computeHistogramFromUserCounts(userCounts);
        assertNotNull(histogram);
        assertEquals(2, histogram.getOrDefault("1-2", 0));
        assertEquals(2, histogram.getOrDefault("3-5", 0));
        assertEquals(2, histogram.getOrDefault("6+", 0));
    }

    @Test
    void computeRegressionFromXY_handlesEmptyAndSinglePoint() throws Exception {
        SalesAnalyticsController controller = controller();
        // empty
        Map<String, Double> regEmpty = controller.computeRegressionFromXY(List.of(), List.of());
        assertNotNull(regEmpty);
        assertTrue(regEmpty.containsKey("a") && regEmpty.containsKey("b"));
        // single point
        Map<String, Double> regSingle = controller.computeRegressionFromXY(List.of(1f), List.of(100f));
        assertNotNull(regSingle);
        assertTrue(regSingle.containsKey("a") && regSingle.containsKey("b"));
    }
}

