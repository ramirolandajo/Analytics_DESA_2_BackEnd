package ar.edu.uade.analytics.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.uade.analytics.Repository.*;
import ar.edu.uade.analytics.Entity.Product;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EventDispatcherServiceActualizarStockTest {

    private EventDispatcherService svc;
    private final ObjectMapper mapper = new ObjectMapper();
    private final SimpleMeterRegistry meter = new SimpleMeterRegistry();

    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock EventRepository eventRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock ViewRepository viewRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock StockChangeLogRepository stockChangeLogRepository;
    @Mock CartRepository cart_repository;

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper, productRepository, brandRepository, categoryRepository,
                eventRepository, reviewRepository, favouriteProductsRepository, view_repository_placeholder(),
                purchaseRepository, null, meter, stockChangeLogRepository, cart_repository_placeholder());
    }

    private ViewRepository view_repository_placeholder() { return mock(ViewRepository.class); }
    private CartRepository cart_repository_placeholder() { return mock(CartRepository.class); }

    @Test
    void handleActualizarStock_createsProd_whenMissing_and_logs() {
        ObjectNode p = mapper.createObjectNode();
        p.put("productCode", 777);
        p.put("stock", 5);
        when(productRepository.findStockByProductCode(777)).thenReturn(null);
        when(productRepository.findByProductCode(777)).thenReturn(null);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockChangeLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("put: actualizar stock", p);

        verify(productRepository).save(any());
        verify(stockChangeLogRepository).save(any());
    }
}

