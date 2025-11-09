package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventDispatcherServicePurchaseTest {

    // no mapper mock needed
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

    EventDispatcherService svc;
    ObjectMapper realMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(realMapper, productRepository, brandRepository, categoryRepository,
                eventRepository, reviewRepository, favouriteProductsRepository, viewRepository,
                purchaseRepository, userRepository, meterRegistry, stockChangeLogRepository, cartRepository);
        // ensure counters are present to avoid NPE
        when(meterRegistry.counter(anyString())).thenReturn(mock(io.micrometer.core.instrument.Counter.class));
    }

    @Test
    void handleCompraConfirmada_createsUser_and_savesPurchase() {
        ObjectNode payload = realMapper.createObjectNode();
        ObjectNode user = realMapper.createObjectNode(); user.put("email", "x@y.com"); user.put("name", "X");
        ObjectNode cart = realMapper.createObjectNode(); cart.put("cartId", 5); cart.put("finalPrice", 50);
        payload.set("user", user); payload.set("cart", cart);

        when(userRepository.findByEmail("x@y.com")).thenReturn(null);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(33);
            return u;
        });

        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleSales("post: compra confirmada", payload);

        verify(userRepository).save(any());
        verify(purchaseRepository).save(any());
    }
}
