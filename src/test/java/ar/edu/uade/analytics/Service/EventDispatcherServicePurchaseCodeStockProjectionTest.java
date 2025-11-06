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

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServicePurchaseCodeStockProjectionTest {
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

//    @Test
//    void handleCompraConfirmada_withPreloadedCodeStock_decrementsStockCorrectly() {
//        ObjectNode payload = mapper.createObjectNode();
//        ObjectNode user = mapper.createObjectNode(); user.put("email","x@example.com"); payload.set("user", user);
//        ObjectNode cart = mapper.createObjectNode(); ArrayNode items = mapper.createArrayNode();
//        ObjectNode it = mapper.createObjectNode(); it.put("productCode", 200); it.put("quantity", 3); items.add(it);
//        cart.set("items", items); payload.set("cart", cart);
//
//        when(userRepository.findByEmail("x@example.com")).thenReturn(null);
//        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
//
//        ProductRepository.CodeStock cs = new ProductRepository.CodeStock() {
//            public Integer getProductCode() { return 200; }
//            public Integer getStock() { return 10; }
//        };
//        when(productRepository.findByProductCodeIn(anySet())).thenReturn(List.of(cs));
//        when(productRepository.findByProductCode(200)).thenReturn(new Product());
//        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
//        when(purchase_repository().save(any())).thenAnswer(inv -> inv.getArgument(0));
//        when(stockChangeLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
//
//        svc.handleSales("post: compra confirmada", payload, OffsetDateTime.now());
//
//        verify(productRepository, atLeastOnce()).save(productCaptor.capture());
//        boolean found = productCaptor.getAllValues().stream().anyMatch(p1 -> p1.getStock()!=null && p1.getStock()==7);
//        assertTrue(found, "Expected product stock to be decreased to 7 based on CodeStock 10 minus quantity 3");
//        verify(purchase_repository()).save(any());
//    }
}
