package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Brand;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServiceMarcaUpdateTest {
    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock CategoryRepository category_repository;
    @Mock EventRepository eventRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock ViewRepository viewRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock UserRepository userRepository;
    MeterRegistry meterRegistry = TestUtils.mockMeterRegistryWithCounter();
    @Mock StockChangeLogRepository stockChangeLogRepository;
    @Mock CartRepository cartRepository;

    EventDispatcherService svc;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper,
                productRepository,
                brandRepository,
                category_repository,
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
    private CartRepository cart_repository() { return cartRepository; }

//    @Test
//    void handleMarca_updatesExistingBrand_and_incrementsCounter() {
//        ObjectNode p = mapper.createObjectNode();
//        p.put("brandCode", 10);
//        p.put("name", "MarcaOld");
//
//        Brand existing = new Brand(); existing.setId(100); existing.setBrandCode(10); existing.setName("Old");
//        when(brandRepository.findByBrandCode(10)).thenReturn(existing);
//        when(brandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
//
//        svc.handleInventory("patch: marca activada", p);
//
//        verify(brandRepository).save(argThat(b -> b.getBrandCode()!=null && b.getBrandCode().equals(10) && b.getId()!=null));
//        verify(meterRegistry.get("analytics.inventory.brand.upsert").counter(), atLeastOnce()).increment();
//    }
}
