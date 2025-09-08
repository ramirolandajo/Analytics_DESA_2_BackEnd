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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, Object> resumen = new HashMap<>();
        resumen.put("totalVentas", totalVentas);
        resumen.put("facturacionTotal", facturacionTotal);
        resumen.put("productosVendidos", productosVendidos);
        float facturacionTotalEnMiles = Math.round((facturacionTotal / 1000f) * 100f) / 100f;
        resumen.put("facturacionTotalEnMiles", facturacionTotalEnMiles);
        // (Opcional) Formato amigable para mostrar en la tarjeta
        resumen.put("facturacionTotalFormateado", String.format("$%,.2f", facturacionTotal));
        // No graphs: return metrics only
        resumen.put("chartBase64", null);
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
        for (Map.Entry<Integer, Integer> entry : sorted) {
            Map<String, Object> prodInfo = new HashMap<>();
            prodInfo.put("productId", entry.getKey());
            prodInfo.put("cantidadVendida", entry.getValue());
            // Agregar el nombre del producto
            ar.edu.uade.analytics.Entity.Product prod = purchaseService.getProductRepository().findById(entry.getKey()).orElse(null);
            String title = (prod != null && prod.getTitle() != null) ? prod.getTitle() : "ID " + entry.getKey();
            prodInfo.put("title", title);
            result.add(prodInfo);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-categories")
    public ResponseEntity<Map<String, Object>> getTopCategories(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "bar") String ignoredChartType) {
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
        for (Map.Entry<String, Integer> entry : sorted) {
            Map<String, Object> catInfo = new HashMap<>();
            catInfo.put("category", entry.getKey());
            catInfo.put("cantidadVendida", entry.getValue());
            result.add(catInfo);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary/chart")
    public ResponseEntity<byte[]> getSalesSummaryChart(
            ) {
        // No chart generation in the service layer
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/top-brands")
    public ResponseEntity<Map<String, Object>> getTopBrands(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "bar") String ignoredChartType) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
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
        List<Map.Entry<String, Integer>> sorted = brandSales.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            Map<String, Object> brandInfo = new HashMap<>();
            brandInfo.put("brand", entry.getKey());
            brandInfo.put("cantidadVendida", entry.getValue());
            result.add(brandInfo);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", null);
        return ResponseEntity.ok(response);
    }

    //Ventas diarias agrupadas por fecha 5→ Gráfico de líneas (Line Chart) para mostrar la evolución temporal.
    @GetMapping("/daily-sales")
    public ResponseEntity<Map<String, Object>> getDailySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
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
        for (String date : sortedDates) {
            Integer cantidad = dailySales.get(date);
            Map<String, Object> info = new HashMap<>();
            info.put("date", date);
            info.put("cantidadVentas", cantidad);
            result.add(info);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stock-history")
    public ResponseEntity<Map<String, Object>> getStockHistoryByProduct(
            @RequestParam("productId") Integer productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // Obtener historial de cambios de stock para el producto
        List<ar.edu.uade.analytics.Entity.StockChangeLog> logs =
                stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(productId);
        List<Map<String, Object>> result = new ArrayList<>();
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
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", null);
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
        for (ar.edu.uade.analytics.Entity.Product p : lowStock) {
            Map<String, Object> info = new HashMap<>();
            info.put("productId", p.getId());
            info.put("title", p.getTitle() != null ? p.getTitle() : "ID " + p.getId());
            info.put("stock", p.getStock());
            result.add(info);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", null);
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
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", null);
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
        String stockChartBase64 = null;
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
        // No charts generated: evolution chart omitted
        Map<String, Object> response = new HashMap<>();
        response.put("totalProductos", totalProductos);
        response.put("stockTotal", stockTotal);
        response.put("productosCriticos", productosCriticos);
        response.put("topProductIds", topProductIds);
        response.put("stockChartBase64", stockChartBase64);
        response.put("evolutionChartBase64", null);
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
        // No chart generation: return only metrics
        Map<String, Object> response = new HashMap<>();
        response.put("data", sorted);
        response.put("chartBase64", null);
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
                for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                    Integer productId = item.getProduct().getId();
                    Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                    productSalesByDay.computeIfAbsent(productId, k -> new ArrayList<>()).add(cantidad);
                }
            }
        }
        // Regresión lineal simple: calcular tendencia de ventas por producto
        Map<Integer, Double> productTrends = computeProductTrends(productSalesByDay);
        // No chart generation: return only metrics
        Map<String, Object> response = new HashMap<>();
        response.put("histogram", histogram);
        response.put("chartBase64", null);
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
        // No chart generation: return only metrics
        Map<String, Object> response = new HashMap<>();
        response.put("chartBase64", null);
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
        Map<String, Object> response = new HashMap<>();
        response.put("categoryName", categoryName);
        response.put("categoryGrowth", dateSales);
        response.put("chartBase64", null);
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
        for (ar.edu.uade.analytics.Entity.StockChangeLog log : logs) {
            LocalDateTime fecha = log.getChangedAt();
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
        Map<String, Object> response = new HashMap<>();
        response.put("events", events);
        response.put("chartBase64", null);
        return response;
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
