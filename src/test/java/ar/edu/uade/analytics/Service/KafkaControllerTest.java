package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Controller.KafkaController;
import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaControllerTest {

    @Test
    void receiveEvent_handlesStockConfirmedAndCreatesPurchase() throws Exception {
        // Mocks
        EventService eventService = mock(EventService.class);
        CartService cartService = mock(CartService.class);
        ProductService productService = mock(ProductService.class);
        var purchaseService = mock(ar.edu.uade.analytics.Service.PurchaseService.class);
        ObjectMapper mapper = new ObjectMapper();

        // Prepare controller and inject mocks via reflection
        KafkaController ctrl = new KafkaController();
        java.lang.reflect.Field f;
        f = KafkaController.class.getDeclaredField("eventService"); f.setAccessible(true); f.set(ctrl, eventService);
        f = KafkaController.class.getDeclaredField("cartService"); f.setAccessible(true); f.set(ctrl, cartService);
        f = KafkaController.class.getDeclaredField("productService"); f.setAccessible(true); f.set(ctrl, productService);
        f = KafkaController.class.getDeclaredField("purchaseService"); f.setAccessible(true); f.set(ctrl, purchaseService);
        f = KafkaController.class.getDeclaredField("objectMapper"); f.setAccessible(true); f.set(ctrl, mapper);

        // Build JSON payload
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "StockConfirmed_CartPurchase");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("cartId", 10);
        // products array
        var arr = mapper.createArrayNode();
        var p = mapper.createObjectNode();
        p.put("productId", 1);
        p.put("stockAfter", 5);
        arr.add(p);
        payload.set("products", arr);
        root.set("payload", payload);

        // cart and product on repository
        Cart c = new Cart(); c.setId(10);
        when(cartService.getCartById(10)).thenReturn(Optional.of(c));
        Product prod = new Product(); prod.setId(1);
        when(productService.getProductById(1)).thenReturn(Optional.of(prod));

        var resp = ctrl.receiveEvent(mapper.writeValueAsString(root));
        assertEquals(org.springframework.http.HttpStatus.OK.value(), resp.getStatusCode().value());

        // verify product stock updated and saved
        verify(productService, times(1)).saveProduct(prod);
        // verify purchase saved
        verify(purchaseService, times(1)).savePurchase(any(ar.edu.uade.analytics.Entity.Purchase.class));
        // verify event saved
        verify(eventService, times(1)).saveEvent(any(Event.class));
    }

    @Test
    void receiveEvent_returnsBadRequest_onMalformedJson() {
        KafkaController ctrl = new KafkaController();
        // inject objectMapper
        try {
            var f = KafkaController.class.getDeclaredField("objectMapper");
            f.setAccessible(true);
            f.set(ctrl, new ObjectMapper());
        } catch (Exception ignored) {}
        var resp = ctrl.receiveEvent("{ not a json }");
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST.value(), resp.getStatusCode().value());
    }
}
