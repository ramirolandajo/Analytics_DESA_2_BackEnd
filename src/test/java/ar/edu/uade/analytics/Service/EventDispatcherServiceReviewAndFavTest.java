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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventDispatcherServiceReviewAndFavTest {
    @Mock ProductRepository productRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock ViewRepository viewRepository;
    @Mock MeterRegistry meterRegistry;
    @Mock Counter counter;
    @Mock BrandRepository brandRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock EventRepository eventRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock UserRepository userRepository;
    @Mock StockChangeLogRepository stockChangeLogRepository;
    @Mock CartRepository cartRepository;

    EventDispatcherService svc;
    ObjectMapper mapper = new ObjectMapper();

    @Captor ArgumentCaptor<Review> reviewCaptor;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString())).thenReturn(counter);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        // pasar todos los mocks al constructor para que el servicio tenga productRepository y otros
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
    }

    private ViewRepository view_repository() { return viewRepository; }
    private CartRepository cart_repository() { return cartRepository; }

    @Test
    void handleReview_acceptsDifferentRatingKeys_and_saves() {
        ObjectNode p = mapper.createObjectNode();
        p.put("rateUpdated", 4.5);
        p.put("message", "ok");
        p.put("productCode", 333);
        Product prod = new Product(); prod.setId(333);
        when(productRepository.findByProductCode(333)).thenReturn(prod);
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // llamar al flujo público que enruta a handleReview
        svc.handleSales("post: review creada", p, null);

        verify(reviewRepository).save(reviewCaptor.capture());
        assertEquals(4.5f, reviewCaptor.getValue().getCalification());
        assertEquals("ok", reviewCaptor.getValue().getDescription());
    }

    @Test
    void handleFavRemove_deletesFirstMatchingFavourite() {
        FavouriteProducts f1 = new FavouriteProducts(); f1.setProductCode(10);
        FavouriteProducts f2 = new FavouriteProducts(); f2.setProductCode(20);
        when(favouriteProductsRepository.findAll()).thenReturn(List.of(f1, f2));

        ObjectNode p = mapper.createObjectNode(); p.put("productCode", 20);
        svc.handleSales("delete: producto quitado de favoritos", p, null);

        verify(favouriteProductsRepository).delete(f2);
    }
}
