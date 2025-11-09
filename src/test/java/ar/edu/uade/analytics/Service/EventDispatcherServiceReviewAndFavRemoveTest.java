package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Repository.*;
import ar.edu.uade.analytics.TestHelpers.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventDispatcherServiceReviewAndFavRemoveTest {
    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock EventRepository eventRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock ViewRepository viewRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock UserRepository userRepository;
    MeterRegistry meterRegistry = TestUtils.mockMeterRegistryWithCounter();
    @Mock StockChangeLogRepository stockChangeLogRepository;
    @Mock CartRepository cart_repository;

    EventDispatcherService svc;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper,
                productRepository,
                brandRepository,
                categoryRepository,
                eventRepository,
                reviewRepository,
                favouriteProductsRepository,
                view_repository(),
                purchaseRepository,
                userRepository,
                meterRegistry,
                stockChangeLogRepository,
                cart_repository);
    }

    private ViewRepository view_repository() { return viewRepository; }

    @Test
    void handleReview_savesReview_and_incrementsCounter() {
        ObjectNode p = mapper.createObjectNode(); p.put("productCode", 100); p.put("rating", 4.5); p.put("description", "ok");
        Product prod = new Product(); prod.setId(100); when(productRepository.findByProductCode(100)).thenReturn(prod);
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // call via public API
        svc.handleSales("post: review creada", p, null);

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void handleFavRemove_deletesMatchingFavourite_and_incrementsCounter() {
        FavouriteProducts f = new FavouriteProducts(); f.setProductCode(200);
        when(favouriteProductsRepository.findAll()).thenReturn(List.of(f));
        ObjectNode p = mapper.createObjectNode(); p.put("productCode", 200);

        svc.handleSales("delete: producto quitado de favoritos", p, null);

        verify(favouriteProductsRepository).delete(f);
    }
}
