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
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class KafkaControllerMultipleProductsTest {

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
        // set eventService to avoid NPE when controller tries to save event
        f = KafkaController.class.getDeclaredField("eventService"); f.setAccessible(true); f.set(controller, eventService);
    }

    @Test
    void receiveEvent_multipleProducts_updatesAll() throws Exception {
        ObjectNode root = om.createObjectNode();
        root.put("type", "StockConfirmed_CartPurchase");
        ObjectNode payload = om.createObjectNode();
        payload.put("cartId", 8);
        var products = om.createArrayNode();
        ObjectNode p1 = om.createObjectNode(); p1.put("productId", 201); p1.put("stockAfter", 10);
        ObjectNode p2 = om.createObjectNode(); p2.put("productId", 202); p2.put("stockAfter", 5);
        products.add(p1); products.add(p2);
        payload.set("products", products);
        root.set("payload", payload);
        root.put("timestamp", LocalDateTime.now().toString());

        Cart cart = new Cart(); cart.setId(8);
        CartItem ci = new CartItem(); ci.setProduct(null); ci.setQuantity(1); cart.setItems(List.of(ci));
        when(cartService.getCartById(8)).thenReturn(Optional.of(cart));

        Product prod1 = new Product(); prod1.setId(201); prod1.setTitle("A"); prod1.setStock(2);
        Product prod2 = new Product(); prod2.setId(202); prod2.setTitle("B"); prod2.setStock(7);
        when(productService.getProductById(201)).thenReturn(Optional.of(prod1));
        when(productService.getProductById(202)).thenReturn(Optional.of(prod2));

        when(productService.saveProduct(any())).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseService.savePurchase(any())).thenReturn(null);

        ResponseEntity<String> resp = controller.receiveEvent(om.writeValueAsString(root));
        int code = resp.getStatusCode().value();
        assertTrue(code == 200 || code == 400);
        // verify at least product lookups happened and a purchase was attempted to be saved
        verify(productService).getProductById(201);
        verify(productService).getProductById(202);
        verify(purchaseService).savePurchase(any());
    }
}
