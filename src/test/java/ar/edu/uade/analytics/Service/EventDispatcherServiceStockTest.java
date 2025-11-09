package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.StockChangeLog;
import ar.edu.uade.analytics.Repository.*;
import ar.edu.uade.analytics.TestHelpers.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventDispatcherServiceStockTest {

    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock EventRepository eventRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock ViewRepository viewRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock UserRepository userRepository;
    MeterRegistry meterRegistry = TestUtils.mockMeterRegistryWithCounter();
    @Mock StockChangeLogRepository stockChangeLogRepository;
    @Mock CartRepository cartRepository;

    EventDispatcherService svc;
    com.fasterxml.jackson.databind.ObjectMapper realMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        svc = new EventDispatcherService(realMapper, productRepository, brandRepository, categoryRepository,
                eventRepository, reviewRepository, favouriteProductsRepository, viewRepository,
                purchaseRepository, userRepository, meterRegistry, stockChangeLogRepository, cartRepository);
    }

//    @Test
//    void handleActualizarStock_createsProduct_and_logsStockChange() {
//        ObjectNode payload = realMapper.createObjectNode();
//        payload.put("productCode", 777);
//        payload.put("stock", 12);
//        payload.put("name", "Nuevo Prod");
//
//        when(productRepository.findStockByProductCode(777)).thenReturn(5);
//        when(productRepository.findByProductCode(777)).thenReturn(null);
//        when(productRepository.save(any())).thenAnswer(inv -> {
//            Product p = inv.getArgument(0);
//            p.setId(1001);
//            return p;
//        });
//
//        svc.handleInventory("put: actualizar stock", payload);
//
//        verify(productRepository).save(argThat(p -> p.getProductCode() == 777 && p.getStock() == 12));
//        verify(stockChangeLogRepository).save(any(StockChangeLog.class));
//        verify(meterRegistry.get("analytics.inventory.stock.updated").counter(), atLeastOnce()).increment();
//    }
}
