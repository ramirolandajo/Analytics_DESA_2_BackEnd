package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Service.AEDService;
import ar.edu.uade.analytics.Service.PurchaseService;
import ar.edu.uade.analytics.Entity.Purchase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import java.awt.Font;
import java.awt.Color;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;

@RestController
@RequestMapping("/analytics/sales")
public class AEDAnalyticsController {

    @Autowired
    private AEDService aedService;
    @Autowired
    private PurchaseService purchaseService;

    @GetMapping("/statistics")
    public ResponseEntity<SalesStatisticsResponse> getSalesStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        if (startDate != null || endDate != null) {
            purchases = purchases.stream().filter(p -> {
                LocalDateTime fecha = p.getDate();
                if (fecha == null) return false;
                return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
            }).toList();
        }
        Map<String, Object> stats = aedService.getPurchaseStatistics(purchases);
        if (stats == null) stats = new java.util.HashMap<>();
        Integer totalVentas = (stats.get("totalVentas") instanceof Number) ? ((Number) stats.get("totalVentas")).intValue() : null;
        Float facturacionTotal = (stats.get("facturacionTotal") instanceof Number) ? ((Number) stats.get("facturacionTotal")).floatValue() : null;
        Integer productosVendidos = (stats.get("productosVendidos") instanceof Number) ? ((Number) stats.get("productosVendidos")).intValue() : null;
        String chartBase64 = null;
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            dataset.addValue((Number) stats.getOrDefault("totalVentas", 0), "Resumen", "Total Ventas");
            dataset.addValue((Number) stats.getOrDefault("facturacionTotal", 0), "Resumen", "Facturación Total");
            dataset.addValue((Number) stats.getOrDefault("productosVendidos", 0), "Resumen", "Productos Vendidos");
            JFreeChart chart = ChartFactory.createBarChart(
                    "Resumen de Ventas", "KPI", "Valor", dataset);
            applyBarChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(600, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            if (bytes.length == 0) {
                chartBase64 = null;
            } else {
                chartBase64 = Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            chartBase64 = null;
        }
        SalesStatisticsResponse response = new SalesStatisticsResponse(totalVentas, facturacionTotal, productosVendidos, chartBase64);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/aed-histogram")
    public ResponseEntity<Map<String, Object>> getSalesHistogram(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        if (startDate != null || endDate != null) {
            purchases = purchases.stream().filter(p -> {
                LocalDateTime fecha = p.getDate();
                if (fecha == null) return false;
                return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
            }).toList();
        }
        Map<String, Object> histogram = aedService.getHistogramData(purchases);
        if (histogram == null) return ResponseEntity.ok(new java.util.HashMap<>());
        // Generar gráfico de barras para el histograma
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Map.Entry<String, Object> entry : histogram.entrySet()) {
                Number value = entry.getValue() instanceof Number ? (Number) entry.getValue() : 0;
                dataset.addValue(value, "Histograma", entry.getKey());
            }
            JFreeChart chart = ChartFactory.createBarChart(
                    "Histograma de Ventas", "Rango", "Cantidad", dataset);
            applyBarChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(600, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            histogram.put("chartBase64", base64Image);
        } catch (Exception e) {
            histogram.put("chartBase64", null);
        }
        return ResponseEntity.ok(histogram);
    }

    @GetMapping("/aed-correlation")
    public ResponseEntity<Map<String, Object>> getSalesCorrelation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        if (startDate != null || endDate != null) {
            purchases = purchases.stream().filter(p -> {
                LocalDateTime fecha = p.getDate();
                if (fecha == null) return false;
                return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
            }).toList();
        }
        Map<String, Object> correlation = aedService.getCorrelationData(purchases);
        if (correlation == null) return ResponseEntity.ok(new java.util.HashMap<>());
        // Generar gráfico de dispersión para la correlación
        try {
            org.jfree.data.xy.XYSeries series = new org.jfree.data.xy.XYSeries("Correlación");
            if (correlation.containsKey("data")) {
                List<?> data = (List<?>) correlation.get("data");
                if (data != null) {
                    for (Object obj : data) {
                        if (obj instanceof Map<?, ?> point) {
                            Object xObj = point.get("x");
                            Object yObj = point.get("y");
                            if (xObj instanceof Number x && yObj instanceof Number y) {
                                series.add(x, y);
                            }
                        }
                    }
                }
            }
            org.jfree.data.xy.XYSeriesCollection dataset = new org.jfree.data.xy.XYSeriesCollection(series);
            JFreeChart chart = ChartFactory.createScatterPlot(
                    "Correlación de Variables", "X", "Y", dataset);
            applyScatterChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(600, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            correlation.put("chartBase64", base64Image);
        } catch (Exception e) {
            correlation.put("chartBase64", null);
        }
        return ResponseEntity.ok(correlation);
    }

