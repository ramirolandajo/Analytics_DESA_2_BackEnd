package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Service.PurchaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesAnalyticsLowStockBoundaryTest {

    @Mock PurchaseService purchaseService;
    @Mock ar.edu.uade.analytics.Repository.CartRepository cartRepository;

    SalesAnalyticsController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new SalesAnalyticsController();
        java.lang.reflect.Field f;
        f = SalesAnalyticsController.class.getDeclaredField("purchaseService"); f.setAccessible(true); f.set(controller, purchaseService);
        f = SalesAnalyticsController.class.getDeclaredField("cartRepository"); f.setAccessible(true); f.set(controller, cartRepository);
    }

    @Test
    void getLowStockBoundary_inclusiveBehavior() {
        // Mock a ProductRepository that returns two products (one at threshold 10 and one below)
        ar.edu.uade.analytics.Repository.ProductRepository pr = mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        Product prod1 = new Product(); prod1.setId(10); prod1.setStock(10); prod1.setTitle("P10");
        Product prod2 = new Product(); prod2.setId(11); prod2.setStock(9); prod2.setTitle("P11");
        when(pr.findAll()).thenReturn(List.of(prod1, prod2));
        when(purchaseService.getProductRepository()).thenReturn(pr);

        ResponseEntity<Map<String, Object>> resp = controller.getLowStockProducts(10, 10);
        assertNotNull(resp);
        Map<String, Object> response = resp.getBody();
        assertNotNull(response);
        Object data = response.get("data");
        assertInstanceOf(List.class, data);
        List<?> list = (List<?>) data;
        assertEquals(2, list.size());
    }
}
