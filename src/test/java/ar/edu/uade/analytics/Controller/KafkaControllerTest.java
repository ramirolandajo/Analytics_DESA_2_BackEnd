package ar.edu.uade.analytics.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SuppressWarnings("deprecation")
@WebMvcTest(KafkaController.class)
public class KafkaControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ar.edu.uade.analytics.Service.EventService eventService;
    @MockBean
    private ar.edu.uade.analytics.Service.CartService cartService;
    @MockBean
    private ar.edu.uade.analytics.Service.ProductService productService;
    @MockBean
    private ar.edu.uade.analytics.Service.PurchaseService purchaseService;

    @Test
    void contextLoads() {
        // Test de carga de contexto
    }

    @Test
    void testReceiveEvent_returnsOk() throws Exception {
        String eventJson = "{" +
                "\"type\":\"StockConfirmed_CartPurchase\"," +
                "\"payload\":{\"cartId\":123}," +
                "\"timestamp\":\"2025-09-06T12:00:00\"}";
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/kafka/event")
                        .contentType("application/json")
                        .content(eventJson);
        mockMvc.perform(request)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testReceiveEvent_stockConfirmedCartPurchase_success() throws Exception {
        String eventJson = "{" +
                "\"type\":\"StockConfirmed_CartPurchase\"," +
                "\"payload\":{\"cartId\":123,\"products\":[{\"productId\":1,\"stockAfter\":5}]}," +
                "\"timestamp\":\"2025-09-06T12:00:00\"}";
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/kafka/event")
                        .contentType("application/json")
                        .content(eventJson);
        // Mock cart and product
        ar.edu.uade.analytics.Entity.Cart cart = new ar.edu.uade.analytics.Entity.Cart();
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        org.mockito.Mockito.when(cartService.getCartById(123)).thenReturn(java.util.Optional.of(cart));
        org.mockito.Mockito.when(productService.getProductById(1)).thenReturn(java.util.Optional.of(product));
        mockMvc.perform(request)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Evento procesado correctamente")));
    }

    @Test
    void testReceiveEvent_typeNotCartPurchase() throws Exception {
        String eventJson = "{" +
                "\"type\":\"OtherEvent\"," +
                "\"payload\":{\"foo\":1}," +
                "\"timestamp\":\"2025-09-06T12:00:00\"}";
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/kafka/event")
                        .contentType("application/json")
                        .content(eventJson);
        mockMvc.perform(request)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Evento procesado correctamente")));
    }

    @Test
    void testReceiveEvent_cartNotFound() throws Exception {
        String eventJson = "{" +
                "\"type\":\"StockConfirmed_CartPurchase\"," +
                "\"payload\":{\"cartId\":999,\"products\":[{\"productId\":1,\"stockAfter\":5}]}," +
                "\"timestamp\":\"2025-09-06T12:00:00\"}";
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/kafka/event")
                        .contentType("application/json")
                        .content(eventJson);
        org.mockito.Mockito.when(cartService.getCartById(999)).thenReturn(java.util.Optional.empty());
        mockMvc.perform(request)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Evento procesado correctamente")));
    }

    @Test
    void testReceiveEvent_productNotFound() throws Exception {
        String eventJson = "{" +
                "\"type\":\"StockConfirmed_CartPurchase\"," +
                "\"payload\":{\"cartId\":123,\"products\":[{\"productId\":1,\"stockAfter\":5}]}," +
                "\"timestamp\":\"2025-09-06T12:00:00\"}";
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/kafka/event")
                        .contentType("application/json")
                        .content(eventJson);
        ar.edu.uade.analytics.Entity.Cart cart = new ar.edu.uade.analytics.Entity.Cart();
        org.mockito.Mockito.when(cartService.getCartById(123)).thenReturn(java.util.Optional.of(cart));
        org.mockito.Mockito.when(productService.getProductById(1)).thenReturn(java.util.Optional.empty());
        mockMvc.perform(request)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Evento procesado correctamente")));
    }

    @Test
    void testReceiveEvent_invalidJson() throws Exception {
        String eventJson = "{invalid json}";
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/kafka/event")
                        .contentType("application/json")
                        .content(eventJson);
        mockMvc.perform(request)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Error procesando evento")));
    }

    @Test
    void testReceiveEvent_payloadMissingFields() throws Exception {
        String eventJson = "{" +
                "\"type\":\"StockConfirmed_CartPurchase\"," +
                "\"payload\":{}," +
                "\"timestamp\":\"2025-09-06T12:00:00\"}";
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/kafka/event")
                        .contentType("application/json")
                        .content(eventJson);
        mockMvc.perform(request)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Error procesando evento")));
    }

    @Test
    void testReceiveEvent_eventServiceThrowsException() throws Exception {
        String eventJson = "{" +
                "\"type\":\"OtherEvent\"," +
                "\"payload\":{\"foo\":1}," +
                "\"timestamp\":\"2025-09-06T12:00:00\"}";
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/kafka/event")
                        .contentType("application/json")
                        .content(eventJson);
        org.mockito.Mockito.doThrow(new RuntimeException("DB error")).when(eventService).saveEvent(org.mockito.Mockito.any());
        mockMvc.perform(request)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Error procesando evento")));
    }

    @Test
    void testReceiveEvent_missingTypeField() throws Exception {
        String eventJson = "{" +
                "\"payload\":{\"cartId\":123}," +
                "\"timestamp\":\"2025-09-06T12:00:00\"}";
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/kafka/event")
                        .contentType("application/json")
                        .content(eventJson);
        mockMvc.perform(request)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Error procesando evento")));
    }

    @Test
    void testReceiveEvent_missingTimestampField() throws Exception {
        String eventJson = "{" +
                "\"type\":\"StockConfirmed_CartPurchase\"," +
                "\"payload\":{\"cartId\":123,\"products\":[{\"productId\":1,\"stockAfter\":5}]}" +
                "}";
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/kafka/event")
                        .contentType("application/json")
                        .content(eventJson);
        ar.edu.uade.analytics.Entity.Cart cart = new ar.edu.uade.analytics.Entity.Cart();
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        org.mockito.Mockito.when(cartService.getCartById(123)).thenReturn(java.util.Optional.of(cart));
        org.mockito.Mockito.when(productService.getProductById(1)).thenReturn(java.util.Optional.of(product));
        mockMvc.perform(request)
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Evento procesado correctamente")));
    }
}
