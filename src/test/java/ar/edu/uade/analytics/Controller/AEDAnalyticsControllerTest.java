package ar.edu.uade.analytics.Controller;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.when;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.io.OutputStream;

@WebMvcTest(AEDAnalyticsController.class)
@Import(AEDAnalyticsControllerTest.MockConfig.class)
public class AEDAnalyticsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ar.edu.uade.analytics.Service.AEDService aedService;
    @Autowired
    private ar.edu.uade.analytics.Service.PurchaseService purchaseService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public ar.edu.uade.analytics.Service.AEDService aedService() {
            return org.mockito.Mockito.mock(ar.edu.uade.analytics.Service.AEDService.class);
        }
        @Bean
        public ar.edu.uade.analytics.Service.PurchaseService purchaseService() {
            return org.mockito.Mockito.mock(ar.edu.uade.analytics.Service.PurchaseService.class);
        }
    }

    @Test
    void contextLoads() {
        // Test de carga de contexto
    }

    @Test
    void testGetSalesStatistics_withData() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVentas", 1);
        stats.put("facturacionTotal", 1000f);
        stats.put("productosVendidos", 2);
        when(aedService.getPurchaseStatistics(List.of(p))).thenReturn(stats);
        mockMvc.perform(get("/analytics/sales/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVentas").value(1));
    }

    @Test
    void testGetSalesStatistics_nullStats() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        when(aedService.getPurchaseStatistics(List.of(p))).thenReturn(null);
        mockMvc.perform(get("/analytics/sales/statistics"))
                .andExpect(status().isOk()); // El controlador debe manejar null y no lanzar excepción
    }

    @Test
    void testGetSalesHistogram_empty() throws Exception {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        when(aedService.getHistogramData(Collections.emptyList())).thenReturn(new HashMap<>());
        mockMvc.perform(get("/analytics/sales/aed-histogram"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chartBase64").exists());
    }

    @Test
    void testGetSalesHistogram_nullMap() throws Exception {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        when(aedService.getHistogramData(Collections.emptyList())).thenReturn(null);
        mockMvc.perform(get("/analytics/sales/aed-histogram"))
                .andExpect(status().isOk()); // El controlador debe manejar null y no lanzar excepción
    }

    @Test
    void testGetSalesHistogram_withDates() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = LocalDateTime.now();
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        when(aedService.getHistogramData(Collections.emptyList())).thenReturn(new HashMap<>());
        mockMvc.perform(get("/analytics/sales/aed-histogram")
                .param("startDate", start.toString())
                .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chartBase64").exists());
    }

    @Test
    void testGetSalesCorrelation_empty() throws Exception {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        when(aedService.getCorrelationData(Collections.emptyList())).thenReturn(new HashMap<>());
        mockMvc.perform(get("/analytics/sales/aed-correlation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chartBase64").exists());
    }

    @Test
    void testGetSalesCorrelation_nullMap() throws Exception {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        when(aedService.getCorrelationData(Collections.emptyList())).thenReturn(null);
        mockMvc.perform(get("/analytics/sales/aed-correlation"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesOutliers_empty() throws Exception {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        when(aedService.getOutliers(Collections.emptyList())).thenReturn(new HashMap<>());
        mockMvc.perform(get("/analytics/sales/outliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chartBase64").exists());
    }

    @Test
    void testGetSalesOutliers_nullMap() throws Exception {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        when(aedService.getOutliers(Collections.emptyList())).thenReturn(null);
        mockMvc.perform(get("/analytics/sales/outliers"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesNulls_empty() throws Exception {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        when(aedService.getNullCounts(Collections.emptyList())).thenReturn(new HashMap<>());
        mockMvc.perform(get("/analytics/sales/nulls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chartBase64").exists());
    }

    @Test
    void testGetSalesNulls_nullMap() throws Exception {
        when(purchaseService.getAllPurchases()).thenReturn(Collections.emptyList());
        when(aedService.getNullCounts(Collections.emptyList())).thenReturn(null);
        mockMvc.perform(get("/analytics/sales/nulls"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesStatistics_variosCaminos() throws Exception {
        // Compra dentro del rango
        ar.edu.uade.analytics.Entity.Purchase p1 = new ar.edu.uade.analytics.Entity.Purchase();
        p1.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.now().minusDays(1));
        // Compra fuera del rango
        ar.edu.uade.analytics.Entity.Purchase p2 = new ar.edu.uade.analytics.Entity.Purchase();
        p2.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p2.setDate(LocalDateTime.now().minusDays(100));
        // Compra con status distinto
        ar.edu.uade.analytics.Entity.Purchase p3 = new ar.edu.uade.analytics.Entity.Purchase();
        p3.setStatus(null);
        p3.setDate(LocalDateTime.now());
        // Compra con fecha nula
        ar.edu.uade.analytics.Entity.Purchase p4 = new ar.edu.uade.analytics.Entity.Purchase();
        p4.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p4.setDate(null);
        var compras = List.of(p1, p2, p3, p4);
        when(purchaseService.getAllPurchases()).thenReturn(compras);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVentas", 1);
        stats.put("facturacionTotal", 1000f);
        stats.put("productosVendidos", 2);
        when(aedService.getPurchaseStatistics(compras)).thenReturn(stats);
        // Sin fechas
        mockMvc.perform(get("/analytics/sales/statistics"))
                .andExpect(status().isOk());
        // Solo startDate
        mockMvc.perform(get("/analytics/sales/statistics").param("startDate", LocalDateTime.now().minusDays(10).toString()))
                .andExpect(status().isOk());
        // Solo endDate
        mockMvc.perform(get("/analytics/sales/statistics").param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
        // Ambos
        mockMvc.perform(get("/analytics/sales/statistics")
                .param("startDate", LocalDateTime.now().minusDays(10).toString())
                .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesHistogram_variosCaminos() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p1 = new ar.edu.uade.analytics.Entity.Purchase();
        p1.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.now().minusDays(1));
        ar.edu.uade.analytics.Entity.Purchase p2 = new ar.edu.uade.analytics.Entity.Purchase();
        p2.setStatus(null);
        p2.setDate(LocalDateTime.now());
        var compras = List.of(p1, p2);
        when(purchaseService.getAllPurchases()).thenReturn(compras);
        Map<String, Object> hist = new HashMap<>();
        hist.put("A", 1);
        when(aedService.getHistogramData(compras)).thenReturn(hist);
        mockMvc.perform(get("/analytics/sales/aed-histogram"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-histogram").param("startDate", LocalDateTime.now().minusDays(10).toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-histogram").param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-histogram")
                .param("startDate", LocalDateTime.now().minusDays(10).toString())
                .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesCorrelation_variosCaminos() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p1 = new ar.edu.uade.analytics.Entity.Purchase();
        p1.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.now().minusDays(1));
        ar.edu.uade.analytics.Entity.Purchase p2 = new ar.edu.uade.analytics.Entity.Purchase();
        p2.setStatus(null);
        p2.setDate(LocalDateTime.now());
        var compras = List.of(p1, p2);
        when(purchaseService.getAllPurchases()).thenReturn(compras);
        Map<String, Object> corr = new HashMap<>();
        corr.put("data", List.of(Map.of("x", 1, "y", 2)));
        when(aedService.getCorrelationData(compras)).thenReturn(corr);
        mockMvc.perform(get("/analytics/sales/aed-correlation"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-correlation").param("startDate", LocalDateTime.now().minusDays(10).toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-correlation").param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-correlation")
                .param("startDate", LocalDateTime.now().minusDays(10).toString())
                .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesNulls_variosCaminos() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p1 = new ar.edu.uade.analytics.Entity.Purchase();
        p1.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.now().minusDays(1));
        ar.edu.uade.analytics.Entity.Purchase p2 = new ar.edu.uade.analytics.Entity.Purchase();
        p2.setStatus(null);
        p2.setDate(LocalDateTime.now());
        var compras = List.of(p1, p2);
        Map<String, Object> nulls = new HashMap<>();
        nulls.put("campo", 5);
        when(purchaseService.getAllPurchases()).thenReturn(compras);
        when(aedService.getNullCounts(compras)).thenReturn(nulls);
        mockMvc.perform(get("/analytics/sales/nulls"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/nulls").param("startDate", LocalDateTime.now().minusDays(10).toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/nulls").param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/nulls")
                .param("startDate", LocalDateTime.now().minusDays(10).toString())
                .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesOutliers_variosCaminos() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p1 = new ar.edu.uade.analytics.Entity.Purchase();
        p1.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.now().minusDays(1));
        ar.edu.uade.analytics.Entity.Purchase p2 = new ar.edu.uade.analytics.Entity.Purchase();
        p2.setStatus(null);
        p2.setDate(LocalDateTime.now());
        var compras = List.of(p1, p2);
        Map<String, Object> outliers = new HashMap<>();
        outliers.put("values", List.of(1, 2, 3));
        when(purchaseService.getAllPurchases()).thenReturn(compras);
        when(aedService.getOutliers(compras)).thenReturn(outliers);
        mockMvc.perform(get("/analytics/sales/outliers"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/outliers").param("startDate", LocalDateTime.now().minusDays(10).toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/outliers").param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/outliers")
                .param("startDate", LocalDateTime.now().minusDays(10).toString())
                .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesStatistics_imageIOException() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVentas", 1);
        stats.put("facturacionTotal", 1000f);
        stats.put("productosVendidos", 2);
        when(aedService.getPurchaseStatistics(List.of(p))).thenReturn(stats);
        try (MockedStatic<ImageIO> mocked = Mockito.mockStatic(ImageIO.class)) {
            mocked.when(() -> ImageIO.write(Mockito.any(BufferedImage.class), Mockito.anyString(), Mockito.any(OutputStream.class))).thenThrow(new java.io.IOException("error"));
            mockMvc.perform(get("/analytics/sales/statistics"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testGetSalesCorrelation_imageIOException() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        Map<String, Object> corr = new HashMap<>();
        corr.put("data", List.of(Map.of("x", 1, "y", 2)));
        when(aedService.getCorrelationData(List.of(p))).thenReturn(corr);
        try (MockedStatic<ImageIO> mocked = Mockito.mockStatic(ImageIO.class)) {
            mocked.when(() -> ImageIO.write(Mockito.any(BufferedImage.class), Mockito.anyString(), Mockito.any(OutputStream.class))).thenThrow(new java.io.IOException("error"));
            mockMvc.perform(get("/analytics/sales/aed-correlation"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testGetSalesHistogram_imageIOException() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        Map<String, Object> hist = new HashMap<>();
        hist.put("A", 1);
        when(aedService.getHistogramData(List.of(p))).thenReturn(hist);
        try (MockedStatic<ImageIO> mocked = Mockito.mockStatic(ImageIO.class)) {
            mocked.when(() -> ImageIO.write(Mockito.any(BufferedImage.class), Mockito.anyString(), Mockito.any(OutputStream.class))).thenThrow(new java.io.IOException("error"));
            mockMvc.perform(get("/analytics/sales/aed-histogram"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testGetSalesNulls_imageIOException() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        Map<String, Object> nulls = new HashMap<>();
        nulls.put("campo", 5);
        when(aedService.getNullCounts(List.of(p))).thenReturn(nulls);
        try (MockedStatic<ImageIO> mocked = Mockito.mockStatic(ImageIO.class)) {
            mocked.when(() -> ImageIO.write(Mockito.any(BufferedImage.class), Mockito.anyString(), Mockito.any(OutputStream.class))).thenThrow(new java.io.IOException("error"));
            mockMvc.perform(get("/analytics/sales/nulls"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testGetSalesOutliers_imageIOException() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        Map<String, Object> outliers = new HashMap<>();
        outliers.put("values", List.of(1, 2, 3));
        when(aedService.getOutliers(List.of(p))).thenReturn(outliers);
        try (MockedStatic<ImageIO> mocked = Mockito.mockStatic(ImageIO.class)) {
            mocked.when(() -> ImageIO.write(Mockito.any(BufferedImage.class), Mockito.anyString(), Mockito.any(OutputStream.class))).thenThrow(new java.io.IOException("error"));
            mockMvc.perform(get("/analytics/sales/outliers"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testGetSalesStatistics_lambdasYFiltrado() throws Exception {
        // Compra dentro del rango y CONFIRMED
        ar.edu.uade.analytics.Entity.Purchase p1 = new ar.edu.uade.analytics.Entity.Purchase();
        p1.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.now().minusDays(1));
        // Compra fuera del rango
        ar.edu.uade.analytics.Entity.Purchase p2 = new ar.edu.uade.analytics.Entity.Purchase();
        p2.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p2.setDate(LocalDateTime.now().minusDays(100));
        // Compra con status distinto
        ar.edu.uade.analytics.Entity.Purchase p3 = new ar.edu.uade.analytics.Entity.Purchase();
        p3.setStatus(null);
        p3.setDate(LocalDateTime.now());
        // Compra con fecha nula
        ar.edu.uade.analytics.Entity.Purchase p4 = new ar.edu.uade.analytics.Entity.Purchase();
        p4.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p4.setDate(null);
        var compras = List.of(p1, p2, p3, p4);
        when(purchaseService.getAllPurchases()).thenReturn(compras);
        // No mockeamos aedService.getPurchaseStatistics para forzar ejecución de lambdas y filtrado
        mockMvc.perform(get("/analytics/sales/statistics"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/statistics").param("startDate", LocalDateTime.now().minusDays(10).toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/statistics").param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/statistics")
                .param("startDate", LocalDateTime.now().minusDays(10).toString())
                .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesCorrelation_lambdasYFiltrado() throws Exception {
        // Compra dentro del rango y CONFIRMED
        ar.edu.uade.analytics.Entity.Purchase p1 = new ar.edu.uade.analytics.Entity.Purchase();
        p1.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.now().minusDays(1));
        // Compra fuera del rango
        ar.edu.uade.analytics.Entity.Purchase p2 = new ar.edu.uade.analytics.Entity.Purchase();
        p2.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p2.setDate(LocalDateTime.now().minusDays(100));
        // Compra con status distinto
        ar.edu.uade.analytics.Entity.Purchase p3 = new ar.edu.uade.analytics.Entity.Purchase();
        p3.setStatus(null);
        p3.setDate(LocalDateTime.now());
        // Compra con fecha nula
        ar.edu.uade.analytics.Entity.Purchase p4 = new ar.edu.uade.analytics.Entity.Purchase();
        p4.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p4.setDate(null);
        var compras = List.of(p1, p2, p3, p4);
        when(purchaseService.getAllPurchases()).thenReturn(compras);
        // No mockeamos aedService.getCorrelationData para forzar ejecución de lambdas y filtrado
        mockMvc.perform(get("/analytics/sales/aed-correlation"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-correlation").param("startDate", LocalDateTime.now().minusDays(10).toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-correlation").param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-correlation")
                .param("startDate", LocalDateTime.now().minusDays(10).toString())
                .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesHistogram_lambdasYFiltrado() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p1 = new ar.edu.uade.analytics.Entity.Purchase();
        p1.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.now().minusDays(1));
        ar.edu.uade.analytics.Entity.Purchase p2 = new ar.edu.uade.analytics.Entity.Purchase();
        p2.setStatus(null);
        p2.setDate(LocalDateTime.now());
        var compras = List.of(p1, p2);
        when(purchaseService.getAllPurchases()).thenReturn(compras);
        // No mockeamos aedService.getHistogramData para forzar ejecución de lambdas y filtrado
        mockMvc.perform(get("/analytics/sales/aed-histogram"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-histogram").param("startDate", LocalDateTime.now().minusDays(10).toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-histogram").param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-histogram")
                .param("startDate", LocalDateTime.now().minusDays(10).toString())
                .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesNulls_lambdasYFiltrado() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p1 = new ar.edu.uade.analytics.Entity.Purchase();
        p1.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.now().minusDays(1));
        ar.edu.uade.analytics.Entity.Purchase p2 = new ar.edu.uade.analytics.Entity.Purchase();
        p2.setStatus(null);
        p2.setDate(LocalDateTime.now());
        var compras = List.of(p1, p2);
        when(purchaseService.getAllPurchases()).thenReturn(compras);
        // No mockeamos aedService.getNullCounts para forzar ejecución de lambdas y filtrado
        mockMvc.perform(get("/analytics/sales/nulls"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/nulls").param("startDate", LocalDateTime.now().minusDays(10).toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/nulls").param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/nulls")
                .param("startDate", LocalDateTime.now().minusDays(10).toString())
                .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesOutliers_lambdasYFiltrado() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p1 = new ar.edu.uade.analytics.Entity.Purchase();
        p1.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p1.setDate(LocalDateTime.now().minusDays(1));
        ar.edu.uade.analytics.Entity.Purchase p2 = new ar.edu.uade.analytics.Entity.Purchase();
        p2.setStatus(null);
        p2.setDate(LocalDateTime.now());
        var compras = List.of(p1, p2);
        when(purchaseService.getAllPurchases()).thenReturn(compras);
        // No mockeamos aedService.getOutliers para forzar ejecución de lambdas y filtrado
        mockMvc.perform(get("/analytics/sales/outliers"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/outliers").param("startDate", LocalDateTime.now().minusDays(10).toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/outliers").param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/outliers")
                .param("startDate", LocalDateTime.now().minusDays(10).toString())
                .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testFiltradoFechasTodosBranches_enEndpoints() throws Exception {
        // Compra con fecha null
        ar.edu.uade.analytics.Entity.Purchase pNull = new ar.edu.uade.analytics.Entity.Purchase();
        pNull.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        pNull.setDate(null);
        // Compra justo en el límite inferior
        LocalDateTime start = LocalDateTime.now().minusDays(5);
        LocalDateTime end = LocalDateTime.now().plusDays(5);
        ar.edu.uade.analytics.Entity.Purchase pStart = new ar.edu.uade.analytics.Entity.Purchase();
        pStart.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        pStart.setDate(start);
        // Compra justo en el límite superior
        ar.edu.uade.analytics.Entity.Purchase pEnd = new ar.edu.uade.analytics.Entity.Purchase();
        pEnd.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        pEnd.setDate(end);
        // Compra dentro del rango
        ar.edu.uade.analytics.Entity.Purchase pIn = new ar.edu.uade.analytics.Entity.Purchase();
        pIn.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        pIn.setDate(start.plusDays(2));
        // Compra fuera del rango (antes)
        ar.edu.uade.analytics.Entity.Purchase pBefore = new ar.edu.uade.analytics.Entity.Purchase();
        pBefore.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        pBefore.setDate(start.minusDays(1));
        // Compra fuera del rango (después)
        ar.edu.uade.analytics.Entity.Purchase pAfter = new ar.edu.uade.analytics.Entity.Purchase();
        pAfter.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        pAfter.setDate(end.plusDays(1));
        var compras = List.of(pNull, pStart, pEnd, pIn, pBefore, pAfter);
        // Para nulls
        Map<String, Object> nulls = new HashMap<>();
        nulls.put("campo", 5);
        when(purchaseService.getAllPurchases()).thenReturn(compras);
        when(aedService.getNullCounts(compras)).thenReturn(nulls);
        mockMvc.perform(get("/analytics/sales/nulls")
                .param("startDate", start.toString())
                .param("endDate", end.toString()))
                .andExpect(status().isOk());
        // Para outliers
        Map<String, Object> outliers = new HashMap<>();
        outliers.put("values", List.of(1, 2, 3));
        when(aedService.getOutliers(compras)).thenReturn(outliers);
        mockMvc.perform(get("/analytics/sales/outliers")
                .param("startDate", start.toString())
                .param("endDate", end.toString()))
                .andExpect(status().isOk());
        // Para histograma
        Map<String, Object> hist = new HashMap<>();
        hist.put("A", 1);
        when(aedService.getHistogramData(compras)).thenReturn(hist);
        mockMvc.perform(get("/analytics/sales/aed-histogram")
                .param("startDate", start.toString())
                .param("endDate", end.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesStatistics_mapVacioYValoresNulos() throws Exception {
        // Mapa vacío
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        when(aedService.getPurchaseStatistics(List.of(p))).thenReturn(new HashMap<>());
        mockMvc.perform(get("/analytics/sales/statistics"))
                .andExpect(status().isOk());
        // Mapa con valores nulos
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVentas", null);
        stats.put("facturacionTotal", null);
        stats.put("productosVendidos", null);
        when(aedService.getPurchaseStatistics(List.of(p))).thenReturn(stats);
        mockMvc.perform(get("/analytics/sales/statistics"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesCorrelation_mapVacioYValoresNulos() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        when(aedService.getCorrelationData(List.of(p))).thenReturn(new HashMap<>());
        mockMvc.perform(get("/analytics/sales/aed-correlation"))
                .andExpect(status().isOk());
        Map<String, Object> corr = new HashMap<>();
        corr.put("data", null);
        when(aedService.getCorrelationData(List.of(p))).thenReturn(corr);
        mockMvc.perform(get("/analytics/sales/aed-correlation"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSalesStatistics_bytesLengthZero() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVentas", 1);
        stats.put("facturacionTotal", 1000f);
        stats.put("productosVendidos", 2);
        when(aedService.getPurchaseStatistics(List.of(p))).thenReturn(stats);
        try (MockedStatic<ImageIO> mocked = Mockito.mockStatic(ImageIO.class)) {
            mocked.when(() -> ImageIO.write(Mockito.any(BufferedImage.class), Mockito.anyString(), Mockito.any(OutputStream.class)))
                    .thenAnswer(invocation -> {
                        // No escribe nada en el OutputStream, así el array queda vacío
                        return true;
                    });
            mockMvc.perform(get("/analytics/sales/statistics"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testGetSalesCorrelation_dataNullOVacia() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        // data nulo
        Map<String, Object> corrNull = new HashMap<>();
        corrNull.put("data", null);
        when(aedService.getCorrelationData(List.of(p))).thenReturn(corrNull);
        mockMvc.perform(get("/analytics/sales/aed-correlation"))
                .andExpect(status().isOk());
        // data vacío
        Map<String, Object> corrVacio = new HashMap<>();
        corrVacio.put("data", List.of());
        when(aedService.getCorrelationData(List.of(p))).thenReturn(corrVacio);
        mockMvc.perform(get("/analytics/sales/aed-correlation"))
                .andExpect(status().isOk());
        // data con valores nulos
        Map<String, Object> punto1 = new HashMap<>();
        punto1.put("x", null);
        punto1.put("y", 2);
        Map<String, Object> punto2 = new HashMap<>();
        punto2.put("x", 1);
        punto2.put("y", null);
        Map<String, Object> corrNulos = new HashMap<>();
        corrNulos.put("data", List.of(punto1, punto2));
        when(aedService.getCorrelationData(List.of(p))).thenReturn(corrNulos);
        mockMvc.perform(get("/analytics/sales/aed-correlation"))
                .andExpect(status().isOk());
    }

    @Test
    void testApplyPieChartStyle_claveEspecialValorNull() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        org.jfree.data.general.DefaultPieDataset<String> dataset = new org.jfree.data.general.DefaultPieDataset<>();
        dataset.setValue("Facturación Total (en miles)", null);
        JFreeChart chart = ChartFactory.createPieChart("Test Pie", dataset, false, false, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        java.lang.reflect.Method m = AEDAnalyticsController.class.getDeclaredMethod("applyPieChartStyle", JFreeChart.class, PiePlot.class);
        m.setAccessible(true);
        m.invoke(controller, chart, plot);
        // No debe lanzar excepción aunque el valor sea null
        assertEquals(java.awt.Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testFiltradoFechasTodosBranches_100porciento() throws Exception {
        // Compra con fecha null
        ar.edu.uade.analytics.Entity.Purchase pNull = new ar.edu.uade.analytics.Entity.Purchase();
        pNull.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        pNull.setDate(null);
        // Compra justo en el límite inferior
        LocalDateTime start = LocalDateTime.now().minusDays(5);
        LocalDateTime end = LocalDateTime.now().plusDays(5);
        ar.edu.uade.analytics.Entity.Purchase pStart = new ar.edu.uade.analytics.Entity.Purchase();
        pStart.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        pStart.setDate(start);
        // Compra justo en el límite superior
        ar.edu.uade.analytics.Entity.Purchase pEnd = new ar.edu.uade.analytics.Entity.Purchase();
        pEnd.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        pEnd.setDate(end);
        // Compra dentro del rango
        ar.edu.uade.analytics.Entity.Purchase pIn = new ar.edu.uade.analytics.Entity.Purchase();
        pIn.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        pIn.setDate(start.plusDays(2));
        // Compra fuera del rango (antes)
        ar.edu.uade.analytics.Entity.Purchase pBefore = new ar.edu.uade.analytics.Entity.Purchase();
        pBefore.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        pBefore.setDate(start.minusDays(1));
        // Compra fuera del rango (después)
        ar.edu.uade.analytics.Entity.Purchase pAfter = new ar.edu.uade.analytics.Entity.Purchase();
        pAfter.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        pAfter.setDate(end.plusDays(1));
        var compras = List.of(pNull, pStart, pEnd, pIn, pBefore, pAfter);
        // No mockeamos aedService para forzar ejecución real del filtro
        when(purchaseService.getAllPurchases()).thenReturn(compras);
        when(aedService.getPurchaseStatistics(compras)).thenReturn(new HashMap<>());
        when(aedService.getCorrelationData(compras)).thenReturn(new HashMap<>());
        // Sin fechas
        mockMvc.perform(get("/analytics/sales/statistics")).andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-correlation")).andExpect(status().isOk());
        // Solo startDate
        mockMvc.perform(get("/analytics/sales/statistics").param("startDate", start.toString())).andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-correlation").param("startDate", start.toString())).andExpect(status().isOk());
        // Solo endDate
        mockMvc.perform(get("/analytics/sales/statistics").param("endDate", end.toString())).andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-correlation").param("endDate", end.toString())).andExpect(status().isOk());
        // Ambos
        mockMvc.perform(get("/analytics/sales/statistics").param("startDate", start.toString()).param("endDate", end.toString())).andExpect(status().isOk());
        mockMvc.perform(get("/analytics/sales/aed-correlation").param("startDate", start.toString()).param("endDate", end.toString())).andExpect(status().isOk());
    }

    @Test
    void testGetSalesCorrelation_imageIOException_sinData() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        Map<String, Object> corr = new HashMap<>(); // sin clave 'data'
        when(aedService.getCorrelationData(List.of(p))).thenReturn(corr);
        try (MockedStatic<ImageIO> mocked = Mockito.mockStatic(ImageIO.class)) {
            mocked.when(() -> ImageIO.write(Mockito.any(BufferedImage.class), Mockito.anyString(), Mockito.any(OutputStream.class))).thenThrow(new java.io.IOException("error"));
            mockMvc.perform(get("/analytics/sales/aed-correlation"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testGetSalesCorrelation_imageIOException_dataNull() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        Map<String, Object> corr = new HashMap<>();
        corr.put("data", null); // clave 'data' presente pero null
        when(aedService.getCorrelationData(List.of(p))).thenReturn(corr);
        try (MockedStatic<ImageIO> mocked = Mockito.mockStatic(ImageIO.class)) {
            mocked.when(() -> ImageIO.write(Mockito.any(BufferedImage.class), Mockito.anyString(), Mockito.any(OutputStream.class))).thenThrow(new java.io.IOException("error"));
            mockMvc.perform(get("/analytics/sales/aed-correlation"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testGetSalesCorrelation_dataContieneNoMap() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase p = new ar.edu.uade.analytics.Entity.Purchase();
        p.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
        p.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(p));
        // 'data' contiene un String en vez de un Map
        Map<String, Object> corr = new HashMap<>();
        corr.put("data", List.of("no es un map"));
        when(aedService.getCorrelationData(List.of(p))).thenReturn(corr);
        mockMvc.perform(get("/analytics/sales/aed-correlation"))
                .andExpect(status().isOk());
    }
}
