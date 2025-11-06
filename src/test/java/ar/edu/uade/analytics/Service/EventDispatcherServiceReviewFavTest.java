package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Repository.*;
import ar.edu.uade.analytics.TestHelpers.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class EventDispatcherServiceReviewFavTest {
    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock EventRepository eventRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock ViewRepository view_repository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock UserRepository userRepository;
    MeterRegistry meterRegistry = TestUtils.mockMeterRegistryWithCounter();
    @Mock StockChangeLogRepository stockChangeLogRepository;
    @Mock CartRepository cart_repository;

    EventDispatcherService svc;
    ObjectMapper mapper = new ObjectMapper();
    Counter c;

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper, productRepository, brandRepository, categoryRepository,
                eventRepository, reviewRepository, favouriteProductsRepository, view_repository,
                purchaseRepository, userRepository, meterRegistry, stockChangeLogRepository, cart_repository);
        // obtain the shared mock Counter instance from the TestUtils-provided registry
        c = meterRegistry.counter("analytics.sales.review");
    }

    @Test
    void handleReview_createsReview_and_incrementsCounter() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("rating", 4.5);
        payload.put("message", "great");

        svc.handleSales("post: review creada", payload);

        verify(reviewRepository).save(any());
        verify(c).increment();
    }

    @Test
    void handleFavAdd_and_Remove_behaviour() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("productCode", 10);
        Product p = new Product(); p.setId(10); p.setProductCode(10);
        when(productRepository.findByProductCode(10)).thenReturn(p);
        when(favouriteProductsRepository.findAll()).thenReturn(java.util.List.of());

        svc.handleSales("post: producto agregado a favoritos", payload);
        verify(favouriteProductsRepository).save(any());
        verify(meterRegistry).counter("analytics.sales.fav", "op", "add");

        svc.handleSales("delete: producto quitado de favoritos", payload);
        verify(favouriteProductsRepository).findAll();
        verify(meterRegistry).counter("analytics.sales.fav", "op", "remove");
    }
}
