package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Service.CartService;
import ar.edu.uade.analytics.Service.EventService;
import ar.edu.uade.analytics.Service.ProductService;
import ar.edu.uade.analytics.Service.PurchaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaControllerTest {

    @Mock EventService eventService;
    @Mock CartService cartService;
    @Mock ProductService productService;
    @Mock PurchaseService purchaseService;

    KafkaController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new KafkaController();
        java.lang.reflect.Field f;
        f = KafkaController.class.getDeclaredField("eventService"); f.setAccessible(true); f.set(controller, eventService);
        f = KafkaController.class.getDeclaredField("cartService"); f.setAccessible(true); f.set(controller, cartService);
        f = KafkaController.class.getDeclaredField("productService"); f.setAccessible(true); f.set(controller, productService);
        f = KafkaController.class.getDeclaredField("purchaseService"); f.setAccessible(true); f.set(controller, purchaseService);
        f = KafkaController.class.getDeclaredField("objectMapper"); f.setAccessible(true); f.set(controller, new ObjectMapper());
    }

    @Test
    void receiveEvent_malformedJson_returnsBadRequest() {
        String badJson = "{ not json }";
        ResponseEntity<String> resp = controller.receiveEvent(badJson);
        assertEquals(400, resp.getStatusCode().value());
    }


}

