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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventDispatcherServiceCategoriaTest {

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
    @Mock CartRepository cartRepository;

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper, productRepository, brandRepository, categoryRepository,
                eventRepository, reviewRepository, favouriteProductsRepository, viewRepository,
                purchaseRepository, null, meter, stockChangeLogRepository, cart_repository_placeholder());
    }

    // helper to satisfy constructor when real repo not needed
    private CartRepository cart_repository_placeholder() { return cart_repository_mock(); }
    private CartRepository cart_repository_mock() { return mock(CartRepository.class); }

    @Test
    void handleCategoria_createsWhenMissing() {
        ObjectNode p = mapper.createObjectNode();
        p.put("name", "NuevaCat");
        when(categoryRepository.findByNameIgnoreCase("NuevaCat")).thenReturn(null);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: categoría creada", p);

        verify(categoryRepository).save(any());
    }
}

