package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Repository.ViewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductViewsTopService {
    private final ViewRepository viewRepository;

    public ProductViewsTopService(ViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    public Map<Integer, Long> countViews(LocalDateTime from, LocalDateTime to) {
        // Normalizar rango: si no hay fechas -> día actual; si invertido -> intercambiar.
        LocalDateTime f = from;
        LocalDateTime t = to;
        if (f == null && t == null) {
            LocalDate today = LocalDate.now();
            f = today.atStartOfDay();
            t = today.atTime(LocalTime.MAX);
        } else if (f != null && t != null && f.isAfter(t)) {
            f = to; t = from; // swap
        }
        return viewRepository.countViewsByProductCode(f, t).stream()
                .collect(Collectors.toMap(ViewRepository.ProductViewsCount::getProductCode,
                        ViewRepository.ProductViewsCount::getTotalViews));
    }
}
