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
            private final long views;

            public ProductViewStats(String productCode, long views, Product product) {
                this.productCode = productCode;
                this.views = views;
            }

            public String getProductCode() {
                return productCode;
            }

            public long getViews() {
                return views;
            }
        }

        // Helper: filtra por rango de fechas; por defecto, el día actual
        private List<View> findViewsInRange(LocalDateTime from, LocalDateTime to) {
            LocalDateTime start;
            LocalDateTime end;

            if (from == null && to == null) {
                LocalDate today = LocalDate.now();
                start = today.atStartOfDay();
                end = today.atTime(LocalTime.MAX);
            } else if (from != null && to != null) {
                start = from;
                end = to;
            } else if (from != null) {
                start = from.toLocalDate().atStartOfDay();
                end = from.toLocalDate().atTime(LocalTime.MAX);
            } else {
                start = to.toLocalDate().atStartOfDay();
                end = to;
            }

            return viewRepository.findAll().stream()
                    .filter(v -> v.getViewedAt() != null && !v.getViewedAt().isBefore(start) && !v.getViewedAt().isAfter(end))
                    .collect(Collectors.toList());
        }

        // Helper: agrupa cantidad de vistas por productCode
        private Map<String, Long> aggregateByProductCode(List<View> views) {
            return views.stream()
                    .map(v -> {
                        String code = String.valueOf(v.getProductCode());
                        if (code == null && v.getProduct() != null) {
                            code = String.valueOf(v.getProduct().getProductCode());
                        }
                        return code;
                    })
                    .filter(code -> code != null)
                    .collect(Collectors.groupingBy(code -> code, Collectors.counting()));
        }

       // GET: top 10 productos más vistos en el rango
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
                           .map(e -> new ProductViewStats(e.getKey(), e.getValue(), productMap.get(e.getKey())))
                           .collect(Collectors.toList());
               }

               // GET: bottom 10 productos menos vistos en el rango
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
                           .map(e -> new ProductViewStats(e.getKey(), e.getValue(), productMap.get(e.getKey())))
                           .collect(Collectors.toList());
               }

}
