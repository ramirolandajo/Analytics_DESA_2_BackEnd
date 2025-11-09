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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServiceInventoryBranchesTest {
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
    private CartRepository cart_repository() { return cartRepository; }

    @Test
    void handleVistaDiaria_arrayPayload_savesViewsAndAssociatesProducts() {
        ArrayNode arr = mapper.createArrayNode();
        ObjectNode v1 = mapper.createObjectNode(); v1.put("productCode", 100);
        ObjectNode v2 = mapper.createObjectNode(); v2.put("productCode", 101);
        arr.add(v1); arr.add(v2);

        Product p100 = new Product(); p100.setId(100); p100.setProductCode(100);
        when(productRepository.findByProductCode(100)).thenReturn(p100);
        when(productRepository.findByProductCode(101)).thenReturn(null);

        svc.handleSales("get: vista diaria de productos", arr, OffsetDateTime.now());

        verify(viewRepository, times(2)).save(any(View.class));
    }

    @Test
    void handleActualizarStock_createsProduct_whenMissing_and_savesLog_and_counters() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("productCode", 555); payload.put("stock", 7); payload.put("reason","manual update");

        when(productRepository.findStockByProductCode(555)).thenReturn(null);
        when(productRepository.findByProductCode(555)).thenReturn(null);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(999);
            return p;
        });
        when(stockChangeLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("put: actualizar stock", payload);

        verify(productRepository).save(argThat(p -> p.getProductCode()!=null && p.getStock()!=null && p.getStock()==7));
        verify(stockChangeLogRepository).save(any());
        verify(meterRegistry, atLeastOnce()).counter(eq("analytics.inventory.stock.updated"));
    }

    @Test
    void handleUpsertProducto_resolvesCategories_byCategoryCodes_and_savesProduct() {
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode codes = mapper.createArrayNode(); codes.add(10); codes.add(11);
        payload.set("categoryCodes", codes);
        payload.put("productCode", 333);

        Category c1 = new Category(); c1.setId(10); c1.setCategoryCode(10);
        Category c2 = new Category(); c2.setId(11); c2.setCategoryCode(11);
        when(categoryRepository.findByCategoryCodeIn(anyList())).thenReturn(List.of(c1,c2));
        when(productRepository.findByProductCode(333)).thenReturn(null);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: agregar un producto", payload);

        verify(categoryRepository).findByCategoryCodeIn(anyList());
        verify(productRepository).save(argThat(p -> p.getCategories()!=null && p.getCategories().size()==2));
    }

    @Test
    void handleUpsertProducto_fallbackCategoriesIds_fromCategoriesArray_callsFindByIdIn() {
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode arr = mapper.createArrayNode(); arr.add(21); arr.add(22);
        payload.set("categories", arr);
        payload.put("productCode", 444);

        when(categoryRepository.findByCategoryCodeIn(anyList())).thenReturn(List.of());
        when(categoryRepository.findByIdIn(anyList())).thenReturn(List.of(new Category(){ { setId(21); } }, new Category(){ { setId(22); } }));
        when(productRepository.findByProductCode(444)).thenReturn(null);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: agregar un producto", payload);

        verify(categoryRepository).findByIdIn(anyList());
        verify(productRepository).save(argThat(p -> p.getCategories()!=null && p.getCategories().size()==2));
    }

    @Test
    void handleMarca_resolvesByName_and_savesBrand_and_incrementsCounter() {
        ObjectNode payload = mapper.createObjectNode(); payload.put("name","MarcaZ"); payload.put("brandCode", 77);
        when(brandRepository.findByNameIgnoreCase("MarcaZ")).thenReturn(null);
        when(brandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: marca creada", payload);

        verify(brandRepository).save(any());
        verify(meterRegistry).counter(eq("analytics.inventory.brand.upsert"));
    }

    @Test
    void handleProductoActivo_togglesActive_and_saves() {
        ObjectNode payload = mapper.createObjectNode(); payload.put("productCode", 888);
        Product prod = new Product(); prod.setProductCode(888); prod.setId(5);
        when(productRepository.findByProductCode(888)).thenReturn(prod);

        svc.handleInventory("patch: producto desactivado", payload);

        verify(productRepository).save(argThat(p -> p.getActive()!=null && !p.getActive()));
        verify(meterRegistry).counter(eq("analytics.inventory.product.active"), any(), any());
    }
}

