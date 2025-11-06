package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SalesAnalyticsControllerCategoriesBrandsTest {
    SalesAnalyticsController controller;
    @Mock PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        controller = new SalesAnalyticsController();
        controller.purchaseService = purchaseService;
    }

    @Test
    void getTopCategories_and_brands_countedCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        Category cat = new Category(); cat.setName("Electro");
        Brand b = new Brand(); b.setName("MarcaX");
        Product p = new Product(); p.setId(1); p.setBrand(b); p.setCategories(java.util.Set.of(cat));
        CartItem ci = new CartItem(); ci.setProduct(p); ci.setQuantity(2);
        Cart cart = new Cart(); cart.setItems(List.of(ci));
        Purchase purchase = new Purchase(); purchase.setStatus(Purchase.Status.CONFIRMED); purchase.setDate(now); purchase.setCart(cart);

        when(purchaseService.getAllPurchases()).thenReturn(List.of(purchase));

        Map<String,Object> topCat = controller.getTopCategories(10, null, null, null);
        List<?> dataCat = (List<?>) topCat.get("data");
        assertEquals(1, dataCat.size());
        Map<?,?> entryCat = (Map<?,?>) dataCat.get(0);
        assertEquals("Electro", entryCat.get("category"));
        assertEquals(2, entryCat.get("cantidadVendida"));

        Map<String,Object> topBrand = controller.getTopBrands(10, null, null, null);
        List<?> dataBrand = (List<?>) topBrand.get("data");
        assertEquals(1, dataBrand.size());
        Map<?,?> entryBrand = (Map<?,?>) dataBrand.get(0);
        assertEquals("MarcaX", entryBrand.get("brand"));
        assertEquals(2, entryBrand.get("cantidadVendida"));
    }

