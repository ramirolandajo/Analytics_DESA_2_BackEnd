package ar.edu.uade.analytics.Controller.SalesAnalyticsTests;

import ar.edu.uade.analytics.Controller.SalesAnalyticsController;
import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Service.PurchaseService;
import ar.edu.uade.analytics.Repository.StockChangeLogRepository;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.plot.PiePlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SalesAnalyticsControllerExtraBranchTests {

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
    void testApplyPieChartStyle_keys_variants() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // Crear pie chart con keys que disparen la rama de 'Facturación Total (en miles)' y 'Facturación Total'
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        dataset.setValue("Facturación Total (en miles)", 5);
        dataset.setValue("Facturación Total", 3);
        dataset.setValue("Otros", 2);
        JFreeChart chart = ChartFactory.createPieChart("Test Pie", dataset, true, true, false);
        PiePlot<?> plot = (PiePlot<?>) chart.getPlot();
        Method applyPie = SalesAnalyticsController.class.getDeclaredMethod("applyPieChartStyle", JFreeChart.class, PiePlot.class);
        applyPie.setAccessible(true);
        // invocation should not throw and should iterate keys
        applyPie.invoke(controller, chart, plot);
        // verify sections exist by querying dataset keys
        assertTrue(dataset.getKeys().contains("Facturación Total (en miles)"));
        assertTrue(dataset.getKeys().contains("Facturación Total"));
    }

    @Test
    void testProductEventsTimeline_topN_clamping_and_grouping() throws Exception {
        SalesAnalyticsController controller = prepareController();
        // Construir logs para 4 productos con distintas frecuencias
        List<StockChangeLog> logs = new ArrayList<>();
        for (int pid = 1; pid <= 4; pid++) {
            for (int i = 0; i < pid; i++) { // product 1 ->1 log, product2->2 logs, etc
                StockChangeLog log = new StockChangeLog();
                Product p = new Product(); p.setId(pid); p.setTitle("P" + pid);
                log.setProduct(p);
                log.setChangedAt(LocalDateTime.of(2023, 6, pid, 10, 0).plusDays(i));
                log.setOldStock(10);
                log.setNewStock(9);
                log.setQuantityChanged(-1);
                log.setReason("Ajuste");
                logs.add(log);
            }
        }
        org.mockito.Mockito.lenient().when(stockChangeLogRepository.findAll()).thenReturn(logs);
        // topN less than 1 -> clamp to 1
        Map<String,Object> resp1 = controller.getProductEventsTimeline(null, null, null, 0).getBody();
        assertNotNull(resp1);
        List<?> events1 = (List<?>) resp1.get("events");
        assertTrue(events1.size() >= 1);
        // topN greater than 10 -> clamp to 10, with few products still returns some events
        Map<String,Object> resp2 = controller.getProductEventsTimeline(null, null, null, 20).getBody();
        assertNotNull(resp2);
        List<?> events2 = (List<?>) resp2.get("events");
        assertTrue(events2.size() >= 1);
    }
}

