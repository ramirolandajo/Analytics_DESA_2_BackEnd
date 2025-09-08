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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
        // No chart generation: return only metrics
        String chartBase64 = null;

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
        // Removed heavy chart creation - keep only the histogram metrics
        histogram.put("chartBase64", null);
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
        // Removed chart generation - keep correlation data only
        correlation.put("chartBase64", null);
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
        // Removed boxplot creation - keep values and mark chart as not generated
        outliers.put("chartBase64", null);
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
        // Removed chart generation - only return null counts
        nulls.put("chartBase64", null);
        return ResponseEntity.ok(nulls);
    }
}