//    @Test
//    void getTopProducts_limitAndTitleFallback() {
//        // product 1 sells 5 units, product 2 sells 3 units
//        LocalDateTime now = LocalDateTime.now();
//        Product prod1 = new Product(); prod1.setId(1); prod1.setTitle("Top1");
//        Product prod2 = new Product(); prod2.setId(2); prod2.setTitle(null);
//
//        CartItem ci1 = new CartItem(); ci1.setProduct(prod1); ci1.setQuantity(5);
//        CartItem ci2 = new CartItem(); ci2.setProduct(prod2); ci2.setQuantity(3);
//        Cart cart1 = new Cart(); cart1.setItems(List.of(ci1));
//        Cart cart2 = new Cart(); cart2.setItems(List.of(ci2));
//        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(now); p1.setCart(cart1);
//        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(now); p2.setCart(cart2);
//
//        when(purchaseService.getAllPurchases()).thenReturn(List.of(p1,p2));
//        // product repository used to resolve titles
//        ar.edu.uade.analytics.Repository.ProductRepository prodRepo = mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
//        when(purchaseService.getProductRepository()).thenReturn(prodRepo);
//        when(prodRepo.findById(1)).thenReturn(java.util.Optional.of(prod1));
//        when(prodRepo.findById(2)).thenReturn(java.util.Optional.of(prod2));
//
//        Map<String,Object> top = controller.getTopProducts(1, null, null);
//        @SuppressWarnings("unchecked")
//        List<Map<String,Object>> data = (List<Map<String,Object>>) top.get("data");
//        assertEquals(1, data.size());
//        Map<String,Object> first = data.get(0);
//        assertEquals(1, first.get("productId"));
//        assertEquals(5, first.get("cantidadVendida"));
//        assertEquals("Top1", first.get("title"));
//    }

    @Test
    void getSalesSummary_filtersByDateRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inRange = now.minusDays(1);
        LocalDateTime outRange = now.minusDays(40);

        Product prod = new Product(); prod.setId(10);
        CartItem ci = new CartItem(); ci.setProduct(prod); ci.setQuantity(1);
        Cart cart = new Cart(); cart.setItems(List.of(ci)); cart.setFinalPrice(100f);

        Purchase inside = new Purchase(); inside.setStatus(Purchase.Status.CONFIRMED); inside.setDate(inRange); inside.setCart(cart); inside.setUser(new User(){ { setId(1); } });
        Purchase outside = new Purchase(); outside.setStatus(Purchase.Status.CONFIRMED); outside.setDate(outRange); outside.setCart(cart);

        when(purchaseService.getAllPurchases()).thenReturn(List.of(inside, outside));

        Map<String,Object> summary = controller.getSalesSummary(inRange.minusDays(1), inRange.plusDays(1));
        assertEquals(1, summary.get("totalVentas"));
        assertEquals(100f, (Float)summary.get("facturacionTotal"));
        assertEquals(1, summary.get("clientesActivos"));
    }

    @Test
    void getTopBrands_ignoresNullBrand() {
        LocalDateTime now = LocalDateTime.now();
        Product prod = new Product(); prod.setId(2); prod.setBrand(null);
        CartItem ci = new CartItem(); ci.setProduct(prod); ci.setQuantity(4);
        Cart cart = new Cart(); cart.setItems(List.of(ci));
        Purchase purchase = new Purchase(); purchase.setStatus(Purchase.Status.CONFIRMED); purchase.setDate(now); purchase.setCart(cart);
        when(purchaseService.getAllPurchases()).thenReturn(List.of(purchase));

        Map<String,Object> topBrand = controller.getTopBrands(10, null, null, null);
        List<?> dataBrand = (List<?>) topBrand.get("data");
        assertTrue(dataBrand.isEmpty());
    }

    @Test
    void getTopProducts_limitZero_returnsEmpty() {
        when(purchaseService.getAllPurchases()).thenReturn(List.of());
        Map<String,Object> top = controller.getTopProducts(0, null, null);
        List<?> data = (List<?>) top.get("data");
        assertTrue(data.isEmpty());
    }

    @Test
    void getTopProducts_titleFallbackForNullTitle() {
        LocalDateTime now = LocalDateTime.now();
        Product prod1 = new Product(); prod1.setId(1); prod1.setTitle(null);
        CartItem ci1 = new CartItem(); ci1.setProduct(prod1); ci1.setQuantity(2);
        Cart cart = new Cart(); cart.setItems(List.of(ci1));
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(now); p1.setCart(cart);
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p1));
        ar.edu.uade.analytics.Repository.ProductRepository prodRepo = mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        when(purchaseService.getProductRepository()).thenReturn(prodRepo);
        when(prodRepo.findById(1)).thenReturn(java.util.Optional.of(prod1));

        Map<String,Object> top = controller.getTopProducts(5, null, null);
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> data = (List<Map<String,Object>>) top.get("data");
        assertEquals(1, data.size());
        assertTrue(((String)data.get(0).get("title")).startsWith("ID "));
    }

    @Test
    void getTrend_withPreviousPeriod_populatesBothCurrentAndPrevious() {
        LocalDateTime end = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime start = end.minusDays(1);
        // one purchase in current window
        Purchase cur = new Purchase(); cur.setStatus(Purchase.Status.CONFIRMED); cur.setDate(end);
        // one purchase in previous window
        Purchase prev = new Purchase(); prev.setStatus(Purchase.Status.CONFIRMED); prev.setDate(start.minusDays(2));
        when(purchaseService.getAllPurchases()).thenReturn(List.of(cur, prev));

        Map<String,Object> res = controller.getTrend(start, end);
        List<?> current = (List<?>) res.get("current");
        List<?> previous = (List<?>) res.get("previous");
        assertNotNull(current);
        assertNotNull(previous);
    }

    @Test
    void getSalesSummary_withNullDates_usesDefaultWindow_and_returnsMap() {
        // create one purchase within default window
        LocalDateTime now = LocalDateTime.now();
        Product prod = new Product(); prod.setId(5);
        CartItem ci = new CartItem(); ci.setProduct(prod); ci.setQuantity(1);
        Cart cart = new Cart(); cart.setItems(List.of(ci)); cart.setFinalPrice(50f);
        Purchase purchase = new Purchase(); purchase.setStatus(Purchase.Status.CONFIRMED); purchase.setDate(now); purchase.setCart(cart);
        when(purchaseService.getAllPurchases()).thenReturn(List.of(purchase));

        Map<String,Object> summary = controller.getSalesSummary(null, null);
        assertTrue(summary.containsKey("totalVentas"));
        assertTrue(summary.containsKey("facturacionTotal"));
    }

    @Test
    void nonConfirmedPurchases_areIgnored_inSummariesAndTrends() {
        LocalDateTime now = LocalDateTime.now();
        Purchase p = new Purchase(); p.setStatus(Purchase.Status.PENDING); p.setDate(now);
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));

        Map<String,Object> summary = controller.getSalesSummary(now.minusDays(1), now.plusDays(1));
        assertEquals(0, summary.get("totalVentas"));

        Map<String,Object> trend = controller.getTrend(now.minusDays(1), now.plusDays(1));
        List<?> current = (List<?>) trend.get("current");
        // current should have entries but zero ventas
        boolean anySales = current.stream().anyMatch(r -> ((Number)((Map)r).get("ventas")).intValue() > 0);
        assertFalse(anySales);
    }

    @Test
    void getTrend_multipleDays_accumulatesAcrossDays() {
        LocalDateTime start = LocalDateTime.now().minusDays(3).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusDays(2);
        Purchase a = new Purchase(); a.setStatus(Purchase.Status.CONFIRMED); a.setDate(start.plusDays(0));
        Purchase b = new Purchase(); b.setStatus(Purchase.Status.CONFIRMED); b.setDate(start.plusDays(1));
        Purchase c = new Purchase(); c.setStatus(Purchase.Status.CONFIRMED); c.setDate(start.plusDays(2));
        when(purchaseService.getAllPurchases()).thenReturn(List.of(a,b,c));

        Map<String,Object> res = controller.getTrend(start, end);
        List<?> current = (List<?>) res.get("current");
        assertEquals(3, current.size());
        int totalVentas = current.stream().mapToInt(r -> ((Number)((Map)r).get("ventas")).intValue()).sum();
        assertEquals(3, totalVentas);
    }

