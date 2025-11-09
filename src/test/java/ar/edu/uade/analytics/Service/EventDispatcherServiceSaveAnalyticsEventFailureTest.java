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
class EventDispatcherServiceSaveAnalyticsEventFailureTest {
    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock EventRepository eventRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock ViewRepository view_repository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock UserRepository userRepository;
    @Mock MeterRegistry meterRegistry;
    @Mock StockChangeLogRepository stockChangeLogRepository;
    @Mock CartRepository cart_repository;
    @Mock Counter counter;
    @Mock ObjectMapper mapper;

    EventDispatcherService svc;

    @BeforeEach
    void setUp() throws Exception {
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        // mapper will throw when serializing
        doThrow(new RuntimeException("boom serializing")).when(mapper).writeValueAsString(any());

        svc = new EventDispatcherService(mapper,
                productRepository,
                brandRepository,
                categoryRepository,
                eventRepository,
                review_repository(),
                favouriteProductsRepository,
                view_repository(),
                purchaseRepository,
                userRepository,
                meterRegistry,
                stockChangeLogRepository,
                cart_repository());
    }

    private ReviewRepository review_repository() { return reviewRepository; }
    private ViewRepository view_repository() { return view_repository; }
    private CartRepository cart_repository() { return cart_repository; }

    @Test
    void saveAnalyticsEvent_handlesMapperException_and_doesNotPersistOrIncrement() {
        // build payload using a real ObjectMapper/node factory to avoid relying on the mocked mapper internals
        ObjectMapper real = new ObjectMapper();
        ObjectNode payload = real.createObjectNode();
        payload.put("x", "y");

        // call saveAnalyticsEvent via handleSales route
        svc.handleSales("post: compra pendiente", payload, null);

        // eventRepository.save should not be called due to exception in mapper.writeValueAsString
        verify(eventRepository, never()).save(any());
        // counter should not be incremented for the event persistence metric
        verify(meterRegistry, never()).counter(eq("analytics.sales.event"), any(String[].class));
    }
}
