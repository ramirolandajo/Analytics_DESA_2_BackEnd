package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServiceMoreSalesTest {
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

    @Captor ArgumentCaptor<Review> reviewCaptor;
    @Captor ArgumentCaptor<FavouriteProducts> favCaptor;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        // Pass exactly the 13 dependencies the service constructor expects
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
                cartRepository);
    }

    @Test
    void handleReview_savesReview_and_incrementsCounter() {
        ObjectNode p = mapper.createObjectNode();
        p.put("rating", 4.5);
        p.put("message", "Great");
        p.put("productCode", 200);

        Product prod = new Product(); prod.setId(200); prod.setProductCode(200);
        when(productRepository.findByProductCode(200)).thenReturn(prod);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        svc.handleSales("post: review creada", p, null);

        verify(reviewRepository).save(reviewCaptor.capture());
        assertEquals(4.5f, reviewCaptor.getValue().getCalification());
        verify(counter).increment();
    }

    @Test
    void handleFavAdd_savesFavourite_and_increments() {
        ObjectNode p = mapper.createObjectNode(); p.put("productCode", 300);
        Product prod = new Product(); prod.setId(300); prod.setProductCode(300);
        when(productRepository.findByProductCode(300)).thenReturn(prod);
        when(favouriteProductsRepository.save(any(FavouriteProducts.class))).thenAnswer(inv -> inv.getArgument(0));

        svc.handleSales("post: producto agregado a favoritos", p, null);

        verify(favouriteProductsRepository).save(favCaptor.capture());
        assertEquals(300, favCaptor.getValue().getProductCode());
        verify(counter).increment();
    }

    @Test
    void handleFavAdd_doesNotSave_whenProductMissing() {
        ObjectNode p = mapper.createObjectNode(); p.put("productCode", 9999);
        when(productRepository.findByProductCode(9999)).thenReturn(null);

        svc.handleSales("post: producto agregado a favoritos", p, null);

        verify(favouriteProductsRepository, never()).save(any());
    }

    @Test
    void handleFavRemove_deletesMatching_and_increments() {
        ObjectNode p = mapper.createObjectNode(); p.put("productCode", 400);
        FavouriteProducts f1 = new FavouriteProducts(); f1.setId(1L); f1.setProductCode(400);
        FavouriteProducts f2 = new FavouriteProducts(); f2.setId(2L); f2.setProductCode(401);
        when(favouriteProductsRepository.findAll()).thenReturn(Arrays.asList(f1, f2));

        svc.handleSales("delete: producto quitado de favoritos", p, null);

        verify(favouriteProductsRepository).delete(f1);
        verify(counter).increment();
    }

    @Test
    void saveAnalyticsEvent_persistsEvent_and_incrementsCounter() {
        ObjectNode payload = mapper.createObjectNode(); payload.put("x","y");
        svc.handleSales("post: compra pendiente", payload, null);
        verify(eventRepository).save(any());
        verify(counter).increment();
    }
}
