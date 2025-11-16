package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Communication.KafkaMockService;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.View;
import ar.edu.uade.analytics.Repository.ProductRepository;
import ar.edu.uade.analytics.Repository.ViewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/product-views")
public class ProductViewController {
    @Autowired
    KafkaMockService kafkaMockServiceSync;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    ViewRepository viewRepository;


    // GET: Sincronizar vistas de productos (mock)
    @Transactional(timeout = 60)
    @GetMapping("/sync")
    public List<View> syncProductViews() {
        KafkaMockService.DailyProductViewsMessage event = kafkaMockServiceSync.getDailyProductViewsMock();
        String timestamp = event.timestamp;
        LocalDateTime viewedAt = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        List<View> savedViews = new ArrayList<>();
        for (KafkaMockService.ProductViewDTO dto : event.payload.products) {
            Product product = productRepository.findAll().stream()
                .filter(p -> p.getProductCode() != null && p.getProductCode().equals(dto.productCode))
                .findFirst().orElse(null);
            if (product != null) {
                View view = new View();
                view.setProduct(product);
                view.setViewedAt(viewedAt);
                view.setProductCode(product.getProductCode());
                savedViews.add(viewRepository.save(view));
            }
        }
        return savedViews;
    }


    // DTO de respuesta para stats
    public static class ProductViewStats {
        private final String productCode;
        private final Product product; // objeto completo
        private final long quantity; // cantidad de vistas

        public ProductViewStats(String productCode, Product product, long quantity) {
            this.productCode = productCode;
            this.product = product;
            this.quantity = quantity;
        }

        public String getProductCode() {
            return productCode;
        }

        public Product getProduct() {
            return product;
        }

        public long getQuantity() {
            return quantity;
        }
    }

    // Helper: filtra por rango de fechas; por defecto, TODA la tabla
    private List<View> findViewsInRange(LocalDateTime from, LocalDateTime to) {
        // Sin fechas: devolver toda la tabla
        if (from == null && to == null) {
            return viewRepository.findAll();
        }

        return viewRepository.findAll().stream()
                .filter(v -> v.getViewedAt() != null)
                .filter(v -> from == null || !v.getViewedAt().isBefore(from)) // v >= from si from está
                .filter(v -> to == null || !v.getViewedAt().isAfter(to))     // v <= to si to está
                .collect(Collectors.toList());
    }

    // Helper: agrupa cantidad de vistas por productCode
    private Map<String, Long> aggregateByProductCode(List<View> views) {
        return views.stream()
                .map(v -> {
                    Integer codeInt = v.getProductCode();
                    if (codeInt == null && v.getProduct() != null) {
                        codeInt = v.getProduct().getProductCode();
                    }
                    return codeInt != null ? String.valueOf(codeInt) : null;
                })
                .filter(code -> code != null)
                .collect(Collectors.groupingBy(code -> code, Collectors.counting()));
    }

    // GET: top 10 productos más vistos en el rango (o total)
    @Transactional(readOnly = true, timeout = 60)
    @GetMapping("/daily/top")
    public List<ProductViewStats> getTop10ProductViews(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<View> views = findViewsInRange(from, to);
        Map<String, Long> counts = aggregateByProductCode(views);

        Map<String, Product> productMap = productRepository.findAll().stream()
                .filter(p -> p.getProductCode() != null)
                .collect(Collectors.toMap(p -> String.valueOf(p.getProductCode()), p -> p, (a, b) -> a));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(e -> new ProductViewStats(e.getKey(), productMap.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    // GET: bottom 10 productos menos vistos en el rango (o total)
    @Transactional(readOnly = true, timeout = 60)
    @GetMapping("/daily/bottom")
    public List<ProductViewStats> getBottom10ProductViews(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<View> views = findViewsInRange(from, to);
        Map<String, Long> counts = aggregateByProductCode(views);

        Map<String, Product> productMap = productRepository.findAll().stream()
                .filter(p -> p.getProductCode() != null)
                .collect(Collectors.toMap(p -> String.valueOf(p.getProductCode()), p -> p, (a, b) -> a));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue())
                .limit(10)
                .map(e -> new ProductViewStats(e.getKey(), productMap.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

}
