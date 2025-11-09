package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventDispatcherServiceExtraTest {

    @org.mockito.Mock private ProductRepository productRepository;
    @org.mockito.Mock private BrandRepository brandRepository;
    @org.mockito.Mock private CategoryRepository categoryRepository;
    @org.mockito.Mock private EventRepository eventRepository;
    @org.mockito.Mock private ReviewRepository reviewRepository;
    @org.mockito.Mock private FavouriteProductsRepository favouriteProductsRepository;
    @org.mockito.Mock private ViewRepository viewRepository;
    @org.mockito.Mock private PurchaseRepository purchaseRepository;
    @org.mockito.Mock private UserRepository userRepository;
    @org.mockito.Mock private StockChangeLogRepository stockChangeLogRepository;
    @org.mockito.Mock private CartRepository cartRepository;

    private ObjectMapper mapper = new ObjectMapper();
    private SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private EventDispatcherService svc;

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper, productRepository, brandRepository, categoryRepository,
                eventRepository, reviewRepository, favouriteProductsRepository, viewRepository,
                purchaseRepository, userRepository, meterRegistry, stockChangeLogRepository, cartRepository);
    }

    @Test
    void handleProductoActivo_setsActive_and_incrementsMetric() throws Exception {
        String json = "{\"productCode\":101}";
        JsonNode p = mapper.readTree(json);
        Product existing = new Product(); existing.setId(10); existing.setProductCode(101);
        when(productRepository.findByProductCode(101)).thenReturn(existing);
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        svc.handleInventory("patch: producto activado", p);
        verify(productRepository).save(argThat(prod -> Boolean.TRUE.equals(((Product)prod).getActive()) || ((Product)prod).getActive() == Boolean.TRUE));
        assertEquals(1.0, meterRegistry.counter("analytics.inventory.product.active", "active", "true").count());

        // deactivate
        existing.setActive(true);
        svc.handleInventory("patch: producto desactivado", p);
        verify(productRepository, times(2)).save(any());
        assertEquals(1.0, meterRegistry.counter("analytics.inventory.product.active", "active", "false").count());
    }

    @Test
    void handleCategoria_createsCategory_and_incrementsMetric() throws Exception {
        String json = "{\"name\":\"NewCategory\"}";
        JsonNode p = mapper.readTree(json);
        when(categoryRepository.findByNameIgnoreCase("NewCategory")).thenReturn(null);
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        svc.handleInventory("post: categoría creada", p);
        verify(categoryRepository).save(any());
        assertEquals(1.0, meterRegistry.counter("analytics.inventory.category.upsert").count());
    }

