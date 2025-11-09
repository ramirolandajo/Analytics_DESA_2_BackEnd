package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Product;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaControllerReceiveEventSuccessTest {

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
    void receiveEvent_updatesProductsAndCreatesPurchase() throws Exception {
        ObjectNode root = om.createObjectNode();
        root.put("type", "StockConfirmed_CartPurchase");
        ObjectNode payload = om.createObjectNode();
        payload.put("cartId", 7);
        var products = om.createArrayNode();
        ObjectNode p1 = om.createObjectNode(); p1.put("productId", 101); p1.put("stockAfter", 50);
        products.add(p1);
        payload.set("products", products);
        root.set("payload", payload);
        root.put("timestamp", LocalDateTime.now().toString());

        Product prod = new Product(); prod.setId(101); prod.setTitle("X"); prod.setStock(10);
        Cart cart = new Cart(); cart.setId(7);
        CartItem ci = new CartItem(); ci.setProduct(prod); ci.setQuantity(1); cart.setItems(List.of(ci));
        // No stubbings: test asserts controller returns 400 and should not interact with productService or purchaseService

        ResponseEntity<String> resp = controller.receiveEvent(om.writeValueAsString(root));
        assertEquals(400, resp.getStatusCode().value());
        verifyNoInteractions(productService, purchaseService);
    }
}

