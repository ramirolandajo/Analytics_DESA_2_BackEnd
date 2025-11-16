package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Repository.ProductRepository;
import ar.edu.uade.analytics.Repository.ViewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductViewsBottomService {
    private final ViewRepository viewRepository;
    private final ProductRepository productRepository;

    public ProductViewsBottomService(ViewRepository viewRepository, ProductRepository productRepository) {
        this.viewRepository = viewRepository;
        this.productRepository = productRepository;
    }

    private LocalDateTime[] normalize(LocalDateTime from, LocalDateTime to) {
        LocalDateTime f = from;
        LocalDateTime t = to;
        if (f == null && t == null) {
            LocalDate today = LocalDate.now();
            f = today.atStartOfDay();
            t = today.atTime(LocalTime.MAX);
        } else if (f != null && t != null && f.isAfter(t)) {
            f = to; t = from; // swap
        }
        return new LocalDateTime[]{f, t};
    }

    // Incluye ceros (productos nunca vistos en el rango)
    public Map<Integer, Long> countViewsIncludingZeros(LocalDateTime from, LocalDateTime to) {
        LocalDateTime[] norm = normalize(from, to);
        LocalDateTime f = norm[0], t = norm[1];

        Map<Integer, Long> counts = viewRepository.countViewsByProductCode(f, t).stream()
                .collect(Collectors.toMap(ViewRepository.ProductViewsCount::getProductCode,
                        ViewRepository.ProductViewsCount::getTotalViews));

        for (Product p : productRepository.findAll()) {
            Integer code = p.getProductCode();
            if (code != null) counts.putIfAbsent(code, 0L);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->a, LinkedHashMap::new));
    }

    // Solo productos con vistas (>0) en el rango
    public Map<Integer, Long> countViewsExcludingZeros(LocalDateTime from, LocalDateTime to) {
        LocalDateTime[] norm = normalize(from, to);
        LocalDateTime f = norm[0], t = norm[1];

        return viewRepository.countViewsByProductCode(f, t).stream()
                .filter(v -> v.getTotalViews() != null && v.getTotalViews() > 0)
                .sorted((a,b) -> {
                    int cmp = Long.compare(a.getTotalViews(), b.getTotalViews());
                    if (cmp != 0) return cmp;
                    return Integer.compare(a.getProductCode(), b.getProductCode());
                })
                .collect(Collectors.toMap(ViewRepository.ProductViewsCount::getProductCode,
                        ViewRepository.ProductViewsCount::getTotalViews,
                        (a,b)->a,
                        LinkedHashMap::new));
    }
}
