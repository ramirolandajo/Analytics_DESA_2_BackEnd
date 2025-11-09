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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServicePurchaseExistingUserTest {
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
    ObjectMapper mapper = new ObjectMapper();

    @Captor ArgumentCaptor<Purchase> purchaseCaptor;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        svc = new EventDispatcherService(mapper,
                productRepository,
                brandRepository,
                categoryRepository,
                eventRepository,
                reviewRepository,
                favouriteProductsRepository,
                viewRepository,
                purchaseRepository,
                userRepository,
                meterRegistry,
                stockChangeLogRepository,
                cart_repository());
    }

    private ViewRepository view_repository() { return viewRepository; }
    private CartRepository cart_repository() { return cartRepository; }

    @Test
    void handleCompraConfirmada_withExistingUser_updatesStocks_and_savesPurchase() {
        ObjectNode p = mapper.createObjectNode();
        ObjectNode user = mapper.createObjectNode(); user.put("email","u2@example.com"); user.put("name","U2");
        p.set("user", user);
        ArrayNode items = mapper.createArrayNode();
        ObjectNode it = mapper.createObjectNode(); it.put("productCode", 1000); it.put("quantity", 2); it.put("title","ProdX"); it.put("price", 50);
        items.add(it);
        ObjectNode cartObj = mapper.createObjectNode(); cartObj.set("items", items); cartObj.put("finalPrice", 100.0);
        p.set("cart", cartObj);

        User existing = new User(); existing.setId(42); existing.setEmail("u2@example.com");
        when(userRepository.findByEmail("u2@example.com")).thenReturn(existing);

        Product prod = new Product(); prod.setId(500); prod.setProductCode(1000); prod.setStock(10);
        when(productRepository.findByProductCode(1000)).thenReturn(prod);
        // Provide CodeStock projection to simulate preloaded stock (anonymous impl)
        ProductRepository.CodeStock cs = new ProductRepository.CodeStock() {
            public Integer getProductCode() { return 1000; }
            public Integer getStock() { return 10; }
        };
        when(productRepository.findByProductCodeIn(anySet())).thenReturn(List.of(cs));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockChangeLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> { Cart c = inv.getArgument(0); c.setId(99); return c; });
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        svc.handleSales("post: compra confirmada", p, OffsetDateTime.now());

        verify(productRepository, atLeastOnce()).save(any(Product.class));
        verify(stockChangeLogRepository, atLeastOnce()).save(any(StockChangeLog.class));
        verify(purchaseRepository).save(purchaseCaptor.capture());
        assertEquals(Purchase.Status.CONFIRMED, purchaseCaptor.getValue().getStatus());
        assertEquals(1, purchaseCaptor.getValue().getCart().getItems().size());
    }
}
