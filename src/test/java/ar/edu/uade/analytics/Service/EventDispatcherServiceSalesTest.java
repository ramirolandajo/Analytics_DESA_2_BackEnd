package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Repository.*;
import ar.edu.uade.analytics.TestHelpers.TestUtils;
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
class EventDispatcherServiceSalesTest {
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
    Counter counter;

    EventDispatcherService svc;
    ObjectMapper mapper = new ObjectMapper();

    @Captor ArgumentCaptor<Purchase> purchaseCaptor;

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper,
                productRepository,
                brandRepository,
                categoryRepository,
                eventRepository,
                reviewRepository,
                favouriteProductsRepository,
                view_repository(),
                purchaseRepository,
                userRepository,
                meterRegistry,
                stockChangeLogRepository,
                cart_repository());
        counter = meterRegistry.counter("analytics.sales.views");
    }

    private ViewRepository view_repository(){ return viewRepository; }
    private CartRepository cart_repository(){ return cartRepository; }

    @Test
    void handleCompraConfirmada_createsMissingProduct_and_savesPurchase() {
        ObjectNode p = mapper.createObjectNode();
        ObjectNode user = mapper.createObjectNode(); user.put("email","u@example.com"); user.put("name","U");
        p.set("user", user);
        ObjectNode cart = mapper.createObjectNode(); cart.put("cartId", 1);
        ArrayNode items = mapper.createArrayNode();
        ObjectNode it = mapper.createObjectNode(); it.put("productCode", 777); it.put("quantity", 2); it.put("title","NewProd"); it.put("price", 9.5);
        items.add(it);
        ObjectNode cartObj = mapper.createObjectNode(); cartObj.set("items", items); cartObj.put("finalPrice", 19.0);
        p.set("cart", cartObj);

        when(userRepository.findByEmail("u@example.com")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> { User u = inv.getArgument(0); u.setId(10); return u; });
        when(productRepository.findByProductCode(777)).thenReturn(null);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> { Product pr = inv.getArgument(0); pr.setId(55); return pr; });
        when(productRepository.findByProductCodeIn(anySet())).thenReturn(Collections.emptyList());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> { Cart c = inv.getArgument(0); c.setId(2); return c; });

        svc.handleSales("post: compra confirmada", p, OffsetDateTime.now());

        verify(productRepository, times(2)).save(any(Product.class));
        verify(purchaseRepository).save(purchaseCaptor.capture());
        assertNotNull(purchaseCaptor.getValue());
        assertEquals(Purchase.Status.CONFIRMED, purchaseCaptor.getValue().getStatus());
    }

    @Test
    void handleCompraConfirmada_ignores_whenNoUserInPayload() {
        ObjectNode p = mapper.createObjectNode();
        svc.handleSales("post: compra confirmada", p, null);
        verify(purchaseRepository, never()).save(any());
    }

    @Test
    void handleVistaDiaria_savesViews_forArrayPayload() {
        ArrayNode arr = mapper.createArrayNode();
        ObjectNode v1 = mapper.createObjectNode(); v1.put("productCode", 100);
        ObjectNode v2 = mapper.createObjectNode(); v2.putNull("productCode");
        arr.add(v1); arr.add(v2);
        when(productRepository.findByProductCode(100)).thenReturn(new Product());
        when(viewRepository.save(any(View.class))).thenAnswer(inv -> inv.getArgument(0));

        svc.handleSales("get: vista diaria de productos", arr, null);

        verify(viewRepository, times(2)).save(any(View.class));
        verify(counter).increment(anyDouble());
    }
}
