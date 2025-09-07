package ar.edu.uade.analytics.Controller;


import ar.edu.uade.analytics.Communication.KafkaMockService;
import ar.edu.uade.analytics.Entity.Purchase;
import ar.edu.uade.analytics.Service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchase")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private ar.edu.uade.analytics.Communication.KafkaMockService kafkaMockService;

    @Autowired
    private ar.edu.uade.analytics.Repository.CartRepository cartRepository;

    @Autowired
    private ar.edu.uade.analytics.Repository.ProductRepository productRepository;

    @Autowired
    private ar.edu.uade.analytics.Repository.StockChangeLogRepository stockChangeLogRepository;

    @Autowired
    private ar.edu.uade.analytics.Repository.UserRepository userRepository;

    @GetMapping
    public List<Purchase> getAllPurchases() {
        return purchaseService.findAll();
    }

    @PostMapping("/sync-mock-sale")
    public ResponseEntity<String> syncMockSale() {
        KafkaMockService.SaleEventMock event = kafkaMockService.getSaleEventMock();
        if (event == null || event.payload == null) {
            return ResponseEntity.badRequest().body("No se encontró evento de venta mockeada.");
        }
        // Crear y persistir el Cart primero
        ar.edu.uade.analytics.Entity.Cart cart = new ar.edu.uade.analytics.Entity.Cart();
        cart.setFinalPrice(event.payload.cart.finalPrice);
        cart.setExternalCartId(event.payload.cart.cartId);
        List<ar.edu.uade.analytics.Entity.CartItem> items = new java.util.ArrayList<>();
        for (KafkaMockService.SaleEventCartItemMock itemMock : event.payload.cart.items) {
            ar.edu.uade.analytics.Entity.CartItem item = new ar.edu.uade.analytics.Entity.CartItem();
            item.setQuantity(itemMock.quantity);
            // Buscar el producto en la base de datos
            ar.edu.uade.analytics.Entity.Product product = productRepository.findById(itemMock.productId.intValue()).orElse(null);
            if (product == null) {
                // Si el producto no existe, ignorar el item
                continue;
            }
            // Actualizar el stock del producto y registrar el cambio
            if (product.getStock() != null) {
                int oldStock = product.getStock();
                int nuevoStock = oldStock - itemMock.quantity;
                product.setStock(Math.max(nuevoStock, 0));
                productRepository.save(product);
                // Registrar el cambio en StockChangeLog
                ar.edu.uade.analytics.Entity.StockChangeLog log = new ar.edu.uade.analytics.Entity.StockChangeLog();
                log.setProduct(product);
                log.setOldStock(oldStock);
                log.setNewStock(product.getStock());
                log.setQuantityChanged(itemMock.quantity);
                log.setChangedAt(java.time.LocalDateTime.now());
                log.setReason("Venta");
                stockChangeLogRepository.save(log);
            }
            item.setProduct(product);
            item.setCart(cart);
            items.add(item);
        }
        cart.setItems(items);
        cartRepository.save(cart);
        // Buscar o crear el usuario
        ar.edu.uade.analytics.Entity.User user = userRepository.findByEmail(event.payload.user.email);
        if (user == null) {
            user = new ar.edu.uade.analytics.Entity.User();
            user.setId(event.payload.user.id);
            user.setName(event.payload.user.name);
            user.setEmail(event.payload.user.email);
            user.setAccountActive(true);
            user.setRole("customer");
            userRepository.save(user);
        }
        // Ahora crear y persistir la Purchase
        Purchase purchase = new Purchase();
        purchase.setStatus(Purchase.Status.valueOf(event.payload.status));
        purchase.setDate(java.time.LocalDateTime.parse(event.timestamp.substring(0, 23)));
        purchase.setCart(cart);
        purchase.setUser(user);
        purchaseService.save(purchase);
        return ResponseEntity.ok("Venta mockeada sincronizada correctamente.");
    }

    @PostMapping("/sync-mock-sale-list")
    public ResponseEntity<String> syncMockSaleList() {
        List<KafkaMockService.SaleEventMock> events = kafkaMockService.getSaleEventMockList();
        int success = 0;
        int fail = 0;
        for (KafkaMockService.SaleEventMock event : events) {
            if (event == null || event.payload == null) {
                fail++;
                continue;
            }
            // Crear y persistir el Cart primero
            ar.edu.uade.analytics.Entity.Cart cart = new ar.edu.uade.analytics.Entity.Cart();
            cart.setFinalPrice(event.payload.cart.finalPrice);
            cart.setExternalCartId(event.payload.cart.cartId);
            List<ar.edu.uade.analytics.Entity.CartItem> items = new java.util.ArrayList<>();
            for (KafkaMockService.SaleEventCartItemMock itemMock : event.payload.cart.items) {
                ar.edu.uade.analytics.Entity.CartItem item = new ar.edu.uade.analytics.Entity.CartItem();
                item.setQuantity(itemMock.quantity);
                // Buscar el producto en la base de datos
                ar.edu.uade.analytics.Entity.Product product = productRepository.findById(itemMock.productId.intValue()).orElse(null);
                if (product == null) {
                    continue;
                }
                if (product.getStock() != null) {
                    int oldStock = product.getStock();
                    int nuevoStock = oldStock - itemMock.quantity;
                    product.setStock(Math.max(nuevoStock, 0));
                    productRepository.save(product);
                    ar.edu.uade.analytics.Entity.StockChangeLog log = new ar.edu.uade.analytics.Entity.StockChangeLog();
                    log.setProduct(product);
                    log.setOldStock(oldStock);
                    log.setNewStock(product.getStock());
                    log.setQuantityChanged(itemMock.quantity);
                    log.setChangedAt(java.time.LocalDateTime.now());
                    log.setReason("Venta");
                    stockChangeLogRepository.save(log);
                }
                item.setProduct(product);
                item.setCart(cart);
                items.add(item);
            }
            cart.setItems(items);
            cartRepository.save(cart);
            ar.edu.uade.analytics.Entity.User user = userRepository.findByEmail(event.payload.user.email);
            if (user == null) {
                user = new ar.edu.uade.analytics.Entity.User();
                user.setId(event.payload.user.id);
                user.setName(event.payload.user.name);
                user.setEmail(event.payload.user.email);
                user.setAccountActive(true);
                user.setRole("customer");
                userRepository.save(user);
            }
            Purchase purchase = new Purchase();
            purchase.setStatus(Purchase.Status.valueOf(event.payload.status));
            purchase.setDate(java.time.LocalDateTime.parse(event.timestamp.substring(0, 23)));
            purchase.setCart(cart);
            purchase.setUser(user);
            purchaseService.save(purchase);
            success++;
        }
        return ResponseEntity.ok("Ventas mockeadas sincronizadas: " + success + ", fallidas: " + fail);
    }

    @Transactional(timeout = 120)
    @PostMapping("/sync-mock-sale-list-kafka")
    public ResponseEntity<String> syncMockSaleListKafka() {
        List<KafkaMockService.SaleEventMock> events = kafkaMockService.getSaleEventMockList();
        int success = 0;
        int fail = 0;
        for (KafkaMockService.SaleEventMock event : events) {
            if (event == null || event.payload == null) {
                fail++;
                continue;
            }
            // Buscar o crear el usuario ANTES del Cart (para setearlo en el Cart)
            ar.edu.uade.analytics.Entity.User user = userRepository.findByEmail(event.payload.user.email);
            if (user == null) {
                user = new ar.edu.uade.analytics.Entity.User();
                // No setear el id manualmente, dejar que JPA lo maneje
                user.setName(event.payload.user.name);
                user.setEmail(event.payload.user.email);
                user.setAccountActive(true);
                user.setRole("customer");
                userRepository.save(user);
                // Volver a buscar el usuario para obtener la instancia administrada
                user = userRepository.findByEmail(event.payload.user.email);
            }
            // Crear y persistir el Cart primero
            ar.edu.uade.analytics.Entity.Cart cart = new ar.edu.uade.analytics.Entity.Cart();
            cart.setFinalPrice(event.payload.cart.finalPrice);
            cart.setExternalCartId(event.payload.cart.cartId);
            cart.setUser(user); // Asignar el usuario al cart
            List<ar.edu.uade.analytics.Entity.CartItem> items = new java.util.ArrayList<>();
            for (KafkaMockService.SaleEventCartItemMock itemMock : event.payload.cart.items) {
                ar.edu.uade.analytics.Entity.CartItem item = new ar.edu.uade.analytics.Entity.CartItem();
                item.setQuantity(itemMock.quantity);
                // Buscar el producto en la base de datos
                ar.edu.uade.analytics.Entity.Product product = productRepository.findById(itemMock.productId.intValue()).orElse(null);
                if (product == null) {
                    continue;
                }
                if (product.getStock() != null) {
                    int oldStock = product.getStock();
                    int nuevoStock = oldStock - itemMock.quantity;
                    product.setStock(Math.max(nuevoStock, 0));
                    productRepository.save(product);
                    ar.edu.uade.analytics.Entity.StockChangeLog log = new ar.edu.uade.analytics.Entity.StockChangeLog();
                    log.setProduct(product);
                    log.setOldStock(oldStock);
                    log.setNewStock(product.getStock());
                    log.setQuantityChanged(itemMock.quantity);
                    log.setChangedAt(java.time.LocalDateTime.now());
                    log.setReason("Venta");
                    stockChangeLogRepository.save(log);
                }
                item.setProduct(product);
                item.setCart(cart);
                items.add(item);
            }
            cart.setItems(items);
            cartRepository.save(cart);
            // Ahora crear y persistir la Purchase
            Purchase purchase = new Purchase();
            purchase.setStatus(Purchase.Status.valueOf(event.payload.status));
            purchase.setDate(java.time.LocalDateTime.parse(event.timestamp.substring(0, 23)));
            purchase.setCart(cart);
            purchase.setUser(user);
            purchaseService.save(purchase);
            success++;
        }
        return ResponseEntity.ok("Ventas mockeadas sincronizadas (Kafka): " + success + ", fallidas: " + fail);
    }
}
