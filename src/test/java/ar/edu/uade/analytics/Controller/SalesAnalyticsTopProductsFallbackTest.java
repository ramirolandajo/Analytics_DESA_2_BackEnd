package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Service.PurchaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesAnalyticsTopProductsFallbackTest {

    @Mock PurchaseService purchaseService;
    @Mock ar.edu.uade.analytics.Repository.CartRepository cartRepository;

    ar.edu.uade.analytics.Controller.SalesAnalyticsController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new ar.edu.uade.analytics.Controller.SalesAnalyticsController();
        java.lang.reflect.Field f = controller.getClass().getDeclaredField("purchaseService"); f.setAccessible(true); f.set(controller, purchaseService);
        f = controller.getClass().getDeclaredField("cartRepository"); f.setAccessible(true); f.set(controller, cartRepository);
    }

    @Test
    void getTopProducts_fallbackToCarts_whenNoPurchases() {
        when(purchaseService.getAllPurchases()).thenReturn(List.of());
        Cart c = new Cart(); c.setId(1);
        CartItem ci = new CartItem(); Product p = new Product(); p.setId(101); p.setTitle("X"); ci.setProduct(p); ci.setQuantity(2); c.setItems(List.of(ci));
        when(cartRepository.findAll()).thenReturn(List.of(c));

        ResponseEntity<java.util.Map<String,Object>> resp = controller.getTopProducts(10, null, null);
        assertNotNull(resp);
        var body = resp.getBody();
        assertNotNull(body);
        var data = body.get("data");
        assertTrue(data instanceof List);
        List<?> list = (List<?>) data;
        assertFalse(list.isEmpty());
    }
}

