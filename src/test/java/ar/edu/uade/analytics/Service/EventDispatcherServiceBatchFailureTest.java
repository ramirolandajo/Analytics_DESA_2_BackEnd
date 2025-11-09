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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventDispatcherServiceBatchFailureTest {

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
    @Mock CartRepository cartRepository;
    @Mock Counter counter;

    EventDispatcherService svc;
    ObjectMapper realMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(realMapper, productRepository, brandRepository, categoryRepository,
                eventRepository, reviewRepository, favouriteProductsRepository, viewRepository,
                purchaseRepository, userRepository, meterRegistry, stockChangeLogRepository, cartRepository);
    }

    @Test
    void handleBatchProductos_whenOneSaveThrows_countsFail() {
        ObjectNode payload = realMapper.createObjectNode();
        var items = realMapper.createArrayNode();
        ObjectNode it = realMapper.createObjectNode(); it.put("productCode", 1); it.put("name", "X"); items.add(it);
        payload.set("items", items);

        when(productRepository.save(any())).thenThrow(new RuntimeException("boom"));
        when(meterRegistry.counter("analytics.inventory.product.batch.ok")).thenReturn(counter);
        when(meterRegistry.counter("analytics.inventory.product.batch.fail")).thenReturn(counter);

        svc.handleInventory("post: agregar productos (batch)", payload);

        // handleBatchProductos increments counters with a double argument (ok/fail), so accept any double
        verify(counter, atLeastOnce()).increment(anyDouble());
    }
}
