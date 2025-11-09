package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Purchase;
import ar.edu.uade.analytics.Service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SalesAnalyticsController {

    @Autowired
    PurchaseService purchaseService;

    @Autowired
    private ar.edu.uade.analytics.Repository.StockChangeLogRepository stockChangeLogRepository;

    public Map<String, Object> getSalesSummary(
            LocalDateTime startDate,
            LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        if (endDate == null) endDate = LocalDateTime.now();
        if (startDate == null) startDate = endDate.minusDays(29); // default 30 días
        int totalVentas = 0;
        float facturacionTotal = 0f;
        int productosVendidos = 0;
        Set<Integer> clientesActivos = new java.util.HashSet<>();
        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                LocalDateTime fecha = purchase.getDate();
                if (!fecha.isBefore(startDate) && !fecha.isAfter(endDate)) {
                    totalVentas++;
                    if (purchase.getUser() != null) clientesActivos.add(purchase.getUser().getId());
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
        resumen.put("clientesActivos", clientesActivos.size());
        float facturacionTotalEnMiles = Math.round((facturacionTotal / 1000f) * 100f) / 100f;
        resumen.put("facturacionTotalEnMiles", facturacionTotalEnMiles);
        resumen.put("facturacionTotalFormateado", String.format("$%,.2f", facturacionTotal));
        resumen.put("chartBase64", null);
        return resumen;
    }

    public Map<String, Object> getTrend(
            LocalDateTime startDate,
            LocalDateTime endDate) {
        if (endDate == null) endDate = LocalDateTime.now();
        if (startDate == null) startDate = endDate.minusDays(29);
        long days = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1;
        LocalDateTime prevEnd = startDate.minusDays(1);
        LocalDateTime prevStart = prevEnd.minusDays(days - 1);
        List<Purchase> purchases = purchaseService.getAllPurchases();

        Map<String, Map<String, Object>> currentMap = new java.util.TreeMap<>();
        Map<String, Map<String, Object>> previousMap = new java.util.TreeMap<>();

        for (int i = 0; i < days; i++) {
            LocalDateTime d = startDate.plusDays(i);
            String key = d.toLocalDate().toString();
            currentMap.put(key, baseTrendRow(key));
            LocalDateTime dPrev = prevStart.plusDays(i);
            String keyPrev = dPrev.toLocalDate().toString();
            previousMap.put(keyPrev, baseTrendRow(keyPrev));
        }

        for (Purchase purchase : purchases) {
            if (purchase.getStatus() != Purchase.Status.CONFIRMED) continue;
            LocalDateTime fecha = purchase.getDate();
            String day = fecha.toLocalDate().toString();
            boolean isCurrent = !fecha.isBefore(startDate) && !fecha.isAfter(endDate);
            boolean isPrevious = !fecha.isBefore(prevStart) && !fecha.isAfter(prevEnd);
            if (isCurrent) {
                Map<String, Object> row = currentMap.get(day);
                if (row != null) accumulateTrendRow(row, purchase);
            } else if (isPrevious) {
                Map<String, Object> row = previousMap.get(day);
                if (row != null) accumulateTrendRow(row, purchase);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("current", new ArrayList<>(currentMap.values()));
        response.put("previous", new ArrayList<>(previousMap.values()));
        response.put("range", Map.of(
                "start", startDate.toString(),
                "end", endDate.toString(),
                "prevStart", prevStart.toString(),
                "prevEnd", prevEnd.toString()
        ));
        return response;
    }

    private Map<String, Object> baseTrendRow(String date) {
        Map<String, Object> m = new HashMap<>();
        m.put("date", date);
        m.put("facturacion", 0f);
        m.put("ventas", 0);
        m.put("unidades", 0);
        return m;
    }
    private void accumulateTrendRow(Map<String, Object> row, Purchase purchase) {
        row.put("ventas", ((Number)row.get("ventas")).intValue() + 1);
        if (purchase.getCart() != null) {
            float fact = purchase.getCart().getFinalPrice() != null ? purchase.getCart().getFinalPrice() : 0f;
            row.put("facturacion", ((Number)row.get("facturacion")).floatValue() + fact);
            if (purchase.getCart().getItems() != null) {
                int unidades = purchase.getCart().getItems().stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
                row.put("unidades", ((Number)row.get("unidades")).intValue() + unidades);
            }
        }
    }

    public Map<String, Object> getTopProducts(
            int limit,
            LocalDateTime startDate,
            LocalDateTime endDate) {
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
        return response;
    }

    public Map<String, Object> getTopCategories(
            int limit,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String ignoredChartType) {
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
        return response;
    }

    public ResponseEntity<byte[]> getSalesSummaryChart(
    ) {
        // No chart generation in the service layer
        return ResponseEntity.noContent().build();
    }

    public Map<String, Object> getTopBrands(
            int limit,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String ignoredChartType) {
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
        return response;
    }

    //Ventas diarias agrupadas por fecha 5→ Gráfico de líneas (Line Chart) para mostrar la evolución temporal.
    public Map<String, Object> getDailySales(
            LocalDateTime startDate,
            LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();

        // Mapas por día
        Map<String, Integer> dailyTransactions = new HashMap<>();     // # de compras confirmadas (transacciones)
        Map<String, Float>   dailyRevenue      = new HashMap<>();     // facturación total del día
        Map<String, Integer> dailyUnits        = new HashMap<>();     // unidades (suma de quantities)

        for (Purchase purchase : purchases) {
            if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                LocalDateTime fecha = purchase.getDate();
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    String day = fecha.toLocalDate().toString();
                    // Transacciones
                    dailyTransactions.put(day, dailyTransactions.getOrDefault(day, 0) + 1);
                    // Facturación
                    float price = 0f;
                    if (purchase.getCart() != null && purchase.getCart().getFinalPrice() != null) {
                        price = purchase.getCart().getFinalPrice();
                    }
                    dailyRevenue.put(day, dailyRevenue.getOrDefault(day, 0f) + price);
                    // Unidades
                    int units = 0;
                    if (purchase.getCart() != null && purchase.getCart().getItems() != null) {
                        for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                            if (item != null && item.getQuantity() != null) {
                                units += item.getQuantity();
                            }
                        }
                    }
                    dailyUnits.put(day, dailyUnits.getOrDefault(day, 0) + units);
                }
            }
        }
        // Ordenar por fecha
        List<String> sortedDates = new ArrayList<>(dailyTransactions.keySet());
        java.util.Collections.sort(sortedDates);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String date : sortedDates) {
            Map<String, Object> info = new HashMap<>();
            Integer trans = dailyTransactions.getOrDefault(date, 0);
            Float revenue = dailyRevenue.getOrDefault(date, 0f);
            Integer units = dailyUnits.getOrDefault(date, 0);
            // Campos esperados por el frontend simplificado de correlación
            info.put("date", date);
            info.put("ventas", trans);          // alias para cantidad de transacciones
            info.put("cantidadVentas", trans);  // compatibilidad con código previo
            info.put("facturacion", revenue);   // total ARS
            info.put("unidades", units);        // suma de ítems (si se necesitara en otros gráficos)
            result.add(info);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        response.put("chartBase64", null);
        return response;
    }

    public Map<String, Object> getStockHistoryByProduct(
            Integer productId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
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
        return response;
    }

    public Map<String, Object> getLowStockProducts(
            int threshold,
            int limit) {
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
        return response;
    }

    public Map<String, Object> getStockHistoryByProductCode(
            Integer productCode,
            boolean showProfit,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        // Buscar el producto por productCode
        ar.edu.uade.analytics.Entity.Product product = purchaseService.getProductRepository().findByProductCode(productCode);
        if (product == null) {
            return Map.of("error", "Producto no encontrado");
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
        return response;
    }

    public Map<String, Object> getProductsDashboard(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer categoryId,
            Integer brandId) {
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
        return response;
    }

    public Map<String, Object> getTopCustomers(
            int limit,
            LocalDateTime startDate,
            LocalDateTime endDate) {
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
        return response;
    }

    public Map<String, Object> getSalesHistogram(
            LocalDateTime startDate,
            LocalDateTime endDate) {
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
        return response;
    }

    public Map<String, Object> getSalesCorrelation(
            LocalDateTime startDate,
            LocalDateTime endDate) {
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
        return response;
    }

    public Map<String, Object> getCategoryGrowth(
            Integer categoryId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
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
        return response;
    }

    public Map<String, Object> getProductEventsTimeline(
            Integer productId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int topN) {
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
                return response;
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
        return response;
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

    public ResponseEntity<Map<String,Object>> getAtRiskCustomers(
            LocalDateTime startDate,
            LocalDateTime endDate) {
        if (endDate == null) endDate = LocalDateTime.now();
        if (startDate == null) startDate = endDate.minusDays(29);
        long days = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1;
        LocalDateTime prevEnd = startDate.minusDays(1);
        LocalDateTime prevStart = prevEnd.minusDays(days - 1);
        List<Purchase> purchases = purchaseService.getAllPurchases();
        Map<Integer, ar.edu.uade.analytics.Entity.User> userMap = new HashMap<>();
        Set<Integer> prevActive = new java.util.HashSet<>();
        Set<Integer> curActive = new java.util.HashSet<>();
        Map<Integer, LocalDateTime> lastPrevPurchase = new HashMap<>();
        for (Purchase p : purchases) {
            if (p.getStatus() != Purchase.Status.CONFIRMED || p.getUser()==null) continue;
            LocalDateTime d = p.getDate();
            int uid = p.getUser().getId();
            userMap.put(uid, p.getUser());
            if (!d.isBefore(prevStart) && !d.isAfter(prevEnd)) {
                prevActive.add(uid);
                lastPrevPurchase.put(uid, lastPrevPurchase.getOrDefault(uid, d).isAfter(d)? lastPrevPurchase.get(uid): d);
            }
            if (!d.isBefore(startDate) && !d.isAfter(endDate)) {
                curActive.add(uid);
            }
        }
        List<Map<String,Object>> atRisk = new ArrayList<>();
        for (Integer uid : prevActive) {
            if (!curActive.contains(uid)) {
                ar.edu.uade.analytics.Entity.User u = userMap.get(uid);
                Map<String,Object> row = new HashMap<>();
                row.put("userId", uid);
                row.put("name", u!=null? u.getName(): "Usuario "+uid);
                row.put("email", u!=null? u.getEmail(): null);
                row.put("lastPurchase", lastPrevPurchase.get(uid)!=null? lastPrevPurchase.get(uid).toString(): null);
                atRisk.add(row);
            }
        }
        Map<String,Object> resp = new HashMap<>();
        resp.put("data", atRisk);
        return ResponseEntity.ok(resp);
    }

    public ResponseEntity<Map<String,Object>> getSlowMovers(
            int minStock,
            int maxSales,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        if (endDate == null) endDate = LocalDateTime.now();
        if (startDate == null) startDate = endDate.minusDays(29);
        List<Purchase> purchases = purchaseService.getAllPurchases();
        Map<Integer,Integer> sales = new HashMap<>();
        Map<Integer,LocalDateTime> lastSale = new HashMap<>();
        for (Purchase p: purchases) {
            if (p.getStatus()!= Purchase.Status.CONFIRMED || p.getCart()==null || p.getCart().getItems()==null) continue;
            LocalDateTime d = p.getDate();
            if (d.isBefore(startDate) || d.isAfter(endDate)) continue;
            for (ar.edu.uade.analytics.Entity.CartItem it: p.getCart().getItems()) {
                if (it.getProduct()==null) continue;
                int pid = it.getProduct().getId();
                int qty = it.getQuantity()!=null? it.getQuantity():0;
                sales.put(pid, sales.getOrDefault(pid,0)+qty);
                lastSale.put(pid, lastSale.getOrDefault(pid,d).isAfter(d)? lastSale.get(pid): d);
            }
        }
        List<ar.edu.uade.analytics.Entity.Product> products = purchaseService.getProductRepository().findAll();
        List<Map<String,Object>> result = new ArrayList<>();
        for (ar.edu.uade.analytics.Entity.Product pr: products) {
            Integer stock = pr.getStock();
            if (stock==null) continue;
            if (stock < minStock) continue; // slow mover: bastante stock
            int s = sales.getOrDefault(pr.getId(),0);
            if (s <= maxSales) {
                Map<String,Object> row = new HashMap<>();
                row.put("productId", pr.getId());
                row.put("title", pr.getTitle());
                row.put("stock", stock);
                row.put("sales", s);
                LocalDateTime ls = lastSale.get(pr.getId());
                row.put("daysSinceLastSale", ls==null? null: ChronoUnit.DAYS.between(ls.toLocalDate(), endDate.toLocalDate()));
                result.add(row);
            }
        }
        Map<String,Object> resp = new HashMap<>();
        resp.put("data", result);
        return ResponseEntity.ok(resp);
    }

    public ResponseEntity<Map<String,Object>> getFastMovers(
            double growthPct,
            int stockThreshold,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        if (endDate == null) endDate = LocalDateTime.now();
        if (startDate == null) startDate = endDate.minusDays(29);
        long days = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1;
        LocalDateTime prevEnd = startDate.minusDays(1);
        LocalDateTime prevStart = prevEnd.minusDays(days - 1);
        List<Purchase> purchases = purchaseService.getAllPurchases();
        Map<Integer,Integer> curSales = new HashMap<>();
        Map<Integer,Integer> prevSales = new HashMap<>();
        for (Purchase p: purchases) {
            if (p.getStatus()!= Purchase.Status.CONFIRMED || p.getCart()==null || p.getCart().getItems()==null) continue;
            LocalDateTime d = p.getDate();
            boolean isCur = !d.isBefore(startDate) && !d.isAfter(endDate);
            boolean isPrev = !d.isBefore(prevStart) && !d.isAfter(prevEnd);
            if (!isCur && !isPrev) continue;
            for (ar.edu.uade.analytics.Entity.CartItem it: p.getCart().getItems()) {
                if (it.getProduct()==null) continue;
                int pid = it.getProduct().getId();
                int qty = it.getQuantity()!=null? it.getQuantity():0;
                if (isCur) curSales.put(pid, curSales.getOrDefault(pid,0)+qty);
                if (isPrev) prevSales.put(pid, prevSales.getOrDefault(pid,0)+qty);
            }
        }
        List<ar.edu.uade.analytics.Entity.Product> products = purchaseService.getProductRepository().findAll();
        List<Map<String,Object>> result = new ArrayList<>();
        for (ar.edu.uade.analytics.Entity.Product pr: products) {
            int c = curSales.getOrDefault(pr.getId(),0);
            int p = prevSales.getOrDefault(pr.getId(),0);
            if (p==0 && c==0) continue;
            double g = p==0? 100.0: ((double)(c-p)/p)*100.0;
            Integer stock = pr.getStock();
            if (g >= growthPct && stock!=null && stock <= stockThreshold) {
                Map<String,Object> row = new HashMap<>();
                row.put("productId", pr.getId());
                row.put("title", pr.getTitle());
                row.put("growthPct", g);
                row.put("salesCurrent", c);
                row.put("salesPrevious", p);
                row.put("stock", stock);
                result.add(row);
            }
        }
        Map<String,Object> resp = new HashMap<>();
        resp.put("data", result);
        return ResponseEntity.ok(resp);
    }
}
