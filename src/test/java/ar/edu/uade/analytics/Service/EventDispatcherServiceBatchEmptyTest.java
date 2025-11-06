package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServiceBatchEmptyTest {
    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock EventRepository eventRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock ViewRepository viewRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock UserRepository userRepository;
    @Mock MeterRegistry meterRegistry;
    @Mock StockChangeLogRepository stockChangeLogRepository;
    @Mock CartRepository cart_repository;
    @Mock Counter counter;

    EventDispatcherService svc;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        svc = new EventDispatcherService(mapper,
                productRepository,
                brandRepository,
                categoryRepository,
                event_repository(),
                review_repository(),
                favouriteProductsRepository,
                view_repository(),
                purchaseRepository,
                userRepository,
                meterRegistry,
                stockChangeLogRepository,
                cart_repository());
    }

    private EventRepository event_repository() { return eventRepository; }
    private ReviewRepository review_repository() { return reviewRepository; }
    private ViewRepository view_repository() { return viewRepository; }
    private CartRepository cart_repository() { return cart_repository; }

    @Test
    void handleBatchProductos_withNoItems_doesNothing() {
        ObjectNode root = mapper.createObjectNode();
        // no items field
        svc.handleInventory("post: agregar productos (batch)", root);
        // verify no saves were called
        verify(productRepository, never()).save(any());
        verify(stockChangeLogRepository, never()).save(any());
        verify(counter, never()).increment();
    }
}

