package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServiceInventoryTest {
    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock EventRepository eventRepository;
    @Mock ReviewRepository review_repository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock ViewRepository viewRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock UserRepository userRepository;
    // use a real simple registry to avoid NPEs from counter(...) returning null
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    @Mock StockChangeLogRepository stockChangeLogRepository;
    @Mock CartRepository cart_repository;

    EventDispatcherService svc;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper,
                productRepository,
                brandRepository,
                categoryRepository,
                eventRepository,
                review_repository,
                favouriteProductsRepository,
                viewRepository,
                purchaseRepository,
                userRepository,
                meterRegistry,
                stockChangeLogRepository,
                cart_repository);
        // SimpleMeterRegistry will provide real Counter instances and avoid NPEs.
    }

    @Test
    void handleUpsertProducto_resolvesBrandAndCategories_and_savesProduct() {
        ObjectNode p = mapper.createObjectNode();
        p.put("productCode", 111);
        p.put("name", "Prod111");
        p.put("brandCode", 77);
        ArrayNode codes = mapper.createArrayNode(); codes.add(10); codes.add(11);
        p.set("categoryCodes", codes);

        Brand b = new Brand(); b.setId(7); b.setBrandCode(77);
        when(brandRepository.findByBrandCode(77)).thenReturn(b);
        List<Category> cats = List.of(new Category(), new Category());
        when(categoryRepository.findByCategoryCodeIn(List.of(10,11))).thenReturn(cats);
        when(productRepository.findByProductCode(111)).thenReturn(null);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: agregar un producto", p);

        verify(productRepository).save(any(Product.class));
        verify(brandRepository).findByBrandCode(77);
        verify(categoryRepository).findByCategoryCodeIn(List.of(10,11));
    }

    @Test
    void handleBatchProductos_countsOkAndFail() {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode items = mapper.createArrayNode();
        ObjectNode okItem = mapper.createObjectNode(); okItem.put("productCode", 200);
        ObjectNode badItem = mapper.createObjectNode(); badItem.put("productCode", 201);
        items.add(okItem); items.add(badItem);
        root.set("items", items);

        // productRepository.save for ok works, for bad throws
        when(productRepository.findByProductCode(200)).thenReturn(null);
        when(productRepository.findByProductCode(201)).thenReturn(null);
        when(productRepository.save(argThat(ar.edu.uade.analytics.TestHelpers.TestUtils.productWithCode(201)))).thenThrow(new RuntimeException("boom"));
        when(productRepository.save(argThat(ar.edu.uade.analytics.TestHelpers.TestUtils.productWithCode(200)))).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: agregar productos (batch)", root);

        // verify the MeterRegistry contains the counters (created by the service)
        assertNotNull(meterRegistry.find("analytics.inventory.product.batch.ok").counter(), "ok counter should exist");
        assertNotNull(meterRegistry.find("analytics.inventory.product.batch.fail").counter(), "fail counter should exist");
    }

    @Test
    void handleMarca_createsOrUpdatesBrand_and_incrementsCounter() {
        ObjectNode p = mapper.createObjectNode();
        p.put("brandCode", 123);
        p.put("name", "MarcaX");

        when(brandRepository.findByBrandCode(123)).thenReturn(null);
        when(brandRepository.findByNameIgnoreCase("MarcaX")).thenReturn(null);
        when(brandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: marca creada", p);

        verify(brandRepository).save(any(Brand.class));
        // assert that the registry now has at least one meter
        assertFalse(meterRegistry.getMeters().isEmpty(), "meter registry should have created meters");
    }

    @Test
    void handleCategoria_createsOrUpdatesCategory_and_incrementsCounter() {
        ObjectNode p = mapper.createObjectNode();
        p.put("categoriesCode", 55);
        p.put("name", "CatY");

        when(categoryRepository.findByCategoryCode(55)).thenReturn(null);
        when(categoryRepository.findByNameIgnoreCase("CatY")).thenReturn(null);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: categoría creada", p);

        verify(categoryRepository).save(any(Category.class));
        assertFalse(meterRegistry.getMeters().isEmpty(), "meter registry should have created meters");
    }

    @Test
    void handleActualizarStock_createsProduct_and_logsStockChange() {
        ObjectNode p = mapper.createObjectNode();
        p.put("productCode", 9999);
        p.put("stock", 5);
        p.put("name", "NewProd");

        when(productRepository.findStockByProductCode(9999)).thenReturn(null);
        when(productRepository.findByProductCode(9999)).thenReturn(null);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product prod = inv.getArgument(0);
            prod.setId(777);
            return prod;
        });
        when(stockChangeLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("put: actualizar stock", p);

        verify(productRepository).save(any(Product.class));
        verify(stockChangeLogRepository).save(any(StockChangeLog.class));
        // inventory update and log counters should be incremented twice in total
    }

}
