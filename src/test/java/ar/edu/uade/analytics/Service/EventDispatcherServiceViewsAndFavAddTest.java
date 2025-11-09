package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

import java.time.OffsetDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServiceViewsAndFavAddTest {
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
    private CartRepository cart_repository() { return cart_repository; }

    @Test
    void handleVistaDiaria_withArray_and_viewsField_incrementsCounter_and_savesViews() {
        // first test: payload is an array
        ArrayNode arr = mapper.createArrayNode();
        ObjectNode v1 = mapper.createObjectNode(); v1.put("productCode", 100);
        ObjectNode v2 = mapper.createObjectNode(); v2.putNull("productCode");
        arr.add(v1); arr.add(v2);

        when(productRepository.findByProductCode(100)).thenReturn(new Product());
        when(viewRepository.save(any(View.class))).thenAnswer(inv -> inv.getArgument(0));

        svc.handleSales("get: vista diaria de productos", arr, null);

        verify(viewRepository, times(2)).save(any(View.class));
        verify(counter).increment(2);

        // second test: payload has field "views"
        ObjectNode root = mapper.createObjectNode();
        ArrayNode views = mapper.createArrayNode();
        ObjectNode vv = mapper.createObjectNode(); vv.put("productCode", 200); views.add(vv);
        root.set("views", views);
        when(productRepository.findByProductCode(200)).thenReturn(new Product());

        svc.handleSales("get: vista diaria de productos", root, OffsetDateTime.now());
        verify(viewRepository, atLeastOnce()).save(any(View.class));
    }

    @Test
    void handleFavAdd_doesNothing_ifProductMissing_and_savesFavourite_ifProductExists() {
        ObjectNode missing = mapper.createObjectNode(); missing.put("productCode", 9999);
        when(productRepository.findByProductCode(9999)).thenReturn(null);
        svc.handleSales("post: producto agregado a favoritos", missing, null);
        verify(favouriteProductsRepository, never()).save(any());

        Product p = new Product(); p.setId(5); p.setProductCode(500);
        ObjectNode exists = mapper.createObjectNode(); exists.put("productCode", 500);
        when(productRepository.findByProductCode(500)).thenReturn(p);
        when(favouriteProductsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleSales("post: producto agregado a favoritos", exists, null);
        verify(favouriteProductsRepository).save(any(FavouriteProducts.class));
        verify(counter).increment();
    }
}
