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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class EventDispatcherServicePurchaseStockTest {
    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock EventRepository eventRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock ViewRepository view_repository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock UserRepository userRepository;
    MeterRegistry meterRegistry = TestUtils.mockMeterRegistryWithCounter();
    @Mock StockChangeLogRepository stockChangeLogRepository;
    @Mock CartRepository cart_repository;

    EventDispatcherService svc;
    ObjectMapper mapper = new ObjectMapper();
    Counter counter;

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper,
                productRepository,
                brandRepository,
                categoryRepository,
                eventRepository,
                reviewRepository,
                favouriteProductsRepository,
                view_repository,
                purchaseRepository,
                userRepository,
                meterRegistry,
                stockChangeLogRepository,
                cart_repository);
        counter = meterRegistry.counter("analytics.inventory.stock.updated");
    }

    @Test
    void handleCompraConfirmada_decrementsStock_usingPreloadedCodeStock() {
        ObjectNode payload = mapper.createObjectNode();
        ObjectNode user = mapper.createObjectNode(); user.put("email", "u@example.com"); payload.set("user", user);
        ObjectNode cart = mapper.createObjectNode(); cart.put("cartId", 1); ArrayNode items = mapper.createArrayNode(); ObjectNode it = mapper.createObjectNode(); it.put("productCode", 200); it.put("quantity", 3); items.add(it); cart.set("items", items); payload.set("cart", cart);

        Product p = new Product(); p.setId(20); p.setProductCode(200); p.setStock(10);
        when(userRepository.findByEmail("u@example.com")).thenReturn(null);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.findByProductCode(200)).thenReturn(p);
        // Provide CodeStock projection to simulate preloaded stock
        ProductRepository.CodeStock cs = new ProductRepository.CodeStock(){
            public Integer getProductCode(){ return 200; }
            public Integer getStock(){ return 10; }
        };
        when(productRepository.findByProductCodeIn(any())).thenReturn(List.of(cs));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockChangeLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleSales("post: compra confirmada", payload);

        // After purchase, product stock should have been reduced to 7
        verify(productRepository, atLeastOnce()).save(argThat((Product prod) -> prod.getStock()!=null && prod.getStock()==7));
        verify(stockChangeLogRepository, atLeastOnce()).save(any());
        verify(purchaseRepository).save(any());
    }

    @Test
    void handleBatchProductos_countsFail_whenSaveThrows() {
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode items = mapper.createArrayNode();
        ObjectNode it1 = mapper.createObjectNode(); it1.put("product_code", 301); items.add(it1);
        ObjectNode it2 = mapper.createObjectNode(); it2.put("product_code", 302); items.add(it2);
        payload.set("items", items);

        when(productRepository.findByProductCode(anyInt())).thenReturn(null);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (Integer.valueOf(302).equals(p.getProductCode())) throw new RuntimeException("fail-save");
            return p;
        });

        svc.handleInventory("post: agregar productos (batch)", payload);

        // verify counters for ok and fail were requested
        verify(meterRegistry).counter("analytics.inventory.product.batch.ok");
        verify(meterRegistry).counter("analytics.inventory.product.batch.fail");
    }
}
