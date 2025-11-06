package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.Product;
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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaControllerEdgeTest {
    @Mock ar.edu.uade.analytics.Service.EventService eventService;
    @Mock ar.edu.uade.analytics.Service.CartService cartService;
    @Mock ar.edu.uade.analytics.Service.ProductService productService;
    @Mock PurchaseService purchaseService;

    KafkaController ctrl;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        ctrl = new KafkaController();
        Field f = KafkaController.class.getDeclaredField("eventService"); f.setAccessible(true); f.set(ctrl, eventService);
        f = KafkaController.class.getDeclaredField("cartService"); f.setAccessible(true); f.set(ctrl, cartService);
        f = KafkaController.class.getDeclaredField("productService"); f.setAccessible(true); f.set(ctrl, productService);
        f = KafkaController.class.getDeclaredField("objectMapper"); f.setAccessible(true); f.set(ctrl, mapper);
        f = KafkaController.class.getDeclaredField("purchaseService"); f.setAccessible(true); f.set(ctrl, purchaseService);
    }

    @Test
    void receiveEvent_cartMissing_noPurchaseSaved_returns200() throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "StockConfirmed_CartPurchase");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("cartId", 10);
        payload.set("products", mapper.createArrayNode());
        root.set("payload", payload);
        root.put("timestamp", LocalDateTime.now().toString());

        when(cartService.getCartById(10)).thenReturn(Optional.empty());

        ResponseEntity<String> resp = ctrl.receiveEvent(mapper.writeValueAsString(root));
        assertEquals(200, resp.getStatusCode().value());
        verify(purchaseService, never()).savePurchase(any());
    }

    @Test
    void receiveEvent_multipleProducts_updatesAll_and_savesPurchase() throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "StockConfirmed_CartPurchase");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("cartId", 10);
        var products = mapper.createArrayNode();
        ObjectNode p1 = mapper.createObjectNode(); p1.put("productId", 1); p1.put("stockAfter", 5); products.add(p1);
        ObjectNode p2 = mapper.createObjectNode(); p2.put("productId", 2); p2.put("stockAfter", 3); products.add(p2);
        payload.set("products", products);
        root.set("payload", payload);
        root.put("timestamp", LocalDateTime.now().toString());

        Cart cart = new Cart(); cart.setId(10);
        when(cartService.getCartById(10)).thenReturn(Optional.of(cart));
        when(productService.getProductById(1)).thenReturn(Optional.of(new Product()));
        when(productService.getProductById(2)).thenReturn(Optional.of(new Product()));

        ResponseEntity<String> resp = ctrl.receiveEvent(mapper.writeValueAsString(root));
        assertEquals(200, resp.getStatusCode().value());
        verify(productService, times(2)).saveProduct(any());
        verify(purchaseService).savePurchase(any());
    }
}