//    @Test
//    void getDailySales_handlesPurchaseWithNullCart_gracefully() {
//        LocalDateTime day = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
//        Purchase p = new Purchase(); p.setStatus(Purchase.Status.CONFIRMED); p.setDate(day);
//        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
//
//        Map<String,Object> res = controller.getDailySales(day, day);
//        // should produce maps even if no revenue/units
//        assertNotNull(res.get("dailyUnits"));
//        assertNotNull(res.get("dailyRevenue"));
//    }

    @Test
    void getTopCategories_returnsEmpty_whenNoCategoriesPresent() {
        LocalDateTime now = LocalDateTime.now();
        Product prod = new Product(); prod.setId(7); prod.setCategories(null);
        CartItem ci = new CartItem(); ci.setProduct(prod); ci.setQuantity(2);
        Cart cart = new Cart(); cart.setItems(List.of(ci));
        Purchase purchase = new Purchase(); purchase.setStatus(Purchase.Status.CONFIRMED); purchase.setDate(now); purchase.setCart(cart);
        when(purchaseService.getAllPurchases()).thenReturn(List.of(purchase));

        Map<String,Object> topCat = controller.getTopCategories(10, null, null, null);
        List<?> data = (List<?>) topCat.get("data");
        assertTrue(data.isEmpty());
    }

    @Test
    void getSalesSummary_formattingAndMilesRounded() {
        LocalDateTime now = LocalDateTime.now();
        Product prod = new Product(); prod.setId(8);
        CartItem ci = new CartItem(); ci.setProduct(prod); ci.setQuantity(1);
        Cart cart = new Cart(); cart.setItems(List.of(ci)); cart.setFinalPrice(123456f);
        Purchase purchase = new Purchase(); purchase.setStatus(Purchase.Status.CONFIRMED); purchase.setDate(now); purchase.setCart(cart);
        when(purchaseService.getAllPurchases()).thenReturn(List.of(purchase));

        Map<String,Object> summary = controller.getSalesSummary(now.minusDays(1), now.plusDays(1));
        assertEquals(123456f, (Float) summary.get("facturacionTotal"));
        assertTrue(((String)summary.get("facturacionTotalFormateado")).contains("$") || ((String)summary.get("facturacionTotalFormateado")).contains("$"));
        // facturacionTotalEnMiles ~ 123.46
        assertEquals(Math.round((123456f/1000f)*100f)/100f, (Float)summary.get("facturacionTotalEnMiles"));
    }

    @Test
    void getTopProducts_multipleOrderingAndLimit() {
        LocalDateTime now = LocalDateTime.now();
        Product p1 = new Product(); p1.setId(1); p1.setTitle("A");
        Product p2 = new Product(); p2.setId(2); p2.setTitle("B");
        CartItem i1 = new CartItem(); i1.setProduct(p1); i1.setQuantity(2);
        CartItem i2 = new CartItem(); i2.setProduct(p2); i2.setQuantity(3);
        Purchase pu1 = new Purchase(); pu1.setStatus(Purchase.Status.CONFIRMED); pu1.setDate(now); pu1.setCart(new Cart(){ { setItems(List.of(i1,i2)); } });
        when(purchaseService.getAllPurchases()).thenReturn(List.of(pu1));
        ar.edu.uade.analytics.Repository.ProductRepository prodRepo = mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        when(purchaseService.getProductRepository()).thenReturn(prodRepo);
        when(prodRepo.findById(1)).thenReturn(java.util.Optional.of(p1));
        when(prodRepo.findById(2)).thenReturn(java.util.Optional.of(p2));

        Map<String,Object> top = controller.getTopProducts(2, null, null);
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> data = (List<Map<String,Object>>) top.get("data");
        assertEquals(2, data.size());
        // product 2 should be first (quantity 3)
        assertEquals(2, data.get(0).get("productId"));
        assertEquals(3, data.get(0).get("cantidadVendida"));
    }

    @Test
    void getTopBrands_handlesTies_and_includesBoth() {
        LocalDateTime now = LocalDateTime.now();
        Brand b1 = new Brand(); b1.setName("B1");
        Brand b2 = new Brand(); b2.setName("B2");
        Product prod1 = new Product(); prod1.setId(11); prod1.setBrand(b1);
        Product prod2 = new Product(); prod2.setId(12); prod2.setBrand(b2);
        CartItem ci1 = new CartItem(); ci1.setProduct(prod1); ci1.setQuantity(2);
        CartItem ci2 = new CartItem(); ci2.setProduct(prod2); ci2.setQuantity(2);
        Cart cart = new Cart(); cart.setItems(List.of(ci1,ci2));
        Purchase purchase = new Purchase(); purchase.setStatus(Purchase.Status.CONFIRMED); purchase.setDate(now); purchase.setCart(cart);
        when(purchaseService.getAllPurchases()).thenReturn(List.of(purchase));

        Map<String,Object> res = controller.getTopBrands(10, null, null, null);
        List<?> data = (List<?>) res.get("data");
        assertEquals(2, data.size());
    }

    @Test
    void getTopProducts_limitGreaterThanAvailable_returnsAllProducts() {
        LocalDateTime now = LocalDateTime.now();
        Product prod1 = new Product(); prod1.setId(100); prod1.setTitle("P1");
        Product prod2 = new Product(); prod2.setId(101); prod2.setTitle("P2");
        CartItem i1 = new CartItem(); i1.setProduct(prod1); i1.setQuantity(1);
        CartItem i2 = new CartItem(); i2.setProduct(prod2); i2.setQuantity(2);
        Purchase pu = new Purchase(); pu.setStatus(Purchase.Status.CONFIRMED); pu.setDate(now); pu.setCart(new Cart(){ { setItems(List.of(i1,i2)); } });
        when(purchaseService.getAllPurchases()).thenReturn(List.of(pu));
        ar.edu.uade.analytics.Repository.ProductRepository prodRepo = mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        when(purchaseService.getProductRepository()).thenReturn(prodRepo);
        when(prodRepo.findById(100)).thenReturn(java.util.Optional.of(prod1));
        when(prodRepo.findById(101)).thenReturn(java.util.Optional.of(prod2));

        Map<String,Object> top = controller.getTopProducts(10, null, null);
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> data = (List<Map<String,Object>>) top.get("data");
        assertEquals(2, data.size());
    }

    @Test
    void getSalesSummary_countsUniqueClients() {
        LocalDateTime now = LocalDateTime.now();
        User u = new User(); u.setId(55);
        Cart cart = new Cart(); cart.setFinalPrice(10f);
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(now); p1.setCart(cart); p1.setUser(u);
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(now); p2.setCart(cart); p2.setUser(u);
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p1,p2));

        Map<String,Object> summary = controller.getSalesSummary(now.minusDays(1), now.plusDays(1));
        assertEquals(2, summary.get("totalVentas"));
        assertEquals(1, summary.get("clientesActivos"));
    }

