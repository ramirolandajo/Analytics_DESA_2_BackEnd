package ar.edu.uade.analytics.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartItemController.class)
public class CartItemControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // Test de carga de contexto
    }
}

