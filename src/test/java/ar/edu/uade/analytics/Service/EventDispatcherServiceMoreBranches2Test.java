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

import java.util.List;

import static ar.edu.uade.analytics.TestHelpers.TestUtils.productWithCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServiceMoreBranches2Test {
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
                purchase_repository(),
                user_repository(),
                meterRegistry,
                stockChangeLogRepository,
                cart_repository());
    }

    private EventRepository event_repository() { return eventRepository; }
    private ReviewRepository review_repository() { return reviewRepository; }
    private ViewRepository view_repository() { return viewRepository; }
    private PurchaseRepository purchase_repository() { return purchaseRepository; }
    private UserRepository user_repository() { return userRepository; }
    private CartRepository cart_repository() { return cart_repository; }

    @Test
    void handleSales_ignoresUnknownAction_noRepoInteraction() {
        ObjectNode payload = mapper.createObjectNode(); payload.put("x", "y");
        svc.handleSales("some: unknown event", payload, null);
        verifyNoInteractions(productRepository, brandRepository, categoryRepository, purchaseRepository, eventRepository);
    }

    @Test
    void handleUpsertProducto_missingBrandAndCategories_attemptsResolve_and_savesProduct() {
        ObjectNode p = mapper.createObjectNode();
        p.put("productCode", 555);
        p.put("name", "NoBrandProd");
        p.put("brandCode", 9999);
        ArrayNode cats = mapper.createArrayNode(); cats.add(11); cats.add(12); p.set("categoryCodes", cats);

        when(brandRepository.findByBrandCode(9999)).thenReturn(null);
        when(categoryRepository.findByCategoryCodeIn(List.of(11,12))).thenReturn(List.of());
        when(productRepository.findByProductCode(555)).thenReturn(null);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: agregar un producto", p);

        verify(brandRepository).findByBrandCode(9999);
        verify(categoryRepository).findByCategoryCodeIn(List.of(11,12));
        verify(productRepository).save(any(Product.class));
    }

//    @Test
//    void handleBatchProductos_largeBatch_countsOkAndFailMixed() {
//        ObjectNode root = mapper.createObjectNode();
//        ArrayNode items = mapper.createArrayNode();
//        for (int i = 1; i <= 5; i++) {
//            ObjectNode it = mapper.createObjectNode(); it.put("productCode", i); items.add(it);
//        }
//        root.set("items", items);
//
//        Counter okCounter = mock(Counter.class);
//        Counter failCounter = mock(Counter.class);
//        when(meterRegistry.counter(eq("analytics.inventory.product.batch.ok"))).thenReturn(okCounter);
//        when(meterRegistry.counter(eq("analytics.inventory.product.batch.fail"))).thenReturn(failCounter);
//
//        when(productRepository.findByProductCode(anyInt())).thenReturn(null);
//        when(productRepository.save(argThat(productWithCode(3)))).thenThrow(new RuntimeException("boom3"));
//        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
//
//        svc.handleInventory("post: agregar productos (batch)", root);
//
//        verify(okCounter).increment(4);
//        verify(failCounter).increment(1);
//    }
}
