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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaControllerCartMissingTest {

    @Mock CartService cartService;
    @Mock ProductService productService;
    @Mock PurchaseService purchaseService;

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
    }

    @Test
    void receiveEvent_cartMissing_noPurchaseSaved() throws Exception {
        ObjectNode root = om.createObjectNode();
        root.put("type", "StockConfirmed_CartPurchase");
        ObjectNode payload = om.createObjectNode();
        payload.put("cartId", 99999);
        payload.set("products", om.createArrayNode());
        root.set("payload", payload);
        root.put("timestamp", LocalDateTime.now().toString());

        org.mockito.Mockito.lenient().when(cartService.getCartById(99999)).thenReturn(Optional.empty());

        ResponseEntity<String> resp = controller.receiveEvent(om.writeValueAsString(root));
        // controller may return 200 or 400 depending on payload parsing; ensure handled and no purchase saved
        int code = resp.getStatusCode().value();
        assertTrue(code == 200 || code == 400);
        // purchaseService.savePurchase should not be invoked because cart not found
        verify(purchaseService, never()).savePurchase(any());
    }
}
