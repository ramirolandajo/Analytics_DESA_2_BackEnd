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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections; // Import Collections for emptyList

@RestController
@RequestMapping("/analytics/sales")
public class SalesAnalyticsController {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private ar.edu.uade.analytics.Repository.StockChangeLogRepository stockChangeLogRepository;

    @Autowired
    private ar.edu.uade.analytics.Repository.CartRepository cartRepository;

    @Autowired
    private ar.edu.uade.analytics.Repository.ConsumedEventLogRepository consumedEventLogRepository;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSalesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        if (endDate == null) endDate = LocalDateTime.now();
        if (startDate == null) startDate = endDate.minusDays(29);
        int totalVentas = 0;
        float facturacionTotal = 0f;
        int productosVendidos = 0;
        Set<Integer> clientesActivos = new java.util.HashSet<>();
        if (purchases != null) { // Added null check
            for (Purchase purchase : purchases) {
                if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                    LocalDateTime fecha = purchase.getDate();
                    if (fecha == null) continue;
                    if (!fecha.isBefore(startDate) && !fecha.isAfter(endDate)) {
                        totalVentas++;
                        if (purchase.getUser() != null) clientesActivos.add(purchase.getUser().getId());
                        if (purchase.getCart() != null) {
                            facturacionTotal += purchase.getCart().getFinalPrice() != null ? purchase.getCart().getFinalPrice() : 0f;
                            if (purchase.getCart().getItems() != null) {
                                productosVendidos += purchase.getCart().getItems().stream().mapToInt(i -> i != null && i.getQuantity() != null ? i.getQuantity() : 0).sum();
                            }
                        }
                    }
                }
            }
        }

        if ((purchases == null || purchases.isEmpty()) || totalVentas == 0) {
            Map<String, Object> resumenDesdeCarts = computeSummaryFromCarts();
            float total = ((Number) resumenDesdeCarts.getOrDefault("facturacionTotal", 0f)).floatValue();
            float facturacionTotalEnMiles = Math.round((total / 1000f) * 100f) / 100f;
            resumenDesdeCarts.put("facturacionTotalEnMiles", facturacionTotalEnMiles);
            resumenDesdeCarts.put("facturacionTotalFormateado", String.format("$%,.2f", total));
            resumenDesdeCarts.putIfAbsent("chartBase64", null);
            return ResponseEntity.ok(resumenDesdeCarts);
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
        return ResponseEntity.ok(resumen);
    }

    @GetMapping("/trend")
    public ResponseEntity<Map<String, Object>> getTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
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

        if (purchases != null) { // Added null check
            for (Purchase purchase : purchases) {
                if (purchase.getStatus() != Purchase.Status.CONFIRMED) continue;
                LocalDateTime fecha = purchase.getDate();
                if (fecha == null) continue;
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
        return ResponseEntity.ok(response);
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
                int unidades = purchase.getCart().getItems().stream().mapToInt(i -> i != null && i.getQuantity() != null ? i.getQuantity() : 0).sum();
                row.put("unidades", ((Number)row.get("unidades")).intValue() + unidades);
            }
        }
    }

    @GetMapping("/top-products")
    public ResponseEntity<Map<String, Object>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        Map<Integer, Integer> productSales = new HashMap<>();
        if (purchases != null) { // Added null check
            for (Purchase purchase : purchases) {
                if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                    LocalDateTime fecha = purchase.getDate();
                    if (fecha == null) continue;
                    if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                        if (purchase.getCart() != null && purchase.getCart().getItems() != null) {
                            for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                                if (item == null || item.getProduct() == null) continue;
                                Integer productId = item.getProduct().getId();
                                Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                                if (productId == null) continue;
                                productSales.put(productId, productSales.getOrDefault(productId, 0) + cantidad);
                            }
                        }
                    }
                }
            }
        }

        if (productSales.isEmpty()) {
            List<ar.edu.uade.analytics.Entity.Cart> carts = cartRepository.findAll();
            for (ar.edu.uade.analytics.Entity.Cart c : carts) {
                if (c.getItems() == null) continue;
                for (ar.edu.uade.analytics.Entity.CartItem item : c.getItems()) {
                    if (item == null || item.getProduct() == null) continue;
                    Integer productId = item.getProduct().getId();
                    Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                    productSales.put(productId, productSales.getOrDefault(productId, 0) + cantidad);
                }
            }
        }

        List<Map.Entry<Integer, Integer>> sorted = productSales.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : sorted) {
            Map<String, Object> prodInfo = new HashMap<>();
            prodInfo.put("productId", entry.getKey());
            prodInfo.put("cantidadVendida", entry.getValue());
            ar.edu.uade.analytics.Entity.Product prod = null;
            ar.edu.uade.analytics.Repository.ProductRepository pr = purchaseService.getProductRepository();
            if (pr != null) {
                prod = pr.findById(entry.getKey()).orElse(null);
            }
            String title = (prod != null && prod.getTitle() != null) ? prod.getTitle() : ("ID " + entry.getKey());
            prodInfo.put("title", title);
            String productCode = (prod != null && prod.getProductCode() != null) ? prod.getProductCode().toString() : "N/A";
            prodInfo.put("productCode", productCode);
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
        Map<String, Integer> categorySales = new HashMap<>();
        if (purchases != null) { // Added null check
            for (Purchase purchase : purchases) {
                if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                    LocalDateTime fecha = purchase.getDate();
                    if (fecha == null) continue;
                    if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                        if (purchase.getCart() != null && purchase.getCart().getItems() != null) {
                            for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                                ar.edu.uade.analytics.Entity.Product product = item != null ? item.getProduct() : null;
                                Integer cantidad = (item != null && item.getQuantity() != null) ? item.getQuantity() : 0;
                                if (product != null && product.getCategories() != null && !product.getCategories().isEmpty()) {
                                    for (ar.edu.uade.analytics.Entity.Category category : product.getCategories()) {
                                        String catName = category != null ? category.getName() : null;
                                        if (catName == null) catName = "Otros";
                                        categorySales.put(catName, categorySales.getOrDefault(catName, 0) + cantidad);
                                    }
                                } else {
                                    categorySales.put("Otros", categorySales.getOrDefault("Otros", 0) + cantidad);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (categorySales.isEmpty()) {
            List<ar.edu.uade.analytics.Entity.Cart> carts = cartRepository.findAll();
            for (ar.edu.uade.analytics.Entity.Cart c : carts) {
                if (c.getItems() == null) continue;
                for (ar.edu.uade.analytics.Entity.CartItem item : c.getItems()) {
                    if (item == null) continue;
                    ar.edu.uade.analytics.Entity.Product product = item.getProduct();
                    Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                    if (product != null && product.getCategories() != null && !product.getCategories().isEmpty()) {
                        for (ar.edu.uade.analytics.Entity.Category category : product.getCategories()) {
                            String catName = category.getName();
                            categorySales.put(catName, categorySales.getOrDefault(catName, 0) + cantidad);
                        }
                    } else {
                        categorySales.put("Otros", categorySales.getOrDefault("Otros", 0) + cantidad);
                    }
                }
            }
        }

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
        if (purchases != null) { // Added null check
            for (Purchase purchase : purchases) {
                if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                    LocalDateTime fecha = purchase.getDate();
                    if (fecha == null) continue;
                    if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                        if (purchase.getCart() != null && purchase.getCart().getItems() != null) {
                            for (ar.edu.uade.analytics.Entity.CartItem item : purchase.getCart().getItems()) {
                                ar.edu.uade.analytics.Entity.Product product = item.getProduct();
                                String brandName = (product != null && product.getBrand() != null && product.getBrand().getName() != null)
                                        ? product.getBrand().getName() : "Otros";
                                Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                                brandSales.put(brandName, brandSales.getOrDefault(brandName, 0) + cantidad);
                            }
                        }
                    }
                }
            }
        }

        if (brandSales.isEmpty()) {
            List<ar.edu.uade.analytics.Entity.Cart> carts = cartRepository.findAll();
            for (ar.edu.uade.analytics.Entity.Cart c : carts) {
                if (c.getItems() == null) continue;
                for (ar.edu.uade.analytics.Entity.CartItem item : c.getItems()) {
                    if (item == null) continue;
                    ar.edu.uade.analytics.Entity.Product product = item.getProduct();
                    String brandName = (product != null && product.getBrand() != null && product.getBrand().getName() != null)
                            ? product.getBrand().getName() : "Otros";
                    Integer cantidad = item.getQuantity() != null ? item.getQuantity() : 0;
                    brandSales.put(brandName, brandSales.getOrDefault(brandName, 0) + cantidad);
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

    @GetMapping("/daily-sales")
    public ResponseEntity<Map<String, Object>> getDailySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        Map<String, Integer> dailyTransactions = new HashMap<>();
        Map<String, Float>   dailyRevenue      = new HashMap<>();
        Map<String, Integer> dailyUnits        = new HashMap<>();
        if (purchases != null) { // Added null check
            for (Purchase purchase : purchases) {
                if (purchase.getStatus() == Purchase.Status.CONFIRMED) {
                    LocalDateTime fecha = purchase.getDate();
                    if (fecha == null) continue;
                    if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                        String day = fecha.toLocalDate().toString();
                        dailyTransactions.put(day, dailyTransactions.getOrDefault(day, 0) + 1);
                        float price = 0f;
                        if (purchase.getCart() != null && purchase.getCart().getFinalPrice() != null) {
                            price = purchase.getCart().getFinalPrice();
                        }
                        dailyRevenue.put(day, dailyRevenue.getOrDefault(day, 0f) + price);
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
        }

        if (dailyTransactions.isEmpty()) {
            OffsetDateTime start = startDate != null ? startDate.atOffset(ZoneOffset.UTC) : null;
            OffsetDateTime end = endDate != null ? endDate.atOffset(ZoneOffset.UTC) : null;
            List<ar.edu.uade.analytics.Entity.ConsumedEventLog> logs;
            if (start != null && end != null) {
                logs = consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseAndProcessedAtBetweenOrderByProcessedAtAsc(
                        ar.edu.uade.analytics.Entity.ConsumedEventLog.Status.PROCESSED,
                        "Compra confirmada",
                        start,
                        end
                );
            } else {
                logs = consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(
                        ar.edu.uade.analytics.Entity.ConsumedEventLog.Status.PROCESSED,
                        "Compra confirmada"
                );
            }
            for (ar.edu.uade.analytics.Entity.ConsumedEventLog log : logs) {
                String day = null;
                float price = 0f;
                int units = 0;
                try {
                    Map<?,?> root = objectMapper.readValue(log.getPayloadJson(), Map.class);
                    Object ts = root.get("timestamp");
                    if (ts instanceof Number) {
                        long seconds = ((Number) ts).longValue();
                        day = java.time.Instant.ofEpochSecond(seconds).atZone(ZoneId.of("UTC")).toLocalDate().toString(); // Changed to UTC
                    }
                    Object payload = root.get("payload");
                    if (payload instanceof Map<?,?> payloadMap) {
                        Object cart = payloadMap.get("cart");
                        if (cart instanceof Map<?,?> cartMap) {
                            Object fp = cartMap.get("finalPrice");
                            if (fp instanceof Number) price = ((Number) fp).floatValue();
                            Object items = cartMap.get("items");
                            if (items instanceof List<?> list) {
                                for (Object o : list) {
                                    if (o instanceof Map<?,?> m) {
                                        Object q = m.get("quantity");
                                        if (q instanceof Number) units += ((Number) q).intValue();
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) { }
                if (day == null && log.getProcessedAt() != null) {
                    day = log.getProcessedAt().atZoneSameInstant(ZoneId.of("UTC")).toLocalDate().toString(); // Changed to UTC
                }
                if (day != null) {
                    dailyTransactions.put(day, dailyTransactions.getOrDefault(day, 0) + 1);
                    dailyRevenue.put(day, dailyRevenue.getOrDefault(day, 0f) + price);
                    dailyUnits.put(day, dailyUnits.getOrDefault(day, 0) + units);
                }
            }
        }

        List<String> sortedDates = new ArrayList<>(dailyTransactions.keySet());
        java.util.Collections.sort(sortedDates);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String date : sortedDates) {
            Map<String, Object> info = new HashMap<>();
            Integer trans = dailyTransactions.getOrDefault(date, 0);
            Float revenue = dailyRevenue.getOrDefault(date, 0f);
            Integer units = dailyUnits.getOrDefault(date, 0);
            info.put("date", date);
            info.put("ventas", trans);
            info.put("cantidadVentas", trans);
            info.put("facturacion", revenue);
            info.put("unidades", units);
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
        List<ar.edu.uade.analytics.Entity.StockChangeLog> logs =
                stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(productId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ar.edu.uade.analytics.Entity.StockChangeLog log : logs) {
            LocalDateTime fecha = log.getChangedAt();
            if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                Map<String, Object> info = new HashMap<>();
                info.put("date", fecha.toLocalDate().toString());
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
        ar.edu.uade.analytics.Repository.ProductRepository pr = purchaseService.getProductRepository();
        if (pr == null) {
            Map<String, Object> body = new HashMap<>();
            body.put("data", java.util.Collections.emptyList());
            body.put("chartBase64", null);
            return ResponseEntity.ok(body);
        }
        List<ar.edu.uade.analytics.Entity.Product> products = pr.findAll();
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
        ar.edu.uade.analytics.Repository.ProductRepository pr = purchaseService.getProductRepository();
        if (pr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Product repository not available"));
        }
        ar.edu.uade.analytics.Entity.Product product = pr.findByProductCode(productCode);
        if (product == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Producto no encontrado"));
        }

        List<Map<String, Object>> allEvents = new ArrayList<>();

        // 1. Logs de cambio de stock (existentes)
        List<ar.edu.uade.analytics.Entity.StockChangeLog> logs =
                stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(product.getId());
        float profitAccum = 0f;
        for (ar.edu.uade.analytics.Entity.StockChangeLog log : logs) {
            LocalDateTime fecha = log.getChangedAt();
            if (fecha == null) continue;
            if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                Map<String, Object> info = new HashMap<>();
                info.put("date", fecha.toLocalDate().toString());
                info.put("oldStock", log.getOldStock());
                info.put("newStock", log.getNewStock());
                info.put("quantityChanged", log.getQuantityChanged());
                info.put("reason", log.getReason());
                float profit = 0f;
                if (showProfit && "Venta".equalsIgnoreCase(log.getReason())) {
                    Float price = product.getPrice() != null ? product.getPrice() : 0f;
                    profit = price * Math.abs(log.getQuantityChanged());
                }
                profitAccum += profit;
                info.put("profit", profit);
                info.put("profitAccumulated", profitAccum);
                // Para ordenación posterior
                info.put("_eventDateTime", fecha);
                allEvents.add(info);
            }
        }

        // 2. Eventos de modificación de producto que alteran stock (ConsumedEventLog)
        List<ar.edu.uade.analytics.Entity.ConsumedEventLog> modLogs =
                consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(
                        ar.edu.uade.analytics.Entity.ConsumedEventLog.Status.PROCESSED, "Producto");
        for (ar.edu.uade.analytics.Entity.ConsumedEventLog ev : modLogs) {
            // Filtrar por rango temporal
            OffsetDateTime processedAt = ev.getProcessedAt();
            if (processedAt == null) continue;
            LocalDateTime fecha = processedAt.toLocalDateTime();
            if ((startDate != null && fecha.isBefore(startDate)) || (endDate != null && fecha.isAfter(endDate))) continue;

            try {
                Map<?, ?> root = objectMapper.readValue(ev.getPayloadJson(), Map.class);
                Object payload = root.get("payload");
                if (!(payload instanceof Map<?, ?> payloadMap)) continue;

                // Intentar obtener productCode dentro del payload
                Integer code = null;
                Object pc = payloadMap.get("productCode");
                if (pc instanceof Number) code = ((Number) pc).intValue();
                // Alternativa: dentro de "product" subobjeto
                if (code == null) {
                    Object prodObj = payloadMap.get("product");
                    if (prodObj instanceof Map<?, ?> prodMap) {
                        Object pc2 = prodMap.get("productCode");
                        if (pc2 instanceof Number) code = ((Number) pc2).intValue();
                    }
                }
                if (code == null || !code.equals(productCode)) continue;

                // Detectar campos de stock
                Integer oldStock = null;
                Integer newStock = null;
                Object os = payloadMap.get("oldStock");
                Object ns = payloadMap.get("newStock");
                if (os instanceof Number) oldStock = ((Number) os).intValue();
                if (ns instanceof Number) newStock = ((Number) ns).intValue();

                // Algunos eventos podrían solo tener "stock"
                if (newStock == null) {
                    Object s = payloadMap.get("stock");
                    if (s instanceof Number) newStock = ((Number) s).intValue();
                }

                Integer quantityChanged = null;
                if (oldStock != null && newStock != null) {
                    quantityChanged = newStock - oldStock;
                }

                // Incluir solo si realmente hay indicio de cambio de stock
                if (oldStock != null || newStock != null) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("date", fecha.toLocalDate().toString());
                    info.put("oldStock", oldStock);
                    info.put("newStock", newStock);
                    info.put("quantityChanged", quantityChanged);
                    info.put("reason", "Modificación de producto");
                    float profit = 0f; // No se calcula profit por modificación
                    info.put("profit", profit);
                    profitAccum += profit;
                    info.put("profitAccumulated", profitAccum);
                    info.put("_eventDateTime", fecha);
                    allEvents.add(info);
                }
            } catch (Exception ignored) { }
        }

        // 3. Ordenar todos los eventos por fecha/hora
        allEvents.sort((a, b) -> {
            LocalDateTime da = (LocalDateTime) a.get("_eventDateTime");
            LocalDateTime db = (LocalDateTime) b.get("_eventDateTime");
            return da.compareTo(db);
        });

        // Limpiar campo interno
        for (Map<String, Object> m : allEvents) {
            m.remove("_eventDateTime");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("data", allEvents);
        response.put("chartBase64", null);
        return ResponseEntity.ok(response);
    }
    private Map<String, Object> computeSummaryFromCarts() {
        List<ar.edu.uade.analytics.Entity.Cart> carts = cartRepository.findAll();
        int totalVentas = carts != null ? carts.size() : 0;
        float facturacionTotal = 0f;
        int productosVendidos = 0;
        Set<Integer> clientesActivos = new java.util.HashSet<>();
        if (carts != null) {
            for (ar.edu.uade.analytics.Entity.Cart c : carts) {
                if (c == null) continue;
                if (c.getFinalPrice() != null) facturacionTotal += c.getFinalPrice();
                if (c.getItems() != null) {
                    for (ar.edu.uade.analytics.Entity.CartItem item : c.getItems()) {
                        if (item != null && item.getQuantity() != null) {
                            productosVendidos += item.getQuantity();
                        }
                    }
                }
                if (c.getUser() != null && c.getUser().getId() != null) {
                    clientesActivos.add(c.getUser().getId());
                }
            }
        }
        Map<String, Object> resumen = new HashMap<>();
        resumen.put("totalVentas", totalVentas);
        resumen.put("facturacionTotal", facturacionTotal);
        resumen.put("productosVendidos", productosVendidos);
        resumen.put("clientesActivos", clientesActivos.size());
        return resumen;
    }

    @GetMapping("/top-customers")
    public ResponseEntity<Map<String, Object>> getTopCustomers(
            @RequestParam(defaultValue = "10") int limit) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        Map<Integer, Float> spendByUser = new HashMap<>();
        Map<Integer, Integer> salesByUser = new HashMap<>();
        Map<Integer, String> nameByUser = new HashMap<>();
        Map<Integer, String> emailByUser = new HashMap<>();
        if (purchases != null) { // Added null check
            for (Purchase p : purchases) {
                if (p.getStatus() != Purchase.Status.CONFIRMED) continue;
                if (p.getUser() == null) continue;
                Integer uid = p.getUser().getId();
                nameByUser.put(uid, p.getUser().getName());
                emailByUser.put(uid, p.getUser().getEmail());
                float price = (p.getCart() != null && p.getCart().getFinalPrice() != null) ? p.getCart().getFinalPrice() : 0f;
                spendByUser.put(uid, spendByUser.getOrDefault(uid, 0f) + price);
                salesByUser.put(uid, salesByUser.getOrDefault(uid, 0) + 1);
            }
        }

        if (spendByUser.isEmpty()) {
            List<ar.edu.uade.analytics.Entity.Cart> carts = cartRepository.findAll();
            for (ar.edu.uade.analytics.Entity.Cart c : carts) {
                if (c.getUser() == null) continue;
                Integer uid = c.getUser().getId();
                nameByUser.put(uid, c.getUser().getName());
                emailByUser.put(uid, c.getUser().getEmail());
                float price = c.getFinalPrice() != null ? c.getFinalPrice() : 0f;
                spendByUser.put(uid, spendByUser.getOrDefault(uid, 0f) + price);
                salesByUser.put(uid, salesByUser.getOrDefault(uid, 0) + 1);
            }
        }
        List<Map.Entry<Integer, Float>> sorted = spendByUser.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Float> e : sorted) {
            Integer uid = e.getKey();
            Map<String, Object> row = new HashMap<>();
            row.put("userId", uid);
            row.put("name", nameByUser.get(uid));
            row.put("email", emailByUser.get(uid));
            row.put("totalSpent", e.getValue());
            row.put("ventas", salesByUser.getOrDefault(uid, 0));
            result.add(row);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("data", result);
        body.put("chartBase64", null);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/histogram")
    public ResponseEntity<Map<String, Object>> getSalesHistogram(@RequestParam(defaultValue = "10") int bins) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        List<Float> amounts = new ArrayList<>();
        if (purchases != null) { // Added null check
            for (Purchase p : purchases) {
                if (p.getStatus() != Purchase.Status.CONFIRMED) continue;
                if (p.getCart() != null && p.getCart().getFinalPrice() != null) amounts.add(p.getCart().getFinalPrice());
            }
        }
        if (amounts.isEmpty()) {
            for (ar.edu.uade.analytics.Entity.Cart c : cartRepository.findAll()) {
                if (c.getFinalPrice() != null) amounts.add(c.getFinalPrice());
            }
        }
        Map<String, Object> resp = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();
        if (amounts.isEmpty()) {
            resp.put("data", data);
            resp.put("chartBase64", null);
            return ResponseEntity.ok(resp);
        }
        float min = amounts.stream().min(Float::compare).orElse(0f);
        float max = amounts.stream().max(Float::compare).orElse(min);
        if (bins < 1) bins = 1;
        if (max == min) {
            Map<String, Object> row = new HashMap<>();
            row.put("binStart", min);
            row.put("binEnd", max);
            row.put("count", amounts.size());
            data.add(row);
            resp.put("data", data);
            resp.put("chartBase64", null);
            return ResponseEntity.ok(resp);
        }
        float width = (max - min) / bins;
        int[] counts = new int[bins];
        for (Float v : amounts) {
            int idx = (int) Math.floor((v - min) / width);
            if (idx == bins) idx = bins - 1;
            counts[idx]++;
        }
        for (int i = 0; i < bins; i++) {
            float start = min + i * width;
            float end = (i == bins - 1) ? max : start + width;
            Map<String, Object> row = new HashMap<>();
            row.put("binStart", start);
            row.put("binEnd", end);
            row.put("count", counts[i]);
            data.add(row);
        }
        resp.put("data", data);
        resp.put("chartBase64", null);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/correlation")
    public ResponseEntity<Map<String, Object>> getCorrelation() {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        Map<Integer, Integer> unitsByProduct = new HashMap<>();
        Map<Integer, Float> priceByProduct = new HashMap<>();
        if (purchases != null) { // Added null check
            for (Purchase p : purchases) {
                if (p.getStatus() != Purchase.Status.CONFIRMED) continue;
                if (p.getCart() == null || p.getCart().getItems() == null) continue;
                for (ar.edu.uade.analytics.Entity.CartItem item : p.getCart().getItems()) {
                    if (item == null || item.getProduct() == null) continue;
                    Integer pid = item.getProduct().getId();
                    Integer q = item.getQuantity() != null ? item.getQuantity() : 0;
                    if (pid == null) continue;
                    unitsByProduct.put(pid, unitsByProduct.getOrDefault(pid, 0) + q);
                    if (item.getProduct().getPrice() != null) priceByProduct.put(pid, item.getProduct().getPrice());
                }
            }
        }
        if (unitsByProduct.isEmpty()) {
            List<ar.edu.uade.analytics.Entity.Cart> carts = cartRepository.findAll();
            if (carts != null) {
                for (ar.edu.uade.analytics.Entity.Cart c : carts) {
                    if (c == null || c.getItems() == null) continue;
                    for (ar.edu.uade.analytics.Entity.CartItem item : c.getItems()) {
                        if (item == null || item.getProduct() == null) continue;
                        Integer pid = item.getProduct().getId();
                        Integer q = item.getQuantity() != null ? item.getQuantity() : 0;
                        if (pid == null) continue;
                        unitsByProduct.put(pid, unitsByProduct.getOrDefault(pid, 0) + q);
                        if (item.getProduct().getPrice() != null) priceByProduct.put(pid, item.getProduct().getPrice());
                    }
                }
            }
        }
        List<Map<String, Object>> points = new ArrayList<>();
        ar.edu.uade.analytics.Repository.ProductRepository pr = purchaseService.getProductRepository();
        for (Map.Entry<Integer, Integer> e : unitsByProduct.entrySet()) {
            Integer pid = e.getKey();
            ar.edu.uade.analytics.Entity.Product prod = null;
            if (pr != null) {
                prod = pr.findById(pid).orElse(null);
            }
            Map<String, Object> m = new HashMap<>();
            m.put("productId", pid);
            m.put("title", prod != null ? prod.getTitle() : ("ID " + pid));
            String productCode = (prod != null && prod.getProductCode() != null) ? prod.getProductCode().toString() : "N/A";
            m.put("productCode", productCode);
            m.put("price", priceByProduct.getOrDefault(pid, 0f));
            m.put("unitsSold", e.getValue());
            points.add(m);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("data", points);
        resp.put("chartBase64", null);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/category-growth")
    public ResponseEntity<Map<String, Object>> getCategoryGrowth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        Map<String, Integer> totals = new HashMap<>();
        if (purchases != null) { // Added null check
            for (Purchase p : purchases) {
                if (p.getStatus() != Purchase.Status.CONFIRMED) continue;
                LocalDateTime fecha = p.getDate();
                if (fecha == null) continue;
                if ((startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))) {
                    if (p.getCart() != null && p.getCart().getItems() != null) {
                        for (ar.edu.uade.analytics.Entity.CartItem item : p.getCart().getItems()) {
                            ar.edu.uade.analytics.Entity.Product product = item.getProduct();
                            Integer q = item.getQuantity() != null ? item.getQuantity() : 0;
                            if (product != null && product.getCategories() != null && !product.getCategories().isEmpty()) {
                                for (ar.edu.uade.analytics.Entity.Category cat : product.getCategories()) {
                                    String name = cat != null ? cat.getName() : null;
                                    if (name == null) name = "Otros";
                                    totals.put(name, totals.getOrDefault(name, 0) + q);
                                }
                            } else {
                                totals.put("Otros", totals.getOrDefault("Otros", 0) + q);
                            }
                        }
                    }
                }
            }
        }

        if (totals.isEmpty()) {
            List<ar.edu.uade.analytics.Entity.ConsumedEventLog> logs = consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(
                    ar.edu.uade.analytics.Entity.ConsumedEventLog.Status.PROCESSED, "Compra confirmada");
            if (logs != null) {
                ar.edu.uade.analytics.Repository.ProductRepository pr = purchaseService.getProductRepository();
                for (ar.edu.uade.analytics.Entity.ConsumedEventLog log : logs) {
                    try {
                        Map<?,?> root = objectMapper.readValue(log.getPayloadJson(), Map.class);
                        Object payload = root.get("payload");
                        if (payload instanceof Map<?,?> payloadMap) {
                            Object cart = payloadMap.get("cart");
                            if (cart instanceof Map<?,?> cartMap) {
                                Object items = cartMap.get("items");
                                if (items instanceof List<?> list) {
                                    for (Object o : list) {
                                        if (o instanceof Map<?,?> m) {
                                            Integer code = null; Integer q = 0;
                                            Object pc = m.get("productCode"); if (pc instanceof Number) code = ((Number) pc).intValue();
                                            Object qq = m.get("quantity");   if (qq instanceof Number) q = ((Number) qq).intValue();
                                            if (code != null && pr != null) {
                                                ar.edu.uade.analytics.Entity.Product prod = pr.findByProductCode(code);
                                                if (prod != null && prod.getCategories() != null && !prod.getCategories().isEmpty()) {
                                                    for (ar.edu.uade.analytics.Entity.Category cat : prod.getCategories()) {
                                                        String name = cat != null ? cat.getName() : null;
                                                        if (name == null) name = "Otros";
                                                        totals.put(name, totals.getOrDefault(name, 0) + q);
                                                    }
                                                } else {
                                                    totals.put("Otros", totals.getOrDefault("Otros", 0) + q);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, Integer> e : totals.entrySet()) {
            data.add(new HashMap<>(Map.of("category", e.getKey(), "unidades", e.getValue())));
        }
        data.sort((a,b)->((Comparable)b.get("unidades")).compareTo(a.get("unidades")));
        Map<String, Object> resp = new HashMap<>();
        resp.put("data", data); // Fixed recursive call
        resp.put("chartBase64", null);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/product-events-timeline")
    public ResponseEntity<Map<String, Object>> getProductEventsTimeline(
            @RequestParam(required = false) Integer productCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        OffsetDateTime start = startDate != null ? startDate.atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime end = endDate != null ? endDate.atOffset(ZoneOffset.UTC) : null;
        List<ar.edu.uade.analytics.Entity.ConsumedEventLog> logs;
        if (start != null && end != null) {
            logs = consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseAndProcessedAtBetweenOrderByProcessedAtAsc(
                    ar.edu.uade.analytics.Entity.ConsumedEventLog.Status.PROCESSED, "Compra", start, end);
        } else {
            logs = consumedEventLogRepository.findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(
                    ar.edu.uade.analytics.Entity.ConsumedEventLog.Status.PROCESSED, "Compra");
        }
        if (logs == null) logs = Collections.emptyList(); // Changed to Collections.emptyList()
        List<Map<String, Object>> data = new ArrayList<>();
        for (ar.edu.uade.analytics.Entity.ConsumedEventLog log : logs) {
            try {
                Map<?,?> root = objectMapper.readValue(log.getPayloadJson(), Map.class);
                String type = log.getEventType();
                String when = log.getProcessedAt() != null ? log.getProcessedAt().toString() : null;
                boolean include = (productCode == null);
                Object payload = root.get("payload");
                if (payload instanceof Map<?,?> payloadMap) {
                    Object cart = payloadMap.get("cart");
                    if (cart instanceof Map<?,?> cartMap) {
                        Object items = cartMap.get("items");
                        if (items instanceof List<?> list) {
                            for (Object o : list) {
                                if (o instanceof Map<?,?> m) {
                                    Object pc = m.get("productCode");
                                    if (productCode != null && pc instanceof Number && ((Number) pc).intValue() == productCode) {
                                        include = true; break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (include) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("eventType", type);
                    row.put("processedAt", when);
                    Object ts = root.get("timestamp");
                    if (ts instanceof Number) {
                        long seconds = ((Number) ts).longValue();
                        row.put("emittedAt", java.time.Instant.ofEpochSecond(seconds).toString());
                    }
                    data.add(row);
                }
            } catch (Exception ignored) {}
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("data", data);
        resp.put("chartBase64", null);
        return ResponseEntity.ok(resp);
    }
}
