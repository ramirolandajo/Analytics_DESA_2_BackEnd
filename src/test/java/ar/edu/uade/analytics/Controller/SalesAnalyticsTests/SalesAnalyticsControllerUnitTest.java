package ar.edu.uade.analytics.Controller.SalesAnalyticsTests;

import ar.edu.uade.analytics.Controller.SalesAnalyticsController;
import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Service.PurchaseService;
import ar.edu.uade.analytics.Repository.StockChangeLogRepository;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SalesAnalyticsControllerUnitTest {

    @Mock
    PurchaseService purchaseService;

    @Mock
    StockChangeLogRepository stockChangeLogRepository;

    @Test
    void testApplyStyles_privateMethods_and_getSalesSummary_direct() throws Exception {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        // inyectar mocks por reflection
        Field f1 = SalesAnalyticsController.class.getDeclaredField("purchaseService");
        f1.setAccessible(true);
        f1.set(controller, purchaseService);
        Field f2 = SalesAnalyticsController.class.getDeclaredField("stockChangeLogRepository");
        f2.setAccessible(true);
        f2.set(controller, stockChangeLogRepository);

        // Preparar dato simple
        Purchase p = new Purchase();
        p.setStatus(Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        Cart c = new Cart();
        c.setFinalPrice(123f);
        p.setCart(c);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p));

        // Llamar getSalesSummary con chartType pie y bar
        Map<String, Object> respPie = controller.getSalesSummary(null, null, "pie").getBody();
        assertNotNull(respPie);
        assertTrue(respPie.containsKey("chartBase64"));

        Map<String, Object> respBar = controller.getSalesSummary(null, null, "bar").getBody();
        assertNotNull(respBar);
        assertTrue(respBar.containsKey("chartBase64"));

        // Invocar métodos privados de estilo via reflexión (smoke test)
        JFreeChart chart = controller.createBarChart(1, 100f, 2);
        assertNotNull(chart);
        JFreeChart pie = controller.createPieChart(1, 100f, 2);
        assertNotNull(pie);
        // aplicar estilo a pie plot
        PiePlot<?> plotObj = (PiePlot<?>) pie.getPlot();
        if (plotObj != null) {
            Method applyPie = SalesAnalyticsController.class.getDeclaredMethod("applyPieChartStyle", JFreeChart.class, PiePlot.class);
            applyPie.setAccessible(true);
            applyPie.invoke(controller, pie, plotObj);
        }
    }

    @Test
    void testGetDailySales_and_TopCustomers_direct() throws Exception {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        Field f1 = SalesAnalyticsController.class.getDeclaredField("purchaseService");
        f1.setAccessible(true);
        f1.set(controller, purchaseService);

        // Daily sales data
        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.of(2023,1,1,10,0));
        Purchase p2 = new Purchase(); p2.setStatus(Purchase.Status.CONFIRMED); p2.setDate(LocalDateTime.of(2023,1,1,11,0));
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1, p2));

        Map<String, Object> daily = controller.getDailySales(null, null, "line").getBody();
        assertNotNull(daily);
        assertTrue(daily.containsKey("data"));

        // Top customers
        Purchase c1 = new Purchase(); c1.setStatus(Purchase.Status.CONFIRMED); c1.setDate(LocalDateTime.now());
        User u = new User(); u.setId(1); u.setName("X"); u.setEmail("x@x"); c1.setUser(u);
        Cart cart = new Cart(); cart.setFinalPrice(200f); c1.setCart(cart);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(c1));
        Map<String, Object> topCust = controller.getTopCustomers(10, null, null).getBody();
        assertNotNull(topCust);
        assertTrue(topCust.containsKey("data"));
    }

    @Test
    void testGetProductsDashboard_and_histogram_and_correlation_direct() throws Exception {
        SalesAnalyticsController controller = new SalesAnalyticsController();
        Field f1 = SalesAnalyticsController.class.getDeclaredField("purchaseService");
        f1.setAccessible(true);
        f1.set(controller, purchaseService);
        Field f2 = SalesAnalyticsController.class.getDeclaredField("stockChangeLogRepository");
        f2.setAccessible(true);
        f2.set(controller, stockChangeLogRepository);

        // product and logs
        Product prod = new Product(); prod.setId(55); prod.setTitle("P"); prod.setStock(9);
        ar.edu.uade.analytics.Repository.ProductRepository productRepo = org.mockito.Mockito.mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(productRepo.findAll()).thenReturn(List.of(prod));
        org.mockito.Mockito.lenient().when(purchaseService.getProductRepository()).thenReturn(productRepo);

        Purchase p1 = new Purchase(); p1.setStatus(Purchase.Status.CONFIRMED); p1.setDate(LocalDateTime.of(2023,1,1,10,0));
        Cart c1 = new Cart(); CartItem it = new CartItem(); it.setProduct(prod); it.setQuantity(3); c1.setItems(List.of(it)); p1.setCart(c1);
        org.mockito.Mockito.lenient().when(purchaseService.getAllPurchases()).thenReturn(List.of(p1));

        // stock logs
        StockChangeLog log = new StockChangeLog(); log.setProduct(prod); log.setChangedAt(LocalDateTime.of(2023,1,1,9,0)); log.setNewStock(12);
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(55)).thenReturn(List.of(log));

        Map<String, Object> resp = controller.getProductsDashboard(null, null, null, null).getBody();
        assertNotNull(resp);
        assertEquals(1, resp.get("totalProductos"));

        Map<String, Object> hist = controller.getSalesHistogram(null, null).getBody();
        assertNotNull(hist);
        assertTrue(hist.containsKey("productTrends"));

        Map<String, Object> corr = controller.getSalesCorrelation(null, null).getBody();
        assertNotNull(corr);
        assertTrue(corr.containsKey("regression"));
    }
}