//    @Test
//    void getDailySales_multiplePurchasesSameDay_sumsValues() {
//        LocalDateTime day = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
//        Product prod = new Product(); prod.setId(1);
//        CartItem it1 = new CartItem(); it1.setProduct(prod); it1.setQuantity(2);
//        CartItem it2 = new CartItem(); it2.setProduct(prod); it2.setQuantity(3);
//        Cart c1 = new Cart(); c1.setItems(List.of(it1)); c1.setFinalPrice(20f);
//        Cart c2 = new Cart(); c2.setItems(List.of(it2)); c2.setFinalPrice(30f);
//        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(day.plusHours(2)); p1.setCart(c1);
//        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(day.plusHours(6)); p2.setCart(c2);
//        when(purchaseService.getAllPurchases()).thenReturn(List.of(p1,p2));
//
//        Map<String,Object> res = controller.getDailySales(day, day);
//        @SuppressWarnings("unchecked")
//        Map<String,Integer> units = (Map<String,Integer>) res.get("dailyUnits");
//        @SuppressWarnings("unchecked")
//        Map<String,Float> revenue = (Map<String,Float>) res.get("dailyRevenue");
//        String key = day.toLocalDate().toString();
//        assertEquals(5, units.get(key).intValue());
//        assertEquals(50f, revenue.get(key));
//    }

}
