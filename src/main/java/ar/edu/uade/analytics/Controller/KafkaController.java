package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.Event;
import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Service.EventService;
import ar.edu.uade.analytics.Service.CartService;
import ar.edu.uade.analytics.Service.ProductService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@ConditionalOnProperty(value = "analytics.mock.enabled", havingValue = "true", matchIfMissing = true)
@RestController
@RequestMapping("/kafka")
public class KafkaController {

    @Autowired
    EventService eventService;
    @Autowired
    CartService cartService;
    @Autowired
    ProductService productService;
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ar.edu.uade.analytics.Service.PurchaseService purchaseService;

    @PostMapping("/event")
    public ResponseEntity<String> receiveEvent(@RequestBody String eventJson) {
        try {
            JsonNode root = objectMapper.readTree(eventJson);
            String type = root.get("type").asText();
            String payload = root.get("payload").toString();
            LocalDateTime timestamp = root.has("timestamp") ? LocalDateTime.parse(root.get("timestamp").asText()) : LocalDateTime.now();

            // Guardar el evento
            Event event = new Event(type, payload);
            event.setTimestamp(timestamp);
            eventService.saveEvent(event);

            // Procesar el evento si es compra
            if ("StockConfirmed_CartPurchase".equals(type)) {
                JsonNode payloadNode = objectMapper.readTree(payload);
                Integer cartId = payloadNode.get("cartId").asInt();
                Optional<Cart> cartOpt = cartService.getCartById(cartId);
                if (cartOpt.isPresent()) {
                    Cart cart = cartOpt.get();
                    JsonNode productsNode = payloadNode.get("products");
                    for (JsonNode prodNode : productsNode) {
                        Integer productId = prodNode.get("productId").asInt();
                        Integer stockAfter = prodNode.get("stockAfter").asInt();
                        Optional<Product> productOpt = productService.getProductById(productId);
                        if (productOpt.isPresent()) {
                            Product product = productOpt.get();
                            product.setStock(stockAfter);
                            productService.saveProduct(product);
                        }
                    }
                    // Registrar la compra en Purchase
                    ar.edu.uade.analytics.Entity.Purchase purchase = new ar.edu.uade.analytics.Entity.Purchase();
                    purchase.setCart(cart);
                    purchase.setDate(timestamp);
                    purchase.setReservationTime(timestamp);
                    purchase.setStatus(ar.edu.uade.analytics.Entity.Purchase.Status.CONFIRMED);
                    purchaseService.savePurchase(purchase);
                }
            }
            return ResponseEntity.ok("Evento procesado correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error procesando evento: " + e.getMessage());
        }
    }
}