//    @Test
//    void handleBatchProductos_countsOkAndFail() throws Exception {
//        String json = "{\"items\":[{\"productCode\":201, \"name\":\"A\"},{\"productCode\":202, \"name\":\"B\"}]}";
//        JsonNode p = mapper.readTree(json);
//
//        // use a mocked MeterRegistry and Counters for deterministic verification
//        io.micrometer.core.instrument.MeterRegistry mockRegistry = mock(io.micrometer.core.instrument.MeterRegistry.class);
//        io.micrometer.core.instrument.Counter okCounter = mock(io.micrometer.core.instrument.Counter.class);
//        io.micrometer.core.instrument.Counter failCounter = mock(io.micrometer.core.instrument.Counter.class);
//        when(mockRegistry.counter(eq("analytics.inventory.product.batch.ok"))).thenReturn(okCounter);
//        when(mockRegistry.counter(eq("analytics.inventory.product.batch.fail"))).thenReturn(failCounter);
//        // fallback to avoid NPE for other counters
//        when(mockRegistry.counter(anyString(), any(String[].class))).thenReturn(okCounter);
//
//        // recreate service using the mocked registry so counters can be verified
//        svc = new EventDispatcherService(mapper, productRepository, brandRepository, categoryRepository,
//                eventRepository, reviewRepository, favouriteProductsRepository, viewRepository,
//                purchaseRepository, userRepository, mockRegistry, stockChangeLogRepository, cartRepository);
//
//        // productRepository.save for productCode 201 throws to simulate failure
//        when(productRepository.findByProductCode(anyInt())).thenReturn(null);
//        when(productRepository.save(any())).thenAnswer(invocation -> {
//            Product prod = invocation.getArgument(0);
//            if (prod == null) return null;
//            Integer code = prod.getProductCode();
//            if (code != null && code.equals(201)) throw new RuntimeException("boom");
//            return prod;
//        });
//
//        svc.handleInventory("post: agregar productos (batch)", p);
//
//        long okInvocations = mockingDetails(okCounter).getInvocations().stream()
//                .filter(inv -> inv.getMethod().getName().equals("increment"))
//                .count();
//        assertTrue(okInvocations >= 1, "expected okCounter to be incremented");
//
//        long failInvocations = mockingDetails(failCounter).getInvocations().stream()
//                .filter(inv -> inv.getMethod().getName().equals("increment"))
//                .count();
//        when(mockRegistry.counter(eq("analytics.inventory.product.batch.ok"))).thenReturn(okCounter);
//        when(mockRegistry.counter(eq("analytics.inventory.product.batch.fail"))).thenReturn(failCounter);
//        // also match calls with tags (name, tags...)
//        when(mockRegistry.counter(eq("analytics.inventory.product.batch.ok"), any(String[].class))).thenReturn(okCounter);
//        when(mockRegistry.counter(eq("analytics.inventory.product.batch.fail"), any(String[].class))).thenReturn(failCounter);
//        // fallback: return a fresh mock counter to avoid mixing ok/fail into the same mock
//        when(mockRegistry.counter(anyString(), any(String[].class))).thenAnswer(inv -> mock(io.micrometer.core.instrument.Counter.class));
//        verify(okCounter, atLeastOnce()).increment();
//        verify(failCounter, atLeastOnce()).increment();
//    }


    @Test
    void handleReview_savesAndIncrements() throws Exception {
        String json = "{\"productCode\":301, \"rating\":4.5, \"message\":\"ok\"}";
        JsonNode p = mapper.readTree(json);
        Product prod = new Product(); prod.setProductCode(301);
        when(productRepository.findByProductCode(301)).thenReturn(prod);
        when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        svc.handleSales("post: review creada", p);
        verify(reviewRepository).save(any());
        assertEquals(1.0, meterRegistry.counter("analytics.sales.review").count());
    }

    @Test
    void handleFavRemove_noop_and_metricIncrement() throws Exception {
        String json = "{\"productCode\":401}";
        JsonNode p = mapper.readTree(json);
        when(favouriteProductsRepository.findAll()).thenReturn(List.of());

        svc.handleSales("delete: producto quitado de favoritos", p);
        verify(favouriteProductsRepository, never()).delete(any());
        assertEquals(1.0, meterRegistry.counter("analytics.sales.fav", "op", "remove").count());
    }

    @Test
    void saveAnalyticsEvent_persistsAndIncrements() throws Exception {
        String json = "{\"x\":1}";
        JsonNode p = mapper.readTree(json);
        when(eventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        svc.handleSales("post: compra pendiente", p);
        verify(eventRepository).save(any());
        assertEquals(1.0, meterRegistry.counter("analytics.sales.event", "type", "post: compra pendiente").count());
    }

    @Test
    void handleVistaDiaria_withViewsArray_savesAndCounts() throws Exception {
        String json = "{\"views\":[{\"productCode\":501},{\"productCode\":502}]}";
        JsonNode p = mapper.readTree(json);
        when(productRepository.findByProductCode(501)).thenReturn(new Product());
        when(productRepository.findByProductCode(502)).thenReturn(null);
        when(viewRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        svc.handleSales("get: vista diaria de productos", p, OffsetDateTime.now());
        verify(viewRepository, times(2)).save(any());
        assertEquals(2.0, meterRegistry.counter("analytics.sales.view.daily").count());
    }
}
