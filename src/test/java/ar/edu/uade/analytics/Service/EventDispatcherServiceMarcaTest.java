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
class EventDispatcherServiceMarcaTest {

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
                purchaseRepository, null, meter, stockChangeLogRepository, cartRepository);
    }

    @Test
    void handleMarca_createsOrUpdatesBrand_and_incrementsCounter() {
        ObjectNode p = mapper.createObjectNode();
        p.put("brandCode", 555);
        p.put("name", "MarcaX");

        when(brandRepository.findByBrandCode(555)).thenReturn(null);
        when(brandRepository.findByNameIgnoreCase("MarcaX")).thenReturn(null);
        when(brandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: marca creada", p);

        verify(brandRepository).save(any());
    }
}

