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
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServiceCompraMixedProductsTest {
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

    @Captor ArgumentCaptor<Product> productCaptor;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        svc = new EventDispatcherService(mapper,
                productRepository,
                brandRepository,
                category_repository(),
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

    private CategoryRepository category_repository() { return categoryRepository; }
    private EventRepository event_repository() { return eventRepository; }
    private ReviewRepository review_repository() { return reviewRepository; }
    private ViewRepository view_repository() { return viewRepository; }
    private PurchaseRepository purchase_repository() { return purchaseRepository; }
    private UserRepository user_repository() { return userRepository; }
    private CartRepository cart_repository() { return cart_repository; }

    @Test
    void handleCompraConfirmada_withExistingAndMissingProducts_savesNewProduct_and_updatesStockAndLogs() {
        ObjectNode p = mapper.createObjectNode();
        ObjectNode user = mapper.createObjectNode(); user.put("email","umix@example.com"); user.put("name","Mx"); p.set("user", user);
        ArrayNode items = mapper.createArrayNode();
        ObjectNode it1 = mapper.createObjectNode(); it1.put("productCode", 400); it1.put("quantity", 1); items.add(it1);
        ObjectNode it2 = mapper.createObjectNode(); it2.put("productCode", 401); it2.put("quantity", 2); it2.put("title","NewProd"); items.add(it2);
        ObjectNode cart = mapper.createObjectNode(); cart.set("items", items); p.set("cart", cart);

        User u = new User(); u.setId(77); u.setEmail("umix@example.com");
        when(userRepository.findByEmail("umix@example.com")).thenReturn(u);

        Product prodExisting = new Product(); prodExisting.setId(400); prodExisting.setProductCode(400); prodExisting.setStock(10);
        when(productRepository.findByProductCode(400)).thenReturn(prodExisting);
        when(productRepository.findByProductCode(401)).thenReturn(null);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockChangeLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cart_repository.save(any())).thenAnswer(inv -> { Cart c = inv.getArgument(0); c.setId(123); return c; });

        svc.handleSales("post: compra confirmada", p, OffsetDateTime.now());

        // verify that productRepository.save was called at least for the missing product (401) and for stock updates
        verify(productRepository, atLeast(1)).save(productCaptor.capture());
        boolean hadNew = productCaptor.getAllValues().stream().anyMatch(pr -> pr.getProductCode()!=null && pr.getProductCode()==401);
        assertTrue(hadNew);
        verify(stockChangeLogRepository, atLeastOnce()).save(any());
        verify(purchaseRepository).save(any());
    }
}

