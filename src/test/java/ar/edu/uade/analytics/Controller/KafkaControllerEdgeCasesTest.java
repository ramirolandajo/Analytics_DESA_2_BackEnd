package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
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

class KafkaControllerEdgeCasesTest {
    static void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void receiveEvent_productMissing_doesNotFail_but_doesNotSaveProduct() throws Exception {
        KafkaController controller = new KafkaController();
        EventService eventService = mock(EventService.class);
        CartService cartService = mock(CartService.class);
        ProductService productService = mock(ProductService.class);
        PurchaseService purchaseService = mock(PurchaseService.class);
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        root.put("type", "StockConfirmed_CartPurchase");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("cartId", 500);
        var products = mapper.createArrayNode();
        ObjectNode pnode = mapper.createObjectNode();
        pnode.put("productId", 9999); // product missing
        pnode.put("stockAfter", 3);
        products.add(pnode);
        payload.set("products", products);
        root.set("payload", payload);
        root.put("timestamp", LocalDateTime.now().toString());

        Cart cart = new Cart(); CartItem it = new CartItem(); it.setProduct(null); it.setQuantity(1); cart.setItems(List.of(it));
        when(cartService.getCartById(500)).thenReturn(Optional.of(cart));
        when(productService.getProductById(9999)).thenReturn(Optional.empty());

        setField(controller, "eventService", eventService);
        setField(controller, "cartService", cartService);
        setField(controller, "productService", productService);
        setField(controller, "purchaseService", purchaseService);
        setField(controller, "objectMapper", mapper);

        ResponseEntity<String> resp = controller.receiveEvent(mapper.writeValueAsString(root));
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        verify(productService, never()).saveProduct(any());
        verify(purchaseService, times(1)).savePurchase(any());
    }
}
