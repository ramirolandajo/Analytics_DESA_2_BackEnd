package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.Purchase;
import ar.edu.uade.analytics.Service.CartService;
import ar.edu.uade.analytics.Service.EventService;
import ar.edu.uade.analytics.Service.ProductService;
import ar.edu.uade.analytics.Service.PurchaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaControllerTest4 {

    @Test
    void receiveEvent_cartExists_savesPurchase() throws Exception {
        KafkaController controller = new KafkaController();
        EventService eventService = mock(EventService.class);
        CartService cartService = mock(CartService.class);
        ProductService productService = mock(ProductService.class);
        PurchaseService purchaseService = mock(PurchaseService.class);
        ObjectMapper mapper = new ObjectMapper();

        // Build payload JSON
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "StockConfirmed_CartPurchase");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("cartId", 123);
        var products = mapper.createArrayNode();
        ObjectNode pnode = mapper.createObjectNode();
        pnode.put("productId", 10);
        pnode.put("stockAfter", 7);
        products.add(pnode);
        payload.set("products", products);
        root.set("payload", payload);
        root.put("timestamp", LocalDateTime.now().toString());

        // Cart and product present
        Cart cart = new Cart();
        CartItem item = new CartItem();
        Product prod = new Product(); prod.setId(10); item.setProduct(prod); item.setQuantity(1);
        cart.setItems(List.of(item));

        when(cartService.getCartById(123)).thenReturn(Optional.of(cart));
        when(productService.getProductById(10)).thenReturn(Optional.of(prod));
        when(purchaseService.savePurchase(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));

        // inject
        setField(controller, "eventService", eventService);
        setField(controller, "cartService", cartService);
        setField(controller, "productService", productService);
        setField(controller, "purchaseService", purchaseService);
        setField(controller, "objectMapper", mapper);

        ResponseEntity<String> resp = controller.receiveEvent(mapper.writeValueAsString(root));
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        verify(productService, times(1)).saveProduct(any(Product.class));
        verify(purchaseService, times(1)).savePurchase(any(Purchase.class));
        verify(eventService, times(1)).saveEvent(any());
    }

//    @Test
//    void receiveEvent_cartMissing_returnsBadRequest() throws Exception {
//        KafkaController controller = new KafkaController();
//        EventService eventService = mock(EventService.class);
//        CartService cartService = mock(CartService.class);
//        ProductService productService = mock(ProductService.class);
//        PurchaseService purchaseService = mock(PurchaseService.class);
//        ObjectMapper mapper = new ObjectMapper();
//
//        ObjectNode root = mapper.createObjectNode();
//        root.put("type", "StockConfirmed_CartPurchase");
//        ObjectNode payload = mapper.createObjectNode();
//        payload.put("cartId", 999);
//        payload.set("products", mapper.createArrayNode());
//        root.set("payload", payload);
//        root.put("timestamp", LocalDateTime.now().toString());
//
//        when(cartService.getCartById(999)).thenReturn(Optional.empty());
//
//        setField(controller, "eventService", eventService);
//        setField(controller, "cartService", cartService);
//        setField(controller, "productService", productService);
//        setField(controller, "purchaseService", purchaseService);
//        setField(controller, "objectMapper", mapper);
//
//        ResponseEntity<String> resp = controller.receiveEvent(mapper.writeValueAsString(root));
//        assertTrue(resp.getStatusCode().is4xxClientError());
//        // event should still be saved
//        verify(eventService, times(1)).saveEvent(any());
//        verify(purchaseService, never()).savePurchase(any());
//    }

    // reflection helper
    static void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