    @GetMapping("/outliers")
    public ResponseEntity<Map<String, Object>> getSalesOutliers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        if (startDate != null || endDate != null) {
            purchases = purchases.stream().filter(p -> {
                LocalDateTime fecha = p.getDate();
                if (fecha == null) return false;
                return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
            }).toList();
        }
        Map<String, Object> outliers = aedService.getOutliers(purchases);
        if (outliers == null) return ResponseEntity.ok(new java.util.HashMap<>());
        // Generar gráfico de caja para outliers
        try {
            org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset dataset = new org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset();
            if (outliers.containsKey("values")) {
                List<Number> values = (List<Number>) outliers.get("values");
                dataset.add(values, "Outliers", "Valores");
            }
            JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
                    "Detección de Outliers", "Categoría", "Valor", dataset, false);
            applyBoxPlotStyle(chart);
            BufferedImage image = chart.createBufferedImage(600, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            outliers.put("chartBase64", base64Image);
        } catch (Exception e) {
            outliers.put("chartBase64", null);
        }
        return ResponseEntity.ok(outliers);
    }

    @GetMapping("/nulls")
    public ResponseEntity<Map<String, Object>> getSalesNulls(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        if (startDate != null || endDate != null) {
            purchases = purchases.stream().filter(p -> {
                LocalDateTime fecha = p.getDate();
                if (fecha == null) return false;
                return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
            }).toList();
        }
        Map<String, Object> nulls = aedService.getNullCounts(purchases);
        if (nulls == null) return ResponseEntity.ok(new java.util.HashMap<>());
        // Generar gráfico de barras para nulls
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Map.Entry<String, Object> entry : nulls.entrySet()) {
                Number value = entry.getValue() instanceof Number ? (Number) entry.getValue() : 0;
                dataset.addValue(value, "Nulls", entry.getKey());
            }
            JFreeChart chart = ChartFactory.createBarChart(
                    "Valores Nulos por Variable", "Variable", "Cantidad", dataset);
            applyBarChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(600, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            nulls.put("chartBase64", base64Image);
        } catch (Exception e) {
            nulls.put("chartBase64", null);
        }
        return ResponseEntity.ok(nulls);
    }

    // Utilidad para aplicar la estética a gráficos de barras
    void applyBarChartStyle(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(new Font("Open Sans", Font.BOLD, 18));
            chart.getTitle().setPaint(Color.BLACK);
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("Open Sans", Font.PLAIN, 14));
            chart.getLegend().setItemPaint(Color.BLACK);
        }
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(Color.BLACK);
        plot.getDomainAxis().setLabelFont(new Font("Open Sans", Font.BOLD, 14));
        plot.getDomainAxis().setTickLabelFont(new Font("Open Sans", Font.PLAIN, 12));
        plot.getDomainAxis().setTickLabelPaint(Color.BLACK);
        plot.getRangeAxis().setLabelFont(new Font("Open Sans", Font.BOLD, 14));
        plot.getRangeAxis().setTickLabelFont(new Font("Open Sans", Font.PLAIN, 12));
        plot.getRangeAxis().setTickLabelPaint(Color.BLACK);
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setSeriesPaint(0, new Color(220,53,69));
        renderer.setDrawBarOutline(true);
        renderer.setSeriesOutlinePaint(0, Color.BLACK);
        renderer.setSeriesOutlineStroke(0, new java.awt.BasicStroke(1.5f));
    }

    // Utilidad para aplicar la estética a gráficos de torta
    void applyPieChartStyle(JFreeChart chart, PiePlot plot) {
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(new Font("Open Sans", Font.BOLD, 18));
            chart.getTitle().setPaint(Color.BLACK);
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("Open Sans", Font.PLAIN, 14));
            chart.getLegend().setItemPaint(Color.BLACK);
        }
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(Color.BLACK);
        plot.setLabelFont(new Font("Open Sans", Font.PLAIN, 13));
        plot.setLabelPaint(Color.BLACK);
        plot.setLabelBackgroundPaint(Color.WHITE);
        plot.setLabelOutlinePaint(Color.BLACK);
        plot.setLabelShadowPaint(null);
        // Aplicar borde negro y grosor a cada sección
        for (Object key : plot.getDataset().getKeys()) {
            Comparable cKey = (Comparable) key;
            plot.setSectionOutlinePaint(cKey, Color.BLACK);
            plot.setSectionOutlineStroke(cKey, new java.awt.BasicStroke(1.5f));
            // Fondo blanco para las secciones requeridas
            if ("Facturación Total (en miles)".equals(key) || "Facturación Total".equals(key)) {
                plot.setSectionPaint(cKey, Color.WHITE);
            }
        }
    }

    // Utilidad para aplicar la estética a gráficos de líneas
    void applyLineChartStyle(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(new Font("Open Sans", Font.BOLD, 18));
            chart.getTitle().setPaint(Color.BLACK);
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("Open Sans", Font.PLAIN, 14));
            chart.getLegend().setItemPaint(Color.BLACK);
        }
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(Color.BLACK);
        plot.getDomainAxis().setLabelFont(new Font("Open Sans", Font.BOLD, 14));
        plot.getDomainAxis().setTickLabelFont(new Font("Open Sans", Font.PLAIN, 12));
        plot.getDomainAxis().setTickLabelPaint(Color.BLACK);
        plot.getRangeAxis().setLabelFont(new Font("Open Sans", Font.BOLD, 14));
        plot.getRangeAxis().setTickLabelFont(new Font("Open Sans", Font.PLAIN, 12));
        plot.getRangeAxis().setTickLabelPaint(Color.BLACK);
    }

    // Utilidad para aplicar la estética a gráficos de dispersión
    void applyScatterChartStyle(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(new Font("Open Sans", Font.BOLD, 18));
            chart.getTitle().setPaint(Color.BLACK);
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("Open Sans", Font.PLAIN, 14));
            chart.getLegend().setItemPaint(Color.BLACK);
        }
        chart.getXYPlot().getDomainAxis().setLabelFont(new Font("Open Sans", Font.BOLD, 14));
        chart.getXYPlot().getDomainAxis().setTickLabelFont(new Font("Open Sans", Font.PLAIN, 12));
        chart.getXYPlot().getDomainAxis().setTickLabelPaint(Color.BLACK);
        chart.getXYPlot().getRangeAxis().setLabelFont(new Font("Open Sans", Font.BOLD, 14));
        chart.getXYPlot().getRangeAxis().setTickLabelFont(new Font("Open Sans", Font.PLAIN, 12));
        chart.getXYPlot().getRangeAxis().setTickLabelPaint(Color.BLACK);
    }

    // Utilidad para aplicar la estética a boxplots
    void applyBoxPlotStyle(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(new Font("Open Sans", Font.BOLD, 18));
            chart.getTitle().setPaint(Color.BLACK);
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("Open Sans", Font.PLAIN, 14));
            chart.getLegend().setItemPaint(Color.BLACK);
        }
        chart.getCategoryPlot().getDomainAxis().setLabelFont(new Font("Open Sans", Font.BOLD, 14));
        chart.getCategoryPlot().getDomainAxis().setTickLabelFont(new Font("Open Sans", Font.PLAIN, 12));
        chart.getCategoryPlot().getDomainAxis().setTickLabelPaint(Color.BLACK);
        chart.getCategoryPlot().getRangeAxis().setLabelFont(new Font("Open Sans", Font.BOLD, 14));
        chart.getCategoryPlot().getRangeAxis().setTickLabelFont(new Font("Open Sans", Font.PLAIN, 12));
        chart.getCategoryPlot().getRangeAxis().setTickLabelPaint(Color.BLACK);
        chart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
        chart.getCategoryPlot().setOutlinePaint(Color.BLACK);
    }
}
