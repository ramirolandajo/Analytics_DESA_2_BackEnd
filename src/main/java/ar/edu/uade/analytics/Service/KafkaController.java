package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Event;
import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Service.EventService;
import ar.edu.uade.analytics.Service.CartService;
import ar.edu.uade.analytics.Service.ProductService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

public class KafkaController {

    @Autowired
    private EventService eventService;
    @Autowired
    private CartService cartService;
    @Autowired
    private ProductService productService;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PurchaseService purchaseService;

}
