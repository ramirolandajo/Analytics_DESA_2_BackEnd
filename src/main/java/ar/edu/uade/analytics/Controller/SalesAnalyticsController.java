package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.Purchase;
import ar.edu.uade.analytics.Service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.awt.Font;
import java.awt.Color;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;

@RestController
@RequestMapping("/analytics/sales")
public class SalesAnalyticsController {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private ar.edu.uade.analytics.Repository.StockChangeLogRepository stockChangeLogRepository;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSalesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "bar") String chartType) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        int totalVentas = 0;
        float facturacionTotal = 0f;
        int productosVendidos = 0;
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                LocalDateTime fecha = purchase.getDate();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    totalVentas++;
                    if (purchase.getCart() != null) {
                        facturacionTotal += purchase.getCart().getFinalPrice() != null ? purchase.getCart().getFinalPrice() : 0f;
                        if (purchase.getCart().getItems() != null) {
                            productosVendidos += purchase.getCart().getItems().stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
                        }
                    }
                }
            }
        }
        Map<String, Object> resumen = new HashMap<>();
        resumen.put("totalVentas", totalVentas);
        resumen.put("facturacionTotal", facturacionTotal);
        resumen.put("productosVendidos", productosVendidos);
        float facturacionTotalEnMiles = Math.round((facturacionTotal / 1000f) * 100f) / 100f;
        resumen.put("facturacionTotalEnMiles", facturacionTotalEnMiles);
        // (Opcional) Formato amigable para mostrar en la tarjeta
        resumen.put("facturacionTotalFormateado", String.format("$%,.2f", facturacionTotal));
        // Generar gráfico en base64
        try {
            BufferedImage image;
            if (chartType.equalsIgnoreCase("pie")) {
                DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
                dataset.setValue("Total Ventas", totalVentas);
                dataset.setValue("Facturación Total (en miles)", facturacionTotalEnMiles);
                dataset.setValue("Productos Vendidos", productosVendidos);
                JFreeChart chart = ChartFactory.createPieChart(
                        "Resumen de Ventas", dataset, true, true, false);
                PiePlot plot = (PiePlot) chart.getPlot();
                applyPieChartStyle(chart, plot);
                plot.setSectionPaint("Total Ventas", new Color(220,53,69));
                plot.setSectionPaint("Facturación Total (en miles)", Color.WHITE);
                plot.setSectionPaint("Productos Vendidos", new Color(230,230,230));
                image = chart.createBufferedImage(600, 400);
            } else {
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                dataset.addValue(totalVentas, "Resumen", "Total Ventas");
                dataset.addValue(facturacionTotalEnMiles, "Resumen", "Facturación Total (en miles)");
                dataset.addValue(productosVendidos, "Resumen", "Productos Vendidos");
                JFreeChart chart = ChartFactory.createBarChart(
                        "Resumen de Ventas", "KPI", "Valor", dataset);
                applyBarChartStyle(chart);
                image = chart.createBufferedImage(600, 400);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            resumen.put("chartBase64", base64Image);
        } catch (Exception e) {
            resumen.put("chartBase64", null);
        }
        return ResponseEntity.ok(resumen);
    }

    @GetMapping("/top-products")
    public ResponseEntity<Map<String, Object>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        Map<Integer, Integer> productSales = new HashMap<>(); // productId -> cantidad vendida
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                LocalDateTime fecha = purchase.getDate();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    if (purchase.getCart() != null && purchase.getCart().getItems() != null) {
                        for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                            Integer productId = item.getProduct().getId();
                            Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                            productSales.put(productId, productSales.getOrDefault(productId, 0) + cantidad);
                        }
                    }
                }
            }
        }
        // Ordenar por cantidad vendida y limitar
        List<Map.Entry<Integer, Integer>> sorted = productSales.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<Integer, Integer> entry : sorted) {
            Map<String, Object> prodInfo = new HashMap<>();
            prodInfo.put("productId", entry.getKey());
            prodInfo.put("cantidadVendida", entry.getValue());
            // Agregar el nombre del producto
            ar.edu.uade.analytics.Entity.Product prod = purchaseService.getProductRepository().findById(entry.getKey()).orElse(null);
            String title = (prod != null && prod.getTitle() != null) ? prod.getTitle() : "ID " + entry.getKey();
            prodInfo.put("title", title);
            result.add(prodInfo);
            dataset.addValue(entry.getValue(), "Productos", title);
        }
        // Generar gráfico de barras horizontal
        String base64Image = null;
        try {
            JFreeChart chart = ChartFactory.createBarChart(
                    "Productos más vendidos", "Producto", "Cantidad Vendida", dataset);
            chart.getCategoryPlot().setOrientation(org.jfree.chart.plot.PlotOrientation.HORIZONTAL);
            applyBarChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(800, 500);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            base64Image = null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", base64Image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-categories")
    public ResponseEntity<Map<String, Object>> getTopCategories(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "bar") String chartType) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        Map<String, Integer> categorySales = new HashMap<>(); // nombreCategoria -> cantidad vendida
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                LocalDateTime fecha = purchase.getDate();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    if (purchase.getCart() != null && purchase.getCart().getItems() != null) {
                        for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                            ar.edu.uade.analytics.Entity.Product product = item.getProduct();
                            if (product != null && product.getCategories() != null) {
                                for (ar.edu.uade.analytics.Entity.Category category : product.getCategories()) {
                                    String catName = category.getName();
                                    Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                                    categorySales.put(catName, categorySales.getOrDefault(catName, 0) + cantidad);
                                }
                            }
                        }
                    }
                }
            }
        }
        // Ordenar por cantidad vendida y limitar
        List<Map.Entry<String, Integer>> sorted = categorySales.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        org.jfree.data.category.DefaultCategoryDataset barDataset = new org.jfree.data.category.DefaultCategoryDataset();
        org.jfree.data.general.DefaultPieDataset<String> pieDataset = new org.jfree.data.general.DefaultPieDataset<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            Map<String, Object> catInfo = new HashMap<>();
            catInfo.put("category", entry.getKey());
            catInfo.put("cantidadVendida", entry.getValue());
            result.add(catInfo);
            barDataset.addValue(entry.getValue(), "Categorías", entry.getKey());
            pieDataset.setValue(entry.getKey(), entry.getValue());
        }
        // Generar gráfico (barras o torta)
        String base64Image = null;
        try {
            JFreeChart chart;
            if (chartType.equalsIgnoreCase("pie")) {
                chart = ChartFactory.createPieChart(
                        "Categorías m��s populares", pieDataset, true, true, false);
                PiePlot plot = (PiePlot) chart.getPlot();
                applyPieChartStyle(chart, plot);
            } else {
                chart = ChartFactory.createBarChart(
                        "Categorías más populares", "Categoría", "Cantidad Vendida", barDataset);
                applyBarChartStyle(chart);
            }
            BufferedImage image = chart.createBufferedImage(800, 500);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            base64Image = null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", base64Image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary/chart")
    public ResponseEntity<byte[]> getSalesSummaryChart(
            @RequestParam(defaultValue = "bar") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        int totalVentas = 0;
        float facturacionTotal = 0f;
        int productosVendidos = 0;
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                LocalDateTime fecha = purchase.getDate();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    totalVentas++;
                    if (purchase.getCart() != null) {
                        facturacionTotal += purchase.getCart().getFinalPrice() != null ? purchase.getCart().getFinalPrice() : 0f;
                        if (purchase.getCart().getItems() != null) {
                            productosVendidos += purchase.getCart().getItems().stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
                        }
                    }
                }
            }
        }
        try {
            byte[] imageBytes;
            if (type.equalsIgnoreCase("pie")) {
                org.jfree.chart.JFreeChart chart = createPieChart(totalVentas, facturacionTotal, productosVendidos);
                java.awt.image.BufferedImage image = chart.createBufferedImage(600, 400);
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(image, "png", baos);
                imageBytes = baos.toByteArray();
            } else {
                org.jfree.chart.JFreeChart chart = createBarChart(totalVentas, facturacionTotal, productosVendidos);
                java.awt.image.BufferedImage image = chart.createBufferedImage(600, 400);
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(image, "png", baos);
                imageBytes = baos.toByteArray();
            }
            return ResponseEntity.ok()
                    .header("Content-Type", "image/png")
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Cambiado a public para que los tests unitarios puedan invocarlos
    public org.jfree.chart.JFreeChart createBarChart(int totalVentas, float facturacionTotal, int productosVendidos) {
        org.jfree.data.category.DefaultCategoryDataset dataset = new org.jfree.data.category.DefaultCategoryDataset();
        dataset.addValue(totalVentas, "Resumen", "Total Ventas");
        dataset.addValue(facturacionTotal, "Resumen", "Facturaci��n Total");
        dataset.addValue(productosVendidos, "Resumen", "Productos Vendidos");
        return org.jfree.chart.ChartFactory.createBarChart(
                "Resumen de Ventas", "KPI", "Valor", dataset);
    }

    public org.jfree.chart.JFreeChart createPieChart(int totalVentas, float facturacionTotal, int productosVendidos) {
        org.jfree.data.general.DefaultPieDataset<String> dataset = new org.jfree.data.general.DefaultPieDataset<>();
        dataset.setValue("Total Ventas", totalVentas);
        dataset.setValue("Facturación Total", facturacionTotal);
        dataset.setValue("Productos Vendidos", productosVendidos);
        return org.jfree.chart.ChartFactory.createPieChart(
                "Resumen de Ventas", dataset, true, true, false);
    }

    @GetMapping("/top-brands")
    public ResponseEntity<Map<String, Object>> getTopBrands(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "bar") String chartType) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        Map<String, Integer> brandSales = new HashMap<>(); // nombreMarca -> cantidad vendida
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                LocalDateTime fecha = purchase.getDate();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    if (purchase.getCart() != null && purchase.getCart().getItems() != null) {
                        for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                            ar.edu.uade.analytics.Entity.Product product = item.getProduct();
                            if (product != null && product.getBrand() != null && product.getBrand().getName() != null) {
                                String brandName = product.getBrand().getName();
                                Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                                brandSales.put(brandName, brandSales.getOrDefault(brandName, 0) + cantidad);
                            }
                        }
                    }
                }
            }
        }
        // Ordenar por cantidad vendida y limitar
        List<Map.Entry<String, Integer>> sorted = brandSales.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        org.jfree.data.category.DefaultCategoryDataset barDataset = new org.jfree.data.category.DefaultCategoryDataset();
        org.jfree.data.general.DefaultPieDataset<String> pieDataset = new org.jfree.data.general.DefaultPieDataset<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            Map<String, Object> brandInfo = new HashMap<>();
            brandInfo.put("brand", entry.getKey());
            brandInfo.put("cantidadVendida", entry.getValue());
            result.add(brandInfo);
            barDataset.addValue(entry.getValue(), "Marcas", entry.getKey());
            pieDataset.setValue(entry.getKey(), entry.getValue());
        }
        // Generar gráfico (barras o torta)
        String base64Image = null;
        try {
            JFreeChart chart;
            if (chartType.equalsIgnoreCase("pie")) {
                chart = ChartFactory.createPieChart(
                        "Marcas más vendidas", pieDataset, true, true, false);
                PiePlot plot = (PiePlot) chart.getPlot();
                applyPieChartStyle(chart, plot);
            } else {
                chart = ChartFactory.createBarChart(
                        "Marcas más vendidas", "Marca", "Cantidad Vendida", barDataset);
                applyBarChartStyle(chart);
            }
            BufferedImage image = chart.createBufferedImage(800, 500);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            base64Image = null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", base64Image);
        return ResponseEntity.ok(response);
    }

    //Ventas diarias agrupadas por fecha 5→ Gráfico de líneas (Line Chart) para mostrar la evolución temporal.
    @GetMapping("/daily-sales")
    public ResponseEntity<Map<String, Object>> getDailySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "line") String chartType) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        Map<String, Integer> dailySales = new HashMap<>(); // fecha (yyyy-MM-dd) -> cantidad ventas
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                LocalDateTime fecha = purchase.getDate();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    String day = fecha.toLocalDate().toString();
                    dailySales.put(day, dailySales.getOrDefault(day, 0) + 1);
                }
            }
        }
        // Ordenar por fecha
        List<String> sortedDates = new ArrayList<>(dailySales.keySet());
        java.util.Collections.sort(sortedDates);
        List<Map<String, Object>> result = new ArrayList<>();
        org.jfree.data.category.DefaultCategoryDataset barDataset = new org.jfree.data.category.DefaultCategoryDataset();
        org.jfree.data.category.DefaultCategoryDataset lineDataset = new org.jfree.data.category.DefaultCategoryDataset();
        for (String date : sortedDates) {
            Integer cantidad = dailySales.get(date);
            Map<String, Object> info = new HashMap<>();
            info.put("date", date);
            info.put("cantidadVentas", cantidad);
            result.add(info);
            barDataset.addValue(cantidad, "Ventas", date);
            lineDataset.addValue(cantidad, "Ventas", date);
        }
        // Generar gráfico (línea o barras)
        String base64Image = null;
        try {
            JFreeChart chart;
            if (chartType.equalsIgnoreCase("bar")) {
                chart = ChartFactory.createBarChart(
                        "Ventas diarias", "Fecha", "Cantidad de Ventas", barDataset);
                applyBarChartStyle(chart);
            } else {
                chart = ChartFactory.createLineChart(
                        "Ventas diarias", "Fecha", "Cantidad de Ventas", lineDataset);
                applyLineChartStyle(chart);
            }
            // Rotar etiquetas del eje X para mejor visualización
            if (chart.getCategoryPlot() != null) {
                org.jfree.chart.axis.CategoryAxis domainAxis = chart.getCategoryPlot().getDomainAxis();
                domainAxis.setCategoryLabelPositions(
                    org.jfree.chart.axis.CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 4)
                );
            }
            BufferedImage image = chart.createBufferedImage(900, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            base64Image = null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", base64Image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stock-history")
    public ResponseEntity<Map<String, Object>> getStockHistoryByProduct(
            @RequestParam("productId") Integer productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "line") String chartType) {
        // Obtener historial de cambios de stock para el producto
        List<ar.edu.uade.analytics.Entity.StockChangeLog> logs =
                stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(productId);
        List<Map<String, Object>> result = new ArrayList<>();
        org.jfree.data.category.DefaultCategoryDataset lineDataset = new org.jfree.data.category.DefaultCategoryDataset();
        org.jfree.data.category.DefaultCategoryDataset barDataset = new org.jfree.data.category.DefaultCategoryDataset();
        for (ar.edu.uade.analytics.Entity.StockChangeLog log : logs) {
            LocalDateTime fecha = log.getChangedAt();
            if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                Map<String, Object> info = new HashMap<>();
                info.put("date", fecha.toLocalDate().toString()); // Solo la fecha
                info.put("oldStock", log.getOldStock());
                info.put("newStock", log.getNewStock());
                info.put("quantityChanged", log.getQuantityChanged());
                info.put("reason", log.getReason());
                result.add(info);
                String fechaLabel = fecha.toLocalDate().toString();
                lineDataset.addValue(log.getNewStock(), "Stock", fechaLabel);
                barDataset.addValue(log.getNewStock(), "Stock", fechaLabel);
            }
        }
        // Generar gráfico (línea o barras)
        String base64Image = null;
        try {
            JFreeChart chart;
            if (chartType.equalsIgnoreCase("bar")) {
                chart = ChartFactory.createBarChart(
                        "Histórico de Stock", "Fecha", "Stock", barDataset);
                applyBarChartStyle(chart);
            } else {
                chart = ChartFactory.createLineChart(
                        "Histórico de Stock", "Fecha", "Stock", lineDataset);
                applyLineChartStyle(chart);
            }
            BufferedImage image = chart.createBufferedImage(900, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            base64Image = null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", base64Image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<Map<String, Object>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(defaultValue = "10") int limit) {
        List<ar.edu.uade.analytics.Entity.Product> products = purchaseService.getProductRepository().findAll();
        // Filtrar productos con stock menor o igual al threshold
        List<ar.edu.uade.analytics.Entity.Product> lowStock = products.stream()
                .filter(p -> p.getStock() != null && p.getStock() <= threshold)
                .sorted(java.util.Comparator.comparingInt(p -> p.getStock() != null ? p.getStock() : Integer.MAX_VALUE))
                .limit(limit)
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (ar.edu.uade.analytics.Entity.Product p : lowStock) {
            Map<String, Object> info = new HashMap<>();
            info.put("productId", p.getId());
            info.put("title", p.getTitle() != null ? p.getTitle() : "ID " + p.getId());
            info.put("stock", p.getStock());
            result.add(info);
            String title = p.getTitle() != null ? p.getTitle() : "ID " + p.getId();
            dataset.addValue(p.getStock(), "Stock", title);
        }
        String base64Image = null;
        try {
            JFreeChart chart = ChartFactory.createBarChart(
                    "Productos con stock crítico", "Producto", "Stock", dataset);
            applyBarChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(800, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            base64Image = null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", base64Image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stock-history-by-product-code")
    public ResponseEntity<Map<String, Object>> getStockHistoryByProductCode(
            @RequestParam("productCode") Integer productCode,
            @RequestParam(required = false, defaultValue = "false") boolean showProfit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // Buscar el producto por productCode
        ar.edu.uade.analytics.Entity.Product product = purchaseService.getProductRepository().findByProductCode(productCode);
        if (product == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Producto no encontrado"));
        }
        // Obtener historial de cambios de stock para el producto
        List<ar.edu.uade.analytics.Entity.StockChangeLog> logs = stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(product.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        org.jfree.data.category.DefaultCategoryDataset lineDataset = new org.jfree.data.category.DefaultCategoryDataset();
        org.jfree.data.category.DefaultCategoryDataset profitDataset = new org.jfree.data.category.DefaultCategoryDataset();
        float profitAccum = 0f;
        for (ar.edu.uade.analytics.Entity.StockChangeLog log : logs) {
            LocalDateTime fecha = log.getChangedAt();
            if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                Map<String, Object> info = new HashMap<>();
                info.put("date", fecha.toLocalDate().toString()); // Solo la fecha
                info.put("oldStock", log.getOldStock());
                info.put("newStock", log.getNewStock());
                info.put("quantityChanged", log.getQuantityChanged());
                info.put("reason", log.getReason());
                // Calcular ganancia si corresponde y el motivo es venta
                float profit = 0f;
                if (showProfit && "Venta".equalsIgnoreCase(log.getReason())) {
                    Float price = product.getPrice() != null ? product.getPrice() : 0f;
                    profit = price * log.getQuantityChanged();
                    profitAccum += profit;
                    info.put("profit", profit);
                    info.put("profitAccum", profitAccum);
                }
                result.add(info);
                lineDataset.addValue(log.getNewStock(), "Stock", fecha.toLocalDate().toString());
                if (showProfit) {
                    profitDataset.addValue(profitAccum, "Ganancia", fecha.toLocalDate().toString());
                }
            }
        }
        // Generar gráfico (l��nea de stock y opcionalmente línea de ganancia)
        String base64Image = null;
        try {
            JFreeChart chart = ChartFactory.createLineChart(
                    "Histórico de Stock y Ganancia", "Fecha", "Valor", lineDataset);
            org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
            if (showProfit) {
                // Añadir segundo dataset y eje para la ganancia
                plot.setDataset(1, profitDataset);
                org.jfree.chart.axis.NumberAxis profitAxis = new org.jfree.chart.axis.NumberAxis("Ganancia");
                plot.setRangeAxis(1, profitAxis);
                plot.mapDatasetToRangeAxis(1, 1);
                // Renderer para la serie de ganancia
                org.jfree.chart.renderer.category.LineAndShapeRenderer profitRenderer = new org.jfree.chart.renderer.category.LineAndShapeRenderer();
                profitRenderer.setSeriesPaint(0, new java.awt.Color(34, 139, 34));
                profitRenderer.setSeriesStroke(0, new java.awt.BasicStroke(2.0f));
                plot.setRenderer(1, profitRenderer);
            }
            applyLineChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(900, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            base64Image = null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", base64Image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products-dashboard")
    public ResponseEntity<Map<String, Object>> getProductsDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer brandId) {
        List<ar.edu.uade.analytics.Entity.Product> products = purchaseService.getProductRepository().findAll();
        // Filtrar por categoría y marca si corresponde
        if (categoryId != null) {
            products = products.stream().filter(p -> p.getCategories() != null && p.getCategories().stream().anyMatch(c -> c.getId().equals(categoryId))).toList();
        }
        if (brandId != null) {
            products = products.stream().filter(p -> p.getBrand() != null && p.getBrand().getId().equals(brandId)).toList();
        }
        // Estadísticas generales
        int totalProductos = products.size();
        int stockTotal = products.stream().mapToInt(p -> p.getStock() != null ? p.getStock() : 0).sum();
        List<Map<String, Object>> productosCriticos = products.stream()
            .filter(p -> p.getStock() != null && p.getStock() <= 10)
            .map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("productId", p.getId());
                map.put("title", p.getTitle());
                map.put("stock", p.getStock());
                return map;
            })
            .toList();
        // Gráfica de stock actual
        DefaultCategoryDataset stockDataset = new DefaultCategoryDataset();
        for (ar.edu.uade.analytics.Entity.Product p : products) {
            String title = p.getTitle() != null ? p.getTitle() : "ID " + p.getId();
            stockDataset.addValue(p.getStock() != null ? p.getStock() : 0, "Stock", title);
        }
        String stockChartBase64 = null;
        try {
            JFreeChart chart = ChartFactory.createBarChart(
                    "Stock actual de productos", "Producto", "Stock", stockDataset);
            applyBarChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(900, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            stockChartBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            stockChartBase64 = null;
        }
        // Gráfica de evolución de stock de los productos más vendidos
        List<Purchase> purchases = purchaseService.getAllPurchases();
        if (startDate != null || endDate != null) {
            purchases = purchases.stream().filter(p -> {
                LocalDateTime fecha = p.getDate();
                return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
            }).toList();
        }
        // Reuse helper to compute product sales applying same filters
        Map<Integer, Integer> productSales = computeProductSalesFromPurchases(purchases, startDate, endDate, categoryId, brandId);
        // Tomar los 5 productos más vendidos
        List<Integer> topProductIds = productSales.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
        String evolutionChartBase64 = buildEvolutionChartBase64(topProductIds, startDate, endDate);
        Map<String, Object> response = new HashMap<>();
        response.put("totalProductos", totalProductos);
        response.put("stockTotal", stockTotal);
        response.put("productosCriticos", productosCriticos);
        response.put("stockChartBase64", stockChartBase64);
        response.put("evolutionChartBase64", evolutionChartBase64);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-customers")
    public ResponseEntity<Map<String, Object>> getTopCustomers(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        // Filtrar por fechas si corresponde
        if (startDate != null || endDate != null) {
            purchases = purchases.stream().filter(p -> {
                LocalDateTime fecha = p.getDate();
                return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
            }).toList();
        }
        // Agrupar por usuario
        Map<Integer, Map<String, Object>> userStats = new HashMap<>();
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED && purchase.getUser() != null) {
                ar.edu.uade.analytics.Entity.User user = purchase.getUser();
                Integer userId = user.getId();
                String name = user.getName();
                String email = user.getEmail();
                float gasto = purchase.getCart() != null && purchase.getCart().getFinalPrice() != null ? purchase.getCart().getFinalPrice() : 0f;
                Map<String, Object> stats = userStats.getOrDefault(userId, new HashMap<>());
                stats.put("userId", userId);
                stats.put("name", name);
                stats.put("email", email);
                stats.put("gastoTotal", ((Number) stats.getOrDefault("gastoTotal", 0f)).floatValue() + gasto);
                stats.put("cantidadCompras", ((Number) stats.getOrDefault("cantidadCompras", 0)).intValue() + 1);
                userStats.put(userId, stats);
            }
        }
        // Ordenar por gasto total y limitar
        List<Map<String, Object>> sorted = userStats.values().stream()
                .sorted((a, b) -> Float.compare(((Number) b.get("gastoTotal")).floatValue(), ((Number) a.get("gastoTotal")).floatValue()))
                .limit(limit)
                .toList();
        // Generar gráfico de barras horizontal
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map<String, Object> stats : sorted) {
            String label = stats.get("name") + " (" + stats.get("email") + ")";
            dataset.addValue(((Number) stats.get("gastoTotal")).floatValue(), "Gasto Total", label);
            dataset.addValue(((Number) stats.get("cantidadCompras")).intValue(), "Cantidad Compras", label);
        }
        String base64Image = null;
        try {
            JFreeChart chart = ChartFactory.createBarChart(
                    "Clientes con mayor gasto y compras", "Cliente", "Valor", dataset);
            chart.getCategoryPlot().setOrientation(org.jfree.chart.plot.PlotOrientation.HORIZONTAL);
            applyBarChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(900, 500);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            base64Image = null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", sorted);
        response.put("chartBase64", base64Image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/histogram")
    public ResponseEntity<Map<String, Object>> getSalesHistogram(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        if (startDate != null || endDate != null) {
            purchases = purchases.stream().filter(p -> {
                LocalDateTime fecha = p.getDate();
                return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
            }).toList();
        }
        // ETL: Agrupar compras por usuario y contar frecuencia
        Map<Integer, Integer> userPurchaseCount = new HashMap<>();
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED && purchase.getUser() != null) {
                Integer userId = purchase.getUser().getId();
                userPurchaseCount.put(userId, userPurchaseCount.getOrDefault(userId, 0) + 1);
            }
        }
        // Histograma: frecuencia de compras por usuario
        Map<String, Integer> histogram = new HashMap<>();
        for (Integer count : userPurchaseCount.values()) {
            String rango = count <= 2 ? "1-2" : count <= 5 ? "3-5" : "6+";
            histogram.put(rango, histogram.getOrDefault(rango, 0) + 1);
        }
        // ML: Predicción de ventas por producto (regresión lineal simple)
        Map<Integer, List<Integer>> productSalesByDay = new HashMap<>(); // productId -> ventas por día
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED && purchase.getCart() != null && purchase.getCart().getItems() != null) {
                String day = purchase.getDate().toLocalDate().toString();
                for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                    Integer productId = item.getProduct().getId();
                    Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                    productSalesByDay.computeIfAbsent(productId, k -> new ArrayList<>()).add(cantidad);
                }
            }
        }
        // Regresión lineal simple: calcular tendencia de ventas por producto
        Map<Integer, Double> productTrends = computeProductTrends(productSalesByDay);
        // Generar gráfico de histograma
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Integer> entry : histogram.entrySet()) {
            dataset.addValue(entry.getValue(), "Usuarios", entry.getKey());
        }
        String base64Image = null;
        try {
            JFreeChart chart = ChartFactory.createBarChart(
                    "Histograma de frecuencia de compras", "Rango de compras", "Cantidad de usuarios", dataset);
            applyBarChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(700, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            base64Image = null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("histogram", histogram);
        response.put("chartBase64", base64Image);
        response.put("productTrends", productTrends);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/correlation")
    public ResponseEntity<Map<String, Object>> getSalesCorrelation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        if (startDate != null || endDate != null) {
            purchases = purchases.stream().filter(p -> {
                LocalDateTime fecha = p.getDate();
                return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
            }).toList();
        }
        // ETL: Relacionar cantidad de compras vs dinero gastado por usuario
        Map<Integer, Integer> userPurchaseCount = new HashMap<>();
        Map<Integer, Float> userSpent = new HashMap<>();
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED && purchase.getUser() != null) {
                Integer userId = purchase.getUser().getId();
                userPurchaseCount.put(userId, userPurchaseCount.getOrDefault(userId, 0) + 1);
                float gasto = purchase.getCart() != null && purchase.getCart().getFinalPrice() != null ? purchase.getCart().getFinalPrice() : 0f;
                userSpent.put(userId, userSpent.getOrDefault(userId, 0f) + gasto);
            }
        }
        // Preparar datos para scatter plot
        List<Float> xList = new ArrayList<>(); // cantidad de compras
        List<Float> yList = new ArrayList<>(); // dinero gastado
        for (Integer userId : userPurchaseCount.keySet()) {
            xList.add(userPurchaseCount.get(userId).floatValue());
            yList.add(userSpent.get(userId));
        }
        // ML: Calcular regresión lineal (y = a + bx)
        Map<String, Double> regression = computeRegressionFromXY(xList, yList);
        // Generar gráfico de dispersión con línea de regresión y línea punteada de conexión
        org.jfree.data.xy.XYSeries series = new org.jfree.data.xy.XYSeries("Usuarios");
        org.jfree.data.xy.XYSeries connection = new org.jfree.data.xy.XYSeries("Conexión");
        for (int i = 0; i < xList.size(); i++) {
            series.add(xList.get(i), yList.get(i));
            connection.add(xList.get(i), yList.get(i));
        }
        org.jfree.data.xy.XYSeries regressionLine = new org.jfree.data.xy.XYSeries("Regresión");
        float minX = xList.stream().min(Float::compare).orElse(0f);
        float maxX = xList.stream().max(Float::compare).orElse(0f);
        regressionLine.add(minX, (float) (regression.get("a") + regression.get("b") * minX));
        regressionLine.add(maxX, (float) (regression.get("a") + regression.get("b") * maxX));
        org.jfree.data.xy.XYSeriesCollection dataset = new org.jfree.data.xy.XYSeriesCollection();
        dataset.addSeries(series);      // 0: puntos
        dataset.addSeries(connection);  // 1: línea punteada
        dataset.addSeries(regressionLine);  // 2: regresión
        String base64Image = null;
        try {
            JFreeChart chart = ChartFactory.createScatterPlot(
                    "Relación compras vs gasto (usuarios)", "Cantidad de compras", "Dinero gastado", dataset);
            // Usar renderer para mostrar puntos, línea punteada y regresión
            org.jfree.chart.plot.XYPlot plot = chart.getXYPlot();
            org.jfree.chart.renderer.xy.XYLineAndShapeRenderer renderer = new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer();
            // Serie 0: solo puntos (azul)
            renderer.setSeriesLinesVisible(0, false);
            renderer.setSeriesShapesVisible(0, true);
            renderer.setSeriesPaint(0, new Color(0, 102, 204));
            // Serie 1: solo línea punteada (gris oscuro)
            renderer.setSeriesLinesVisible(1, true);
            renderer.setSeriesShapesVisible(1, false);
            renderer.setSeriesPaint(1, new Color(80, 80, 80));
            renderer.setSeriesStroke(1, new java.awt.BasicStroke(2.0f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_BEVEL, 0, new float[]{6.0f, 6.0f}, 0));
            // Serie 2: línea de regresión (roja, sólida)
            renderer.setSeriesLinesVisible(2, true);
            renderer.setSeriesShapesVisible(2, false);
            renderer.setSeriesPaint(2, new Color(220,53,69));
            renderer.setSeriesStroke(2, new java.awt.BasicStroke(2.5f));
            plot.setRenderer(renderer);
            applyScatterChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(700, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            base64Image = null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("chartBase64", base64Image);
        response.put("regression", regression);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category-growth")
    public ResponseEntity<Map<String, Object>> getCategoryGrowth(
            @RequestParam Integer categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        if (startDate != null || endDate != null) {
            purchases = purchases.stream().filter(p -> {
                LocalDateTime fecha = p.getDate();
                return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
            }).toList();
        }
        // Agrupar ventas por fecha solo para la categoría elegida (por ID)
        Map<String, Integer> dateSales = new HashMap<>(); // fecha -> cantidad
        String categoryName = null;
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED && purchase.getCart() != null && purchase.getCart().getItems() != null) {
                String fecha = purchase.getDate().toLocalDate().toString();
                for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                    ar.edu.uade.analytics.Entity.Product product = item.getProduct();
                    if (product != null && product.getCategories() != null) {
                        boolean match = product.getCategories().stream().anyMatch(cat -> cat.getId().equals(categoryId));
                        if (match) {
                            // Obtener el nombre de la categoría para el gráfico (solo la primera vez)
                            if (categoryName == null) {
                                categoryName = product.getCategories().stream().filter(cat -> cat.getId().equals(categoryId)).map(cat -> cat.getName()).findFirst().orElse("Categoría " + categoryId);
                            }
                            Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                            dateSales.put(fecha, dateSales.getOrDefault(fecha, 0) + cantidad);
                        }
                    }
                }
            }
        }
        if (categoryName == null) {
            categoryName = "Categoría " + categoryId;
        }
        // Generar gráfico de líneas para la categoría elegida
        org.jfree.data.category.DefaultCategoryDataset dataset = new org.jfree.data.category.DefaultCategoryDataset();
        List<String> sortedDates = new ArrayList<>(dateSales.keySet());
        java.util.Collections.sort(sortedDates);
        for (String fecha : sortedDates) {
            dataset.addValue(dateSales.get(fecha), categoryName, fecha);
        }
        String base64Image = null;
        try {
            JFreeChart chart = ChartFactory.createLineChart(
                    "Crecimiento histórico de '" + categoryName + "'", "Fecha", "Cantidad vendida", dataset);
            CategoryPlot plot = chart.getCategoryPlot();
            if (plot.getRenderer() instanceof org.jfree.chart.renderer.category.LineAndShapeRenderer renderer) {
                renderer.setSeriesStroke(0, new java.awt.BasicStroke(3.0f));
            }
            plot.getDomainAxis().setCategoryLabelPositions(
                org.jfree.chart.axis.CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 4)
            );
            plot.setRangeGridlinePaint(new Color(200, 200, 200, 120));
            plot.setDomainGridlinePaint(new Color(200, 200, 200, 120));
            java.awt.Stroke dashed = new java.awt.BasicStroke(
                1.0f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_BEVEL, 0,
                new float[]{3.0f, 3.0f}, 0);
            plot.setRangeGridlineStroke(dashed);
            plot.setDomainGridlineStroke(dashed);
            plot.setRangeGridlinePaint(new Color(255,255,255,180));
            plot.setDomainGridlinePaint(new Color(255,255,255,180));
            applyLineChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(900, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            base64Image = null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("categoryGrowth", dateSales);
        response.put("chartBase64", base64Image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/product-events-timeline")
    public ResponseEntity<Map<String, Object>> getProductEventsTimeline(
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "5") int topN) {
        // Limitar topN entre 1 y 10
        if (topN < 1) topN = 1;
        if (topN > 10) topN = 10;
        List<ar.edu.uade.analytics.Entity.StockChangeLog> logs;
        if (productId != null) {
            logs = stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(productId);
            // Filtrar por fechas si corresponde
            if (startDate != null || endDate != null) {
                logs = logs.stream().filter(log -> {
                    LocalDateTime fecha = log.getChangedAt();
                    return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
                }).toList();
            }
        } else {
            // 1. Filtrar todos los logs por fecha primero
            logs = stockChangeLogRepository.findAll();
            if (startDate != null || endDate != null) {
                logs = logs.stream().filter(log -> {
                    LocalDateTime fecha = log.getChangedAt();
                    return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
                }).toList();
            }
            // 2. Si no hay logs en el rango, devolver vacío
            if (logs.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("events", new ArrayList<>());
                response.put("chartBase64", null);
                return ResponseEntity.ok(response);
            }
            // 3. Contar eventos por producto SOLO en el rango
            Map<Integer, Integer> productEventCount = new HashMap<>();
            for (ar.edu.uade.analytics.Entity.StockChangeLog log : logs) {
                Integer pid = log.getProduct().getId();
                productEventCount.put(pid, productEventCount.getOrDefault(pid, 0) + 1);
            }
            // 4. Obtener los topN productos con más eventos en el rango
            List<Integer> topProductIds = productEventCount.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(topN)
                    .map(Map.Entry::getKey)
                    .toList();
            // 5. Filtrar logs solo para esos productos
            logs = logs.stream()
                    .filter(log -> topProductIds.contains(log.getProduct().getId()))
                    .toList();
        }
        // Delegar al helper que arma eventos, dataset y gráfico a partir de los logs filtrados
        Map<String, Object> response = buildTimelineFromLogs(logs);
        return ResponseEntity.ok(response);
    }

    // Package-private helper: arma la respuesta (events + chartBase64) a partir de logs ya filtrados por fecha/producto
    public Map<String, Object> buildTimelineFromLogs(List<ar.edu.uade.analytics.Entity.StockChangeLog> logs) {
        List<Map<String, Object>> events = new ArrayList<>();
        org.jfree.data.category.DefaultCategoryDataset dataset = new org.jfree.data.category.DefaultCategoryDataset();
        // Determinar rango de fechas
        LocalDateTime minDate = null;
        LocalDateTime maxDate = null;
        for (ar.edu.uade.analytics.Entity.StockChangeLog log : logs) {
            LocalDateTime fecha = log.getChangedAt();
            if (minDate == null || fecha.isBefore(minDate)) minDate = fecha;
            if (maxDate == null || fecha.isAfter(maxDate)) maxDate = fecha;
        }
        if (minDate == null) minDate = LocalDateTime.now();
        if (maxDate == null) maxDate = LocalDateTime.now();
        if (minDate.equals(maxDate)) {
            minDate = minDate.minusDays(15);
            maxDate = maxDate.plusDays(15);
        } else if (java.time.Duration.between(minDate, maxDate).toDays() < 10) {
            minDate = minDate.minusDays(5);
            maxDate = maxDate.plusDays(5);
        }
        // Mapear logs por producto y fecha
        Map<String, Map<String, Integer>> productDateStock = new HashMap<>();
        for (ar.edu.uade.analytics.Entity.StockChangeLog log : logs) {
            LocalDateTime fecha = log.getChangedAt();
            String productTitle = log.getProduct() != null && log.getProduct().getTitle() != null ? log.getProduct().getTitle() : ("Producto " + (log.getProduct() != null ? log.getProduct().getId() : "-"));
            String fechaStr = fecha.toLocalDate().toString();
            productDateStock.computeIfAbsent(productTitle, k -> new HashMap<>()).put(fechaStr, log.getNewStock());
            Map<String, Object> event = new HashMap<>();
            event.put("date", fecha.toString());
            event.put("type", "StockChange");
            event.put("productId", log.getProduct() != null ? log.getProduct().getId() : null);
            event.put("productTitle", log.getProduct() != null ? log.getProduct().getTitle() : null);
            event.put("oldStock", log.getOldStock());
            event.put("newStock", log.getNewStock());
            event.put("quantityChanged", log.getQuantityChanged());
            event.put("reason", log.getReason());
            events.add(event);
        }
        // Fechas para eje X
        List<String> dateLabels = new ArrayList<>();
        LocalDateTime cursor = minDate;
        while (!cursor.isAfter(maxDate)) {
            dateLabels.add(cursor.toLocalDate().toString());
            cursor = cursor.plusDays(1);
        }
        for (String productTitle : productDateStock.keySet()) {
            Map<String, Integer> dateStock = productDateStock.get(productTitle);
            for (String dateLabel : dateLabels) {
                Integer stock = dateStock.get(dateLabel);
                if (stock != null) {
                    dataset.addValue(stock, productTitle, dateLabel);
                }
            }
        }
        if (events.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("events", events);
            response.put("chartBase64", null);
            return response;
        }
        String base64Image = null;
        try {
            JFreeChart chart = ChartFactory.createLineChart(
                    "Timeline de eventos de productos (cambios de stock)", "Fecha", "Stock", dataset);
            applyLineChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(1000, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            base64Image = null;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("events", events);
        response.put("chartBase64", base64Image);
        return response;
    }

    // Package-private helper to build evolution chart base64 for given product IDs and date range
    public String buildEvolutionChartBase64(List<Integer> topProductIds, LocalDateTime startDate, LocalDateTime endDate) {
        org.jfree.data.category.DefaultCategoryDataset evolutionDataset = new org.jfree.data.category.DefaultCategoryDataset();
        for (Integer prodId : topProductIds) {
            List<ar.edu.uade.analytics.Entity.StockChangeLog> logs = stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(prodId);
            for (ar.edu.uade.analytics.Entity.StockChangeLog log : logs) {
                LocalDateTime fecha = log.getChangedAt();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    String seriesName = "Producto " + prodId;
                    evolutionDataset.addValue(log.getNewStock(), seriesName, fecha.toString());
                }
            }
        }
        String evolutionChartBase64 = null;
        try {
            JFreeChart chart = ChartFactory.createLineChart(
                    "Evolución de stock de productos más vendidos", "Fecha", "Stock", evolutionDataset);
            applyLineChartStyle(chart);
            BufferedImage image = chart.createBufferedImage(900, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            evolutionChartBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            evolutionChartBase64 = null;
        }
        return evolutionChartBase64;
    }

    // Utilidad para aplicar la estética a gráficos de barras
    private void applyBarChartStyle(JFreeChart chart) {
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
    private void applyPieChartStyle(JFreeChart chart, PiePlot<?> plot) {
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
        for (Object key : plot.getDataset().getKeys()) {
            Comparable<?> cKey = (Comparable<?>) key;
            plot.setSectionOutlinePaint(cKey, Color.BLACK);
            plot.setSectionOutlineStroke(cKey, new java.awt.BasicStroke(1.5f));
            if ("Facturación Total (en miles)".equals(key) || "Facturación Total".equals(key)) {
                plot.setSectionPaint(cKey, Color.WHITE);
            }
        }
    }

    // Utilidad para aplicar la estética a gráficos de líneas
    private void applyLineChartStyle(JFreeChart chart) {
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
        if (plot.getRenderer() instanceof org.jfree.chart.renderer.category.LineAndShapeRenderer renderer) {
            renderer.setSeriesStroke(0, new java.awt.BasicStroke(3.0f));
        }
    }

    // Utilidad para aplicar la estética a gráficos de dispersión
    private void applyScatterChartStyle(JFreeChart chart) {
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
    private void applyBoxPlotStyle(JFreeChart chart) {
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

    // Package-private helper: calcular tendencias (pendiente) a partir de ventas por días por producto
    Map<Integer, Double> computeProductTrends(Map<Integer, List<Integer>> productSalesByDay) {
        Map<Integer, Double> productTrends = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : productSalesByDay.entrySet()) {
            List<Integer> ventas = entry.getValue();
            int n = ventas.size();
            if (n < 2) {
                productTrends.put(entry.getKey(), 0.0);
                continue;
            }
            double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
            for (int i = 0; i < n; i++) {
                sumX += i;
                sumY += ventas.get(i);
                sumXY += i * ventas.get(i);
                sumXX += i * i;
            }
            double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX + 0.0001);
            productTrends.put(entry.getKey(), slope);
        }
        return productTrends;
    }

    // Package-private helper: construir histograma a partir de map userId->purchaseCount
    public Map<String, Integer> computeHistogramFromUserCounts(Map<Integer, Integer> userPurchaseCount) {
        Map<String, Integer> histogram = new HashMap<>();
        for (Integer count : userPurchaseCount.values()) {
            String rango = count <= 2 ? "1-2" : count <= 5 ? "3-5" : "6+";
            histogram.put(rango, histogram.getOrDefault(rango, 0) + 1);
        }
        return histogram;
    }

    // Package-private helper: calcular regresi��n lineal (a, b) dados vectores x,y
    public Map<String, Double> computeRegressionFromXY(List<Float> xList, List<Float> yList) {
        int n = xList.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            sumX += xList.get(i);
            sumY += yList.get(i);
            sumXY += xList.get(i) * yList.get(i);
            sumXX += xList.get(i) * xList.get(i);
        }
        double b = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX + 0.0001);
        double a = (sumY - b * sumX) / (n + 0.0001);
        return Map.of("a", a, "b", b);
    }

    // Package-private helper: calcular productSales map desde una lista de purchases (aplica filtros start/end, categoryId, brandId opcionales)
    public Map<Integer, Integer> computeProductSalesFromPurchases(List<Purchase> purchases, LocalDateTime startDate, LocalDateTime endDate, Integer categoryId, Integer brandId) {
        Map<Integer, Integer> productSales = new HashMap<>();
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED && purchase.getCart() != null && purchase.getCart().getItems() != null) {
                LocalDateTime fecha = purchase.getDate();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                        ar.edu.uade.analytics.Entity.Product prod = item.getProduct();
                        if (prod == null) continue;
                        if (categoryId != null && (prod.getCategories() == null || prod.getCategories().stream().noneMatch(c -> c.getId().equals(categoryId)))) continue;
                        if (brandId != null && (prod.getBrand() == null || !prod.getBrand().getId().equals(brandId))) continue;
                        Integer productId = prod.getId();
                        Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                        productSales.put(productId, productSales.getOrDefault(productId, 0) + cantidad);
                    }
                }
            }
        }
        return productSales;
    }

    // Package-private helper: compute sales KPIs from purchases within optional date range
    public Map<String, Object> computeSalesKPIs(List<Purchase> purchases, LocalDateTime startDate, LocalDateTime endDate) {
        int totalVentas = 0;
        float facturacionTotal = 0f;
        int productosVendidos = 0;
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                LocalDateTime fecha = purchase.getDate();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    totalVentas++;
                    if (purchase.getCart() != null) {
                        facturacionTotal += purchase.getCart().getFinalPrice() != null ? purchase.getCart().getFinalPrice() : 0f;
                        if (purchase.getCart().getItems() != null) {
                            productosVendidos += purchase.getCart().getItems().stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
                        }
                    }
                }
            }
        }
        float facturacionTotalEnMiles = Math.round((facturacionTotal / 1000f) * 100f) / 100f;
        Map<String, Object> kpi = new HashMap<>();
        kpi.put("totalVentas", totalVentas);
        kpi.put("facturacionTotal", facturacionTotal);
        kpi.put("productosVendidos", productosVendidos);
        kpi.put("facturacionTotalEnMiles", facturacionTotalEnMiles);
        return kpi;
    }

    // Package-private helper: compute category sales map from purchases within optional date range
    public Map<String, Integer> computeCategorySales(List<Purchase> purchases, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Integer> categorySales = new HashMap<>();
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                LocalDateTime fecha = purchase.getDate();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    if (purchase.getCart() != null && purchase.getCart().getItems() != null) {
                        for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                            ar.edu.uade.analytics.Entity.Product product = item.getProduct();
                            if (product != null && product.getCategories() != null) {
                                for (ar.edu.uade.analytics.Entity.Category category : product.getCategories()) {
                                    String catName = category.getName();
                                    Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                                    categorySales.put(catName, categorySales.getOrDefault(catName, 0) + cantidad);
                                }
                            }
                        }
                    }
                }
            }
        }
        return categorySales;
    }

    // Package-private helper: compute brand sales map from purchases within optional date range
    public Map<String, Integer> computeBrandSales(List<Purchase> purchases, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Integer> brandSales = new HashMap<>();
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                LocalDateTime fecha = purchase.getDate();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    if (purchase.getCart() != null && purchase.getCart().getItems() != null) {
                        for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                            ar.edu.uade.analytics.Entity.Product product = item.getProduct();
                            if (product != null && product.getBrand() != null && product.getBrand().getName() != null) {
                                String brandName = product.getBrand().getName();
                                Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                                brandSales.put(brandName, brandSales.getOrDefault(brandName, 0) + cantidad);
                            }
                        }
                    }
                }
            }
        }
        return brandSales;
    }

    // Package-private helper: compute daily sales map (date -> count) from purchases within optional date range
    public Map<String, Integer> computeDailySalesMap(List<Purchase> purchases, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Integer> dailySales = new HashMap<>();
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                LocalDateTime fecha = purchase.getDate();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    String day = fecha.toLocalDate().toString();
                    dailySales.put(day, dailySales.getOrDefault(day, 0) + 1);
                }
            }
        }
        return dailySales;
    }

    // Package-private helper: compute stock history data (list of maps) from logs and product + profit flag
    public List<Map<String, Object>> computeStockHistoryData(ar.edu.uade.analytics.Entity.Product product, List<ar.edu.uade.analytics.Entity.StockChangeLog> logs, boolean showProfit, LocalDateTime startDate, LocalDateTime endDate) {
        List<Map<String, Object>> result = new ArrayList<>();
        float profitAccum = 0f;
        for (ar.edu.uade.analytics.Entity.StockChangeLog log : logs) {
            LocalDateTime fecha = log.getChangedAt();
            if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                Map<String, Object> info = new HashMap<>();
                info.put("date", fecha.toLocalDate().toString());
                info.put("oldStock", log.getOldStock());
                info.put("newStock", log.getNewStock());
                info.put("quantityChanged", log.getQuantityChanged());
                info.put("reason", log.getReason());
                if (showProfit && "Venta".equalsIgnoreCase(log.getReason())) {
                    Float price = product.getPrice() != null ? product.getPrice() : 0f;
                    float profit = price * log.getQuantityChanged();
                    profitAccum += profit;
                    info.put("profit", profit);
                    info.put("profitAccum", profitAccum);
                }
                result.add(info);
            }
        }
        return result;
    }
}
