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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServiceProductActiveTest {
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
    @Mock CartRepository cartRepository;

    EventDispatcherService svc;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper,
                productRepository,
                brandRepository,
                categoryRepository,
                eventRepository,
                review_repository(),
                favouriteProductsRepository,
                view_repository(),
                purchaseRepository,
                userRepository,
                meterRegistry,
                stockChangeLogRepository,
                cart_repository());
    }

    private ReviewRepository review_repository() { return reviewRepository; }
    private ViewRepository view_repository() { return view_repository; }
    private CartRepository cart_repository() { return cartRepository; }

    @Test
    void handleProductoActivo_doesNothing_whenProductMissing() {
        ObjectNode p = mapper.createObjectNode(); p.put("productCode", 12345);
        when(productRepository.findByProductCode(12345)).thenReturn(null);

        svc.handleInventory("patch: producto desactivado", p);

        verify(productRepository, never()).save(any());
    }

//    @Test
//    void handleProductoActivo_setsActiveFlag_and_incrementsCounter() {
//        ObjectNode p = mapper.createObjectNode(); p.put("productCode", 111);
//        when(productRepository.findByProductCode(111)).thenReturn(new Product());
//        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
//
//        // deactivate
//        svc.handleInventory("patch: producto desactivado", p);
//        verify(productRepository).save(any(Product.class));
//        verify(meterRegistry.get("analytics.inventory.product.active").counter(), atLeastOnce()).increment();
//
//        // activate
//        svc.handleInventory("patch: producto activado", p);
//        verify(productRepository, times(2)).save(any(Product.class));
//        verify(meterRegistry.get("analytics.inventory.product.active").counter(), times(2)).increment();
//    }
//
//    @Test
//    void handleUpsertProducto_createsProduct_withImagesAndFlags_and_incrementsCounter() {
//        ObjectNode p = mapper.createObjectNode();
//        p.put("productCode", 222);
//        p.put("name", "New With Images");
//        p.put("unitPrice", 123.45);
//        p.put("price", 120.0);
//        p.put("discount", 5.0);
//        p.put("stock", 10);
//        p.put("new", true);
//        p.put("bestSeller", true);
//        p.put("featured", false);
//        p.put("hero", true);
//        p.put("active", true);
//        p.putArray("images").add("/img/a.jpg").add("/img/b.jpg");
//        p.put("brandCode", 7);
//        ArrayNode cats = mapper.createArrayNode(); cats.add(1); cats.add(2); p.set("categoryCodes", cats);
//
//        Brand b = new Brand(); b.setId(7); b.setBrandCode(7);
//        when(brandRepository.findByBrandCode(7)).thenReturn(b);
//        when(categoryRepository.findByCategoryCodeIn(List.of(1,2))).thenReturn(List.of(new Category(), new Category()));
//        when(productRepository.findByProductCode(222)).thenReturn(null);
//        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
//
//        svc.handleInventory("post: agregar un producto", p);
//
//        verify(productRepository).save(argThat(prod -> prod.getProductCode()!=null && prod.getProductCode().equals(222)));
//        verify(meterRegistry.get("analytics.inventory.product.upsert").counter(), atLeastOnce()).increment();
//    }

//    @Test
//    void handleUpsertProducto_updatesExistingProduct_changesFields_but_keepsId() {
//        ObjectNode p = mapper.createObjectNode();
//        p.put("productCode", 333);
//        p.put("name", "Existing Updated");
//        p.put("price", 99.9);
//        Product existing = new Product(); existing.setId(55); existing.setProductCode(333); existing.setTitle("Old"); existing.setStock(5);
//        when(productRepository.findByProductCode(333)).thenReturn(existing);
//        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
//
//        svc.handleInventory("post: agregar un producto", p);
//
//        verify(productRepository).save(argThat(prod -> prod.getId()!=null && prod.getId().equals(55) && "Existing Updated".equals(prod.getTitle())));
//        verify(meterRegistry.get("analytics.inventory.product.upsert").counter(), atLeastOnce()).increment();
//    }

}
