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

@RestController
@RequestMapping("/api/product-views")
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
}
