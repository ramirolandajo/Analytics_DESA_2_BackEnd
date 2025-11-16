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

import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

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
    @JsonPropertyOrder({"productCode", "product", "quantity"})
    public static class ProductViewStats {
        private final Integer productCode; // numérico
        private final Product product; // objeto completo
        private final long quantity; // cantidad de vistas

        public ProductViewStats(Integer productCode, Product product, long quantity) {
            this.productCode = productCode;
            this.product = product;
            this.quantity = quantity;
        }

        public Integer getProductCode() { return productCode; }
        public Product getProduct() { return product; }
        @JsonProperty("quantity")
        public long getQuantity() { return quantity; }
        // Alias por compatibilidad
        @JsonProperty("views")
        public long getViews() { return quantity; }
    }

    private Map<Integer, Long> calculateViewCounts(LocalDateTime from, LocalDateTime to) {
        LocalDateTime normalizedFrom = from;
        LocalDateTime normalizedTo = to;
        if (from != null && to != null && from.isAfter(to)) {
            normalizedFrom = to;
            normalizedTo = from;
        }
        return viewRepository.countViewsByProductCode(normalizedFrom, normalizedTo).stream()
                .collect(Collectors.toMap(ViewRepository.ProductViewsCount::getProductCode,
                        ViewRepository.ProductViewsCount::getTotalViews));
    }

    private Map<Integer, Product> buildProductMap() {
        return productRepository.findAll().stream()
                .filter(p -> p.getProductCode() != null)
                .collect(Collectors.toMap(Product::getProductCode, p -> p, (a, b) -> a));
    }

    // GET: top 10 productos más vistos en el rango (o total)
    @Transactional(readOnly = true, timeout = 60)
    @GetMapping("/daily/top")
    public List<ProductViewStats> getTop10ProductViews(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        Map<Integer, Long> counts = calculateViewCounts(from, to);
        if (counts.isEmpty()) {
            return List.of();
        }

        Map<Integer, Product> productMap = buildProductMap();

        return counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(10)
                .map(e -> new ProductViewStats(e.getKey(), productMap.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    // GET: bottom 10 productos menos vistos en el rango (solo desde tabla view)
    @Transactional(readOnly = true, timeout = 60)
    @GetMapping("/daily/bottom")
    public List<ProductViewStats> getBottom10ProductViews(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        Map<Integer, Long> counts = calculateViewCounts(from, to);
        if (counts.isEmpty()) {
            return List.of();
        }

        Map<Integer, Product> productMap = buildProductMap();

        return counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(10)
                .map(e -> new ProductViewStats(e.getKey(), productMap.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

}
