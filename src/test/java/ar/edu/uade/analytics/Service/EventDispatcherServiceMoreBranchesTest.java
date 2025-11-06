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
class EventDispatcherServiceMoreBranchesTest {
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
    void handleCompraConfirmada_ignores_whenNoUserPresent() {
        ObjectNode p = mapper.createObjectNode();
        ObjectNode cart = mapper.createObjectNode();
        ArrayNode items = mapper.createArrayNode();
        ObjectNode it = mapper.createObjectNode(); it.put("productCode", 700); it.put("quantity", 1); items.add(it);
        cart.set("items", items);
        p.set("cart", cart);

        svc.handleSales("post: compra confirmada", p, OffsetDateTime.now());

        verify(userRepository, never()).findByEmail(anyString());
        verify(purchaseRepository, never()).save(any());
    }

//    @Test
//    void handleCompraConfirmada_whenPurchaseSaveThrows_incrementsCounter_and_continues() {
//        ObjectNode p = mapper.createObjectNode();
//        ObjectNode user = mapper.createObjectNode(); user.put("email","err@example.com"); p.set("user", user);
//        ObjectNode cart = mapper.createObjectNode(); ArrayNode items = mapper.createArrayNode(); ObjectNode it = mapper.createObjectNode(); it.put("productCode", 710); it.put("quantity", 1); items.add(it); cart.set("items", items); p.set("cart", cart);
//
//        User u = new User(); u.setId(8); u.setEmail("err@example.com");
//        when(userRepository.findByEmail("err@example.com")).thenReturn(u);
//        when(productRepository.findByProductCode(710)).thenReturn(new Product());
//        // make purchaseRepository.save throw
//        when(purchaseRepository.save(any())).thenThrow(new RuntimeException("save fail"));
//
//        // This should not throw (service must handle exceptions internally)
//        svc.handleSales("post: compra confirmada", p, OffsetDateTime.now());
//
//        // verify purchaseRepository.save was attempted
//        verify(purchaseRepository).save(any());
//        // counter.increment should be called to register failure (generic counter stubbed)
//        verify(counter, atLeastOnce()).increment();
//    }

    @Test
    void handleInventory_updatesExistingProduct_stockAndLogs() {
        ObjectNode p = mapper.createObjectNode(); p.put("productCode", 800); p.put("stock", 42);
        Product existing = new Product(); existing.setId(900); existing.setProductCode(800); existing.setStock(10);
        when(productRepository.findByProductCode(800)).thenReturn(existing);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockChangeLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("put: actualizar stock", p);

        verify(productRepository).save(argThat(prod -> prod.getProductCode()!=null && prod.getProductCode().equals(800) && prod.getStock()!=null && prod.getStock()==42));
        verify(stockChangeLogRepository).save(any(StockChangeLog.class));
    }
}
