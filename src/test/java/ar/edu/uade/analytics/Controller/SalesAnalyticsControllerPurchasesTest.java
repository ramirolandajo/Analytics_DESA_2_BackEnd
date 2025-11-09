package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.Purchase;
import ar.edu.uade.analytics.Service.PurchaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesAnalyticsControllerPurchasesTest {

    @Mock
    PurchaseService purchaseService;
    @Mock
    ar.edu.uade.analytics.Repository.CartRepository cartRepository;
    @Mock
    ar.edu.uade.analytics.Repository.ConsumedEventLogRepository consumedEventLogRepository;

    SalesAnalyticsController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new SalesAnalyticsController();
        java.lang.reflect.Field f;
        f = SalesAnalyticsController.class.getDeclaredField("purchaseService"); f.setAccessible(true); f.set(controller, purchaseService);
        f = SalesAnalyticsController.class.getDeclaredField("cartRepository"); f.setAccessible(true); f.set(controller, cartRepository);
        f = SalesAnalyticsController.class.getDeclaredField("consumedEventLogRepository"); f.setAccessible(true); f.set(controller, consumedEventLogRepository);
        f = SalesAnalyticsController.class.getDeclaredField("objectMapper"); f.setAccessible(true); f.set(controller, new ObjectMapper());
    }

    @Test
    void getTopProducts_fromPurchases_aggregatesProperly() {
        Product p = new Product(); p.setId(11); p.setTitle("PP");
        CartItem it = new CartItem(); it.setProduct(p); it.setQuantity(2);
        Cart c = new Cart(); c.setItems(List.of(it));
        Purchase purchase = new Purchase(); purchase.setId(1); purchase.setStatus(Purchase.Status.CONFIRMED); purchase.setCart(c); purchase.setDate(LocalDateTime.now());
        when(purchaseService.getAllPurchases()).thenReturn(List.of(purchase));

        ResponseEntity<Map<String, Object>> resp = controller.getTopProducts(10, null, null);
        assertEquals(200, resp.getStatusCode().value());
        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        List<?> data = (List<?>) body.get("data");
        assertFalse(data.isEmpty());
    }
}

