package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Repository.*;
import ar.edu.uade.analytics.TestHelpers.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServiceUpsertCategoriesByIdTest {
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
    @Mock CartRepository cartRepository;

    EventDispatcherService svc;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper,
                productRepository,
                brandRepository,
                categoryRepository,
                eventRepository,
                review_repository(),
                favouriteProductsRepository,
                viewRepository,
                purchaseRepository,
                userRepository,
                meterRegistry,
                stockChangeLogRepository,
                cart_repository());
    }

    private ReviewRepository review_repository() { return reviewRepository; }
    private CartRepository cart_repository() { return cartRepository; }

    @Test
    void handleUpsertProducto_resolvesCategoryIds_usingFindByIdIn() {
        ObjectNode p = mapper.createObjectNode();
        p.put("productCode", 2222);
        ArrayNode categories = mapper.createArrayNode(); categories.add(7); categories.add(8);
        p.set("categories", categories);

        when(productRepository.findByProductCode(2222)).thenReturn(null);
        when(categoryRepository.findByCategoryCodeIn(anyList())).thenReturn(List.of());
        when(categoryRepository.findByIdIn(List.of(7,8))).thenReturn(List.of(new Category(), new Category()));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: agregar un producto", p);

        verify(categoryRepository).findByIdIn(List.of(7,8));
        verify(productRepository).save(any(Product.class));
    }
}
