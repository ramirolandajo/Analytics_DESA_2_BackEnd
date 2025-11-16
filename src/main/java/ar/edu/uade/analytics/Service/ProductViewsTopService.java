package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Repository.ViewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductViewsTopService {
    private final ViewRepository viewRepository;

    public ProductViewsTopService(ViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    public Map<Integer, Long> countViews(LocalDateTime from, LocalDateTime to) {
        // Si from>to, intercambio
        LocalDateTime f = from, t = to;
        if (from != null && to != null && from.isAfter(to)) {
            f = to; t = from;
        }
        return viewRepository.countViewsByProductCode(f, t).stream()
                .collect(Collectors.toMap(ViewRepository.ProductViewsCount::getProductCode,
                        ViewRepository.ProductViewsCount::getTotalViews));
    }
}

