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
        // Rango por defecto: toda la tabla (no acotar). Si invertido -> intercambiar.
        LocalDateTime f = from;
        LocalDateTime t = to;
        if (f != null && t != null && f.isAfter(t)) {
            f = to; t = from; // swap
        }
        return viewRepository.countViewsByProductCode(f, t).stream()
                .collect(Collectors.toMap(ViewRepository.ProductViewsCount::getProductCode,
                        ViewRepository.ProductViewsCount::getTotalViews));
    }
}
