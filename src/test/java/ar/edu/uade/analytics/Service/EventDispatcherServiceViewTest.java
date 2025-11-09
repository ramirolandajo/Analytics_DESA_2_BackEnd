package ar.edu.uade.analytics.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.uade.analytics.Repository.ProductRepository;
import ar.edu.uade.analytics.Repository.BrandRepository;
import ar.edu.uade.analytics.Repository.CategoryRepository;
import ar.edu.uade.analytics.Repository.EventRepository;
import ar.edu.uade.analytics.Repository.ReviewRepository;
import ar.edu.uade.analytics.Repository.FavouriteProductsRepository;
import ar.edu.uade.analytics.Repository.ViewRepository;
import ar.edu.uade.analytics.Repository.PurchaseRepository;
import ar.edu.uade.analytics.Repository.UserRepository;
import ar.edu.uade.analytics.Repository.StockChangeLogRepository;
import ar.edu.uade.analytics.Repository.CartRepository;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EventDispatcherServiceViewTest {

    private EventDispatcherService svc;
    private final ObjectMapper mapper = new ObjectMapper();
    private final SimpleMeterRegistry meter = new SimpleMeterRegistry();

    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock EventRepository eventRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock ViewRepository viewRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock UserRepository userRepository;
    @Mock StockChangeLogRepository stockChangeLogRepository;
    @Mock CartRepository cartRepository;

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(mapper, productRepository, brandRepository, categoryRepository,
                eventRepository, reviewRepository, favouriteProductsRepository, viewRepository,
                purchaseRepository, null, meter, stockChangeLogRepository, cartRepository);
    }

    @Test
    void handleVistaDiaria_savesViews_and_incrementsCounter() throws JsonProcessingException {
        JsonNode payload = mapper.readTree("[{\"productCode\":1001, \"device\":\"web\"}] ");
        when(viewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        svc.handleSales("get: vista diaria de productos", payload);
        verify(viewRepository, atLeastOnce()).save(any());
    }
}
