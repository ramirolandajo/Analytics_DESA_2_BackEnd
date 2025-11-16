package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Repository.ProductRepository;
import ar.edu.uade.analytics.Repository.ViewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public Map<Integer, Long> countViewsIncludingZeros(LocalDateTime from, LocalDateTime to) {
        // Normalizar rango: si from>to, intercambio. Rangos parciales se dejan abiertos.
        LocalDateTime f = from, t = to;
        if (from != null && to != null && from.isAfter(to)) { f = to; t = from; }

        Map<Integer, Long> counts = viewRepository.countViewsByProductCode(f, t).stream()
                .collect(Collectors.toMap(ViewRepository.ProductViewsCount::getProductCode,
                        ViewRepository.ProductViewsCount::getTotalViews));

        // Completar con 0 para productos existentes sin vistas en el rango
        for (Product p : productRepository.findAll()) {
            Integer code = p.getProductCode();
            if (code != null) counts.putIfAbsent(code, 0L);
        }
        // Devolver ordenado ascendente
        return counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->a, LinkedHashMap::new));
    }
}

