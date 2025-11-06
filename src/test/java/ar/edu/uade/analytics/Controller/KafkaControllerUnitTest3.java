package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Service.CartService;
import ar.edu.uade.analytics.Service.EventService;
import ar.edu.uade.analytics.Service.ProductService;
import ar.edu.uade.analytics.Service.PurchaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaControllerAdditionalTest {

    // reflection helper
    static void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void receiveEvent_nonPurchase_eventSavedAndIgnored() throws Exception {
        KafkaController controller = new KafkaController();
        EventService eventService = mock(EventService.class);
        CartService cartService = mock(CartService.class);
        ProductService productService = mock(ProductService.class);
        PurchaseService purchaseService = mock(PurchaseService.class);
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        root.put("type", "SOME_OTHER_EVENT");
        root.set("payload", mapper.createObjectNode().put("x", "y"));
        root.put("timestamp", LocalDateTime.now().toString());

        setField(controller, "eventService", eventService);
        setField(controller, "cartService", cartService);
        setField(controller, "productService", productService);
        setField(controller, "purchaseService", purchaseService);
        setField(controller, "objectMapper", mapper);

        ResponseEntity<String> resp = controller.receiveEvent(mapper.writeValueAsString(root));
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        // event should be saved, but no purchase or product update
        verify(eventService, times(1)).saveEvent(any());
        verify(purchaseService, never()).savePurchase(any());
        verify(productService, never()).saveProduct(any());
    }

    @Test
    void receiveEvent_malformedJson_returnsBadRequest() throws Exception {
        KafkaController controller = new KafkaController();
        EventService eventService = mock(EventService.class);
        setField(controller, "eventService", eventService);
        setField(controller, "cartService", mock(CartService.class));
        setField(controller, "productService", mock(ProductService.class));
        setField(controller, "purchaseService", mock(PurchaseService.class));
        setField(controller, "objectMapper", new ObjectMapper());

        String badJson = "{ this is not valid json";
        ResponseEntity<String> resp = controller.receiveEvent(badJson);
        assertTrue(resp.getStatusCode().is4xxClientError());
        // invalid JSON -> eventService.saveEvent should NOT be called
        verify(eventService, never()).saveEvent(any());
    }
}
