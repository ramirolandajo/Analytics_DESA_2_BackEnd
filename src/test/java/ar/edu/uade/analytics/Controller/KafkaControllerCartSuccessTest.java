package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Service.CartService;
import ar.edu.uade.analytics.Service.ProductService;
import ar.edu.uade.analytics.Service.PurchaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaControllerCartSuccessTest {

    @Mock CartService cartService;
    @Mock ProductService productService;
    @Mock PurchaseService purchaseService;
    @Mock ar.edu.uade.analytics.Service.EventService eventService;

    KafkaController controller;
    ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        controller = new KafkaController();
        java.lang.reflect.Field f;
        f = KafkaController.class.getDeclaredField("cartService"); f.setAccessible(true); f.set(controller, cartService);
        f = KafkaController.class.getDeclaredField("productService"); f.setAccessible(true); f.set(controller, productService);
        f = KafkaController.class.getDeclaredField("purchaseService"); f.setAccessible(true); f.set(controller, purchaseService);
        f = KafkaController.class.getDeclaredField("objectMapper"); f.setAccessible(true); f.set(controller, om);
        f = KafkaController.class.getDeclaredField("eventService"); f.setAccessible(true); f.set(controller, eventService);
    }

    @Test
    void receiveEvent_cartExists_savesPurchaseAndReturns200() throws Exception {
        ObjectNode root = om.createObjectNode();
        root.put("type", "StockConfirmed_CartPurchase");
        ObjectNode payload = om.createObjectNode(); payload.put("cartId", 42); payload.set("products", om.createArrayNode());
        root.set("payload", payload);
        root.put("timestamp", LocalDateTime.now().toString());

        ar.edu.uade.analytics.Entity.Cart cart = new ar.edu.uade.analytics.Entity.Cart(); cart.setId(42);
        when(cartService.getCartById(42)).thenReturn(Optional.of(cart));
        when(purchaseService.savePurchase(any())).thenReturn(null);

        ResponseEntity<String> resp = controller.receiveEvent(om.writeValueAsString(root));
        assertNotNull(resp);
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        verify(purchaseService).savePurchase(any());
    }
}
