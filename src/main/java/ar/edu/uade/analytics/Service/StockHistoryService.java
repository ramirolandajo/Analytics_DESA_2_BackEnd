package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.ConsumedEventLog;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.StockChangeLog;
import ar.edu.uade.analytics.Repository.ConsumedEventLogRepository;
import ar.edu.uade.analytics.Repository.StockChangeLogRepository;
import ar.edu.uade.analytics.Repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class StockHistoryService {

    @Autowired
    private PurchaseService purchaseService;
    @Autowired
    private StockChangeLogRepository stockChangeLogRepository;
    @Autowired
    private ConsumedEventLogRepository consumedEventLogRepository;
    @Autowired
    private ObjectMapper objectMapper;

    public Map<String, Object> getStockHistoryByProductCode(Integer productCode,
                                                            boolean showProfit,
                                                            LocalDateTime startDate,
                                                            LocalDateTime endDate) {
        ProductRepository pr = purchaseService.getProductRepository();
        if (pr == null) {
            return Map.of("error", "Product repository not available");
        }
        Product product = pr.findByProductCode(productCode);
        if (product == null) {
            return Map.of("error", "Producto no encontrado");
        }

        List<Map<String, Object>> allEvents = new ArrayList<>();
        float profitAccum = 0f;

        // 1. Logs de cambio de stock
        List<StockChangeLog> logs = stockChangeLogRepository.findByProductIdOrderByChangedAtAsc(product.getId());
        for (StockChangeLog log : logs) {
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
                info.put("_eventDateTime", fecha);
                allEvents.add(info);
            }
        }

        // 2. Eventos de modificación de producto (Producto/Product)
        OffsetDateTime start = startDate != null ? startDate.atOffset(java.time.ZoneOffset.UTC) : null;
        OffsetDateTime end = endDate != null ? endDate.atOffset(java.time.ZoneOffset.UTC) : null;
        List<ConsumedEventLog> modLogs = new ArrayList<>();
        if (start != null && end != null) {
            modLogs.addAll(consumedEventLogRepository
                    .findByStatusAndEventTypeContainingIgnoreCaseAndProcessedAtBetweenOrderByProcessedAtAsc(
                            ConsumedEventLog.Status.PROCESSED, "Producto", start, end));
            modLogs.addAll(consumedEventLogRepository
                    .findByStatusAndEventTypeContainingIgnoreCaseAndProcessedAtBetweenOrderByProcessedAtAsc(
                            ConsumedEventLog.Status.PROCESSED, "Product", start, end));
        } else {
            modLogs.addAll(consumedEventLogRepository
                    .findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(
                            ConsumedEventLog.Status.PROCESSED, "Producto"));
            modLogs.addAll(consumedEventLogRepository
                    .findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(
                            ConsumedEventLog.Status.PROCESSED, "Product"));
        }

        for (ConsumedEventLog ev : modLogs) {
            OffsetDateTime processedAt = ev.getProcessedAt();
            if (processedAt == null) continue;
            LocalDateTime fecha = processedAt.toLocalDateTime();
            if ((startDate != null && fecha.isBefore(startDate)) || (endDate != null && fecha.isAfter(endDate))) continue;

            try {
                Map<?, ?> root = objectMapper.readValue(ev.getPayloadJson(), Map.class);
                Object payload = root.get("payload");
                if (!(payload instanceof Map<?, ?> payloadMap)) continue;

                Map<?, ?> prodMap = null;
                Object prodObj = payloadMap.get("product");
                if (prodObj instanceof Map<?, ?>) prodMap = (Map<?, ?>) prodObj;

                Integer code = extractInt(payloadMap.get("productCode"));
                if (code == null && prodMap != null) code = extractInt(prodMap.get("productCode"));
                if (code == null) code = extractInt(payloadMap.get("code"));
                if (code == null && prodMap != null) code = extractInt(prodMap.get("code"));

                Integer pid = extractInt(payloadMap.get("productId"));
                if (pid == null && prodMap != null) pid = extractInt(prodMap.get("id"));

                boolean matches = (code != null && code.equals(productCode)) ||
                        (pid != null && pid.equals(product.getId()));
                if (!matches) continue;

                Integer oldStock = null;
                Integer newStock = null;

                oldStock = firstNonNull(oldStock,
                        extractInt(payloadMap.get("oldStock")),
                        extractInt(payloadMap.get("stockBefore")));
                newStock = firstNonNull(newStock,
                        extractInt(payloadMap.get("newStock")),
                        extractInt(payloadMap.get("stockAfter")),
                        extractInt(payloadMap.get("stock")));

                if (prodMap != null) {
                    oldStock = firstNonNull(oldStock,
                            extractInt(prodMap.get("oldStock")));
                    newStock = firstNonNull(newStock,
                            extractInt(prodMap.get("newStock")),
                            extractInt(prodMap.get("stock")));
                }

                Map<?, ?> before = asMap(payloadMap.get("before"));
                Map<?, ?> after = asMap(payloadMap.get("after"));
                if (before != null) {
                    Object bs = before.get("stock");
                    if (bs == null && before.get("product") instanceof Map<?, ?> bpm) {
                        bs = ((Map<?, ?>) bpm).get("stock");
                    }
                    oldStock = firstNonNull(oldStock, extractInt(bs));
                }
                if (after != null) {
                    Object asv = after.get("stock");
                    if (asv == null && after.get("product") instanceof Map<?, ?> apm) {
                        asv = ((Map<?, ?>) apm).get("stock");
                    }
                    newStock = firstNonNull(newStock, extractInt(asv));
                }

                Object changesObj = payloadMap.get("changes");
                if (changesObj instanceof List<?> list) {
                    for (Object o : list) {
                        if (!(o instanceof Map<?, ?> m)) continue;
                        Object field = m.get("field");
                        if (field != null && String.valueOf(field).equalsIgnoreCase("stock")) {
                            oldStock = firstNonNull(oldStock, extractInt(m.get("oldValue")));
                            newStock = firstNonNull(newStock, extractInt(m.get("newValue")));
                        }
                    }
                }

                Integer quantityChanged = (oldStock != null && newStock != null) ? (newStock - oldStock) : null;

                if (oldStock != null || newStock != null) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("date", fecha.toLocalDate().toString());
                    info.put("oldStock", oldStock);
                    info.put("newStock", newStock);
                    info.put("quantityChanged", quantityChanged);
                    info.put("reason", "Modificación de producto");
                    float profit = 0f;
                    info.put("profit", profit);
                    profitAccum += profit;
                    info.put("profitAccumulated", profitAccum);
                    info.put("_eventDateTime", fecha);
                    allEvents.add(info);
                }
            } catch (Exception ignored) { }
        }

        allEvents.sort((a, b) -> {
            LocalDateTime da = (LocalDateTime) a.get("_eventDateTime");
            LocalDateTime db = (LocalDateTime) b.get("_eventDateTime");
            return da.compareTo(db);
        });
        for (Map<String, Object> m : allEvents) {
            m.remove("_eventDateTime");
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("data", allEvents);
        resp.put("chartBase64", null);
        return resp;
    }

    private Integer extractInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof String s) {
            try { return Integer.parseInt(s); } catch (Exception ignored) {}
        }
        return null;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... vals) {
        for (T v : vals) if (v != null) return v;
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> asMap(Object o) {
        return (o instanceof Map<?, ?>) ? (Map<?, ?>) o : null;
    }
}