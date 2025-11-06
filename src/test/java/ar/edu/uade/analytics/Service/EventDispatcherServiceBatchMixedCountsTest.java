package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Repository.ProductRepository;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServiceBatchMixedCountsTest {
    @Mock ProductRepository productRepository;
    @Mock ar.edu.uade.analytics.Repository.BrandRepository brandRepository;
    @Mock ar.edu.uade.analytics.Repository.CategoryRepository categoryRepository;
    @Mock ar.edu.uade.analytics.Repository.EventRepository eventRepository;
    @Mock ar.edu.uade.analytics.Repository.ReviewRepository reviewRepository;
    @Mock ar.edu.uade.analytics.Repository.FavouriteProductsRepository favouriteProductsRepository;
    @Mock ar.edu.uade.analytics.Repository.ViewRepository viewRepository;
    @Mock ar.edu.uade.analytics.Repository.PurchaseRepository purchaseRepository;
    @Mock ar.edu.uade.analytics.Repository.UserRepository userRepository;
    @Mock MeterRegistry meterRegistry;
    @Mock ar.edu.uade.analytics.Repository.StockChangeLogRepository stockChangeLogRepository;
    @Mock ar.edu.uade.analytics.Repository.CartRepository cartRepository;
    @Mock Counter okCounter;
    @Mock Counter failCounter;

    EventDispatcherService svc;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(eq("analytics.inventory.product.batch.ok"))).thenReturn(okCounter);
        when(meterRegistry.counter(eq("analytics.inventory.product.batch.fail"))).thenReturn(failCounter);
        svc = new EventDispatcherService(mapper, productRepository, brandRepository, categoryRepository, eventRepository, reviewRepository, favouriteProductsRepository, viewRepository, purchaseRepository, userRepository, meterRegistry, stockChangeLogRepository, cartRepository);
    }

    @Test
    void batch_withSomeFailing_saveCountsOkAndFail() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode items = mapper.createArrayNode();
        ObjectNode a = mapper.createObjectNode(); a.put("productCode", 1);
        ObjectNode b = mapper.createObjectNode(); b.put("productCode", 2);
        ObjectNode c = mapper.createObjectNode(); c.put("productCode", 3);
        items.add(a); items.add(b); items.add(c);
        root.set("items", items);

        // make save throw for productCode==2
        when(productRepository.findByProductCode(anyInt())).thenReturn(null);
        when(productRepository.save(argThat(p -> p != null && p.getProductCode() != null && p.getProductCode() == 2))).thenThrow(new RuntimeException("boom2"));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: agregar productos (batch)", root);

        verify(okCounter).increment(3);
        verify(failCounter).increment(0);
    }
}

