package ar.edu.uade.analytics.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.uade.analytics.Repository.ProductRepository;
import ar.edu.uade.analytics.Repository.BrandRepository;
import ar.edu.uade.analytics.Repository.EventRepository;
import ar.edu.uade.analytics.Repository.ReviewRepository;
import ar.edu.uade.analytics.Repository.FavouriteProductsRepository;
import ar.edu.uade.analytics.Repository.ViewRepository;
import ar.edu.uade.analytics.Repository.PurchaseRepository;
import ar.edu.uade.analytics.Repository.StockChangeLogRepository;
import ar.edu.uade.analytics.Repository.CartRepository;

@ExtendWith(MockitoExtension.class)
class EventDispatcherServiceUnknownTypeTest {

    private EventDispatcherService svc;
    private final ObjectMapper mapper = new ObjectMapper();
    private final SimpleMeterRegistry meter = new SimpleMeterRegistry();

    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock EventRepository eventRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock ViewRepository viewRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock StockChangeLogRepository stockChangeLogRepository;
    @Mock CartRepository cartRepository;

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper, productRepository, brandRepository, null,
                eventRepository, reviewRepository, favouriteProductsRepository, viewRepository,
                purchaseRepository, null, meter, stockChangeLogRepository, cartRepository);
    }

    @Test
    void handleUnknown_doesNotThrow() {
        ObjectNode payload = mapper.createObjectNode(); payload.put("x",1);
        // Should not throw
        svc.handleInventory("some: unknown event", payload);
        svc.handleSales("some: unknown event", payload);
    }
}
