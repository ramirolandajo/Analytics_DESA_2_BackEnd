package ar.edu.uade.analytics.Communication;

import ar.edu.uade.analytics.DTO.BrandDTO;
import ar.edu.uade.analytics.DTO.CategoryDTO;
import ar.edu.uade.analytics.DTO.ProductDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@ConditionalOnProperty(value = "analytics.mock.enabled", havingValue = "true", matchIfMissing = true)
@Service
public class KafkaMockService {

    // Mock para edición simple de producto
    public EditProductSimpleMessage getEditProductMockSimple() {
        return new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(
                        1021, // productCode
                        999, // nuevo stock
                        199.99f // nuevo precio
                ),
                java.time.LocalDateTime.now().toString()
        );
    }
    public static class EditProductSimpleMessage {
        public String type;
        public EditProductSimplePayload payload;
        public String timestamp;
        public EditProductSimpleMessage(String type, EditProductSimplePayload payload, String timestamp) {
            this.type = type;
            this.payload = payload;
            this.timestamp = timestamp;
        }
    }
    public static class EditProductSimplePayload {
        public Integer productCode;
        public Integer stock;
        public Float price;
        public EditProductSimplePayload(Integer productCode, Integer stock, Float price) {
            this.productCode = productCode;
            this.stock = stock;
            this.price = price;
        }
    }
    // Mock para edición completa de producto
    public EditProductFullMessage getEditProductMockFull() {
        return new EditProductFullMessage(
                "EditProductFull",
                new EditProductFullPayload(
                        1234,
                        "Producto editado desde mock",
                        "Descripción editada desde mock",
                        123.45f,
                        888,
                        List.of("https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=1400&auto=format&fit=crop"),
                        new BrandDTO(1L, "Samsung", true),
                        List.of(new CategoryDTO(1L, "Celulares", true)),
                        true,
                        false,
                        true,
                        false,
                        true,
                        4.5f,
                        5.0f,
                        130.0f,
                        30
                ),
                java.time.LocalDateTime.now().toString()
        );
    }
    public static class EditProductFullMessage {
        public String type;
        public EditProductFullPayload payload;
        public String timestamp;
        public EditProductFullMessage(String type, EditProductFullPayload payload, String timestamp) {
            this.type = type;
            this.payload = payload;
            this.timestamp = timestamp;
        }
    }
    public static class EditProductFullPayload extends ProductDTO {
        // Keep same parameter order as ProductDTO: active, calification, discount, priceUnit
        public EditProductFullPayload(Integer id, String title, String description, Float price, Integer stock, List<String> mediaSrc, BrandDTO brand, List<CategoryDTO> categories, Boolean isNew, Boolean isBestseller, Boolean isFeatured, Boolean hero, Boolean active, Float calification, Float discount, Float priceUnit, Integer productCode) {
            super(Long.valueOf(id), title, description, price, stock, mediaSrc, brand, categories, isNew, isBestseller, isFeatured, hero, active, calification, discount, priceUnit, productCode);
        }
    }
    // Mock para agregar producto
    public AddProductMessage getAddProductMock() {
        return new AddProductMessage(
                "AddProduct",
                new AddProductPayload(
                        new ProductDTO(
                                99L,
                                "Nuevo Producto Mock",
                                "Descripción del producto mockeado",
                                500.0f,
                                50,
                                List.of("https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=1400&auto=format&fit=crop"),
                                new BrandDTO(1L, "Samsung", true),
                                List.of(new CategoryDTO(1L, "Celulares", true)),
                                true,
                                false,
                                true,
                                true,
                                true,
                                0f,
                                10.0f,
                                450.0f,
                                9999
                        )
                ),
                java.time.LocalDateTime.now().toString()
        );
    }
    public static class AddProductMessage {
        public String type;
        public AddProductPayload payload;
        public String timestamp;
        public AddProductMessage(String type, AddProductPayload payload, String timestamp) {
            this.type = type;
            this.payload = payload;
            this.timestamp = timestamp;
        }
    }
    public static class AddProductPayload {
        public ProductDTO product;
        public AddProductPayload(ProductDTO product) {
            this.product = product;
        }
    }

    // Mock para activar producto
    public ActivateProductMessage getActivateProductMock() {
        return new ActivateProductMessage(
                "ActivateProduct",
                new ActivateProductPayload(29L),
                java.time.LocalDateTime.now().toString()
        );
    }
    public static class ActivateProductMessage {
        public String type;
        public ActivateProductPayload payload;
        public String timestamp;
        public ActivateProductMessage(String type, ActivateProductPayload payload, String timestamp) {
            this.type = type;
            this.payload = payload;
            this.timestamp = timestamp;
        }
    }
    public static class ActivateProductPayload {
        public Long id;
        public ActivateProductPayload(Long id) {
            this.id = id;
        }
    }

    // Mock para desactivar producto
    public DeactivateProductMessage getDeactivateProductMock() {
        return new DeactivateProductMessage(
                "DeactivateProduct",
                new DeactivateProductPayload(29L),
                java.time.LocalDateTime.now().toString()
        );
    }
    public static class DeactivateProductMessage {
        public String type;
        public DeactivateProductPayload payload;
        public String timestamp;
        public DeactivateProductMessage(String type, DeactivateProductPayload payload, String timestamp) {
            this.type = type;
            this.payload = payload;
            this.timestamp = timestamp;
        }
    }
    public static class DeactivateProductPayload {
        public Long id;
        public DeactivateProductPayload(Long id) {
            this.id = id;
        }
    }
    // Mock para review de producto
    public ProductReviewMockMessage getProductReviewMock() {
        return new ProductReviewMockMessage(
                "ProductReview",
                new ProductReviewMockPayload(
                        11, // productId
                        4.5f, // calification
                        "Excelente producto, muy recomendable!"
                ),
                java.time.LocalDateTime.now().toString()
        );
    }
    public static class ProductReviewMockMessage {
        public String type;
        public ProductReviewMockPayload payload;
        public String timestamp;
        public ProductReviewMockMessage(String type, ProductReviewMockPayload payload, String timestamp) {
            this.type = type;
            this.payload = payload;
            this.timestamp = timestamp;
        }
    }
    public static class ProductReviewMockPayload {
        public Integer productId;
        public float calification;
        public String description;
        public ProductReviewMockPayload(Integer productId, float calification, String description) {
            this.productId = productId;
            this.calification = calification;
            this.description = description;
        }
    }
    // Mock para vistas diarias de productos
    public DailyProductViewsMessage getDailyProductViewsMock() {
        return new DailyProductViewsMessage(
                "DAILY_PRODUCT_VIEWS",
                new DailyProductViewsPayload(List.of(
                        new ProductViewDTO(11, "Smartphone", 1001),
                        new ProductViewDTO(12, "Laptop", 1002),
                        new ProductViewDTO(13, "Auriculares Bluetooth", 1003),
                        new ProductViewDTO(14, "Smartwatch", 1004),
                        new ProductViewDTO(15, "Tablet", 1005),
                        new ProductViewDTO(16, "Monitor LED", 1006),
                        new ProductViewDTO(17, "Impresora Láser", 1007),
                        new ProductViewDTO(18, "Mouse inalámbrico", 1008),
                        new ProductViewDTO(19, "Teclado mecánico", 1009),
                        new ProductViewDTO(20, "Notebook Gamer", 1010),
                        new ProductViewDTO(11, "Smartphone", 1001),
                        new ProductViewDTO(12, "Laptop", 1002),
                        new ProductViewDTO(13, "Auriculares Bluetooth", 1003),
                        new ProductViewDTO(14, "Smartwatch", 1004),
                        new ProductViewDTO(15, "Tablet", 1005),
                        new ProductViewDTO(16, "Monitor LED", 1006),
                        new ProductViewDTO(17, "Impresora Láser", 1007),
                        new ProductViewDTO(18, "Mouse inalámbrico", 1008),
                        new ProductViewDTO(19, "Teclado mecánico", 1009),
                        new ProductViewDTO(20, "Notebook Gamer", 1010)
                )),
                "2025-09-02T09:00:00.394725500"
        );
    }
    public static class DailyProductViewsMessage {
        public String type;
        public DailyProductViewsPayload payload;
        public String timestamp;
        public DailyProductViewsMessage(String type, DailyProductViewsPayload payload, String timestamp) {
            this.type = type;
            this.payload = payload;
            this.timestamp = timestamp;
        }
    }
    public static class DailyProductViewsPayload {
        public List<ProductViewDTO> products;
        public DailyProductViewsPayload(List<ProductViewDTO> products) {
            this.products = products;
        }
    }
    public static class ProductViewDTO {
        public Integer id;
        public String nombre;
        public Integer productCode;
        public ProductViewDTO(Integer id, String nombre, Integer productCode) {
            this.id = id;
            this.nombre = nombre;
            this.productCode = productCode;
        }
    }
    // Mock para evento de venta - usuario 3 (inventado)
    public SaleEventMock getSaleEventMock() {
        SaleEventCartItemMock item1 = new SaleEventCartItemMock(1, 21, 800.0f, "Tablet Pro 10\"");
        SaleEventCartItemMock item2 = new SaleEventCartItemMock(1, 22, 180.0f, "Disco SSD 1TB");
        SaleEventCartMock cart = new SaleEventCartMock(36, 980.0f, List.of(item1, item2));
        SaleEventUserMock user = new SaleEventUserMock(3, "Lucía", "lucia@example.com");
        SaleEventPayloadMock payload = new SaleEventPayloadMock(36, user, cart, "CONFIRMED");
        return new SaleEventMock(
                "StockConfirmed_CartPurchase",
                payload,
                "2025-09-16T09:00:00.000000000"
        );
    }

    // Mock para evento de venta - usuario 4 (inventado)
    public SaleEventMock getSaleEventMock13() {
        SaleEventCartItemMock item1 = new SaleEventCartItemMock(2, 23, 210.0f, "Smartwatch Fitness");
        SaleEventCartItemMock item2 = new SaleEventCartItemMock(1, 24, 350.0f, "Cámara de acción 4K");
        SaleEventCartMock cart = new SaleEventCartMock(37, 770.0f, List.of(item1, item2));
        SaleEventUserMock user = new SaleEventUserMock(4, "Javier", "javier@example.com");
        SaleEventPayloadMock payload = new SaleEventPayloadMock(37, user, cart, "CONFIRMED");
        return new SaleEventMock(
                "StockConfirmed_CartPurchase",
                payload,
                "2025-09-17T14:00:00.000000000"
        );
    }

    // Mock para evento de venta - usuario 1, productos variados
    public SaleEventMock getSaleEventMock14() {
        SaleEventCartItemMock item1 = new SaleEventCartItemMock(1, 14, 200.0f, "Smartwatch");
        SaleEventCartItemMock item2 = new SaleEventCartItemMock(1, 25, 600.0f, "Monitor LED 27\"");
        SaleEventCartMock cart = new SaleEventCartMock(38, 800.0f, List.of(item1, item2));
        SaleEventUserMock user = new SaleEventUserMock(1, "Enzo", "enzoandreaasplanatti@gmail.com");
        SaleEventPayloadMock payload = new SaleEventPayloadMock(38, user, cart, "CONFIRMED");
        return new SaleEventMock(
                "StockConfirmed_CartPurchase",
                payload,
                "2025-09-18T16:00:00.000000000"
        );
    }

    // Mock para evento de venta - usuario 2, productos variados
    public SaleEventMock getSaleEventMock15() {
        SaleEventCartItemMock item1 = new SaleEventCartItemMock(1, 16, 250.0f, "Monitor LED");
        SaleEventCartItemMock item2 = new SaleEventCartItemMock(1, 18, 35.0f, "Mouse inalámbrico");
        SaleEventCartItemMock item3 = new SaleEventCartItemMock(1, 19, 70.0f, "Teclado mecánico");
        SaleEventCartMock cart = new SaleEventCartMock(39, 355.0f, List.of(item1, item2, item3));
        SaleEventUserMock user = new SaleEventUserMock(2, "María", "maria@example.com");
        SaleEventPayloadMock payload = new SaleEventPayloadMock(39, user, cart, "CONFIRMED");
        return new SaleEventMock(
                "StockConfirmed_CartPurchase",
                payload,
                "2025-09-19T18:00:00.000000000"
        );
    }


    public static class SaleEventMock {
        public String type;
        public SaleEventPayloadMock payload;
        public String timestamp;
        public SaleEventMock(String type, SaleEventPayloadMock payload, String timestamp) {
            this.type = type;
            this.payload = payload;
            this.timestamp = timestamp;
        }
    }
    public static class SaleEventPayloadMock {
        public Integer purchaseId;
        public SaleEventUserMock user;
        public SaleEventCartMock cart;
        public String status;
        public SaleEventPayloadMock(Integer purchaseId, SaleEventUserMock user, SaleEventCartMock cart, String status) {
            this.purchaseId = purchaseId;
            this.user = user;
            this.cart = cart;
            this.status = status;
        }
    }
    public static class SaleEventUserMock {
        public Integer id;
        public String name;
        public String email;
        public SaleEventUserMock(Integer id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }
    public static class SaleEventCartMock {
        public Integer cartId;
        public Float finalPrice;
        public List<SaleEventCartItemMock> items;
        public SaleEventCartMock(Integer cartId, Float finalPrice, List<SaleEventCartItemMock> items) {
            this.cartId = cartId;
            this.finalPrice = finalPrice;
            this.items = items;
        }
    }
    public static class SaleEventCartItemMock {
        public Integer quantity;
        public Integer productId;
        public Float price;
        public String title;
        public SaleEventCartItemMock(Integer quantity, Integer productId, Float price, String title) {
            this.quantity = quantity;
            this.productId = productId;
            this.price = price;
            this.title = title;
        }
    }
    // Mock para producto favorito
    public AddFavouriteProductMessage getAddFavouriteProductMock() {
        return new AddFavouriteProductMessage(
                "ADD_FAVOURITE_PRODUCT",
                new AddFavouriteProductPayload(1006, 41, "Tablet"),
                "2025-09-04T08:04:39.410475900"
        );
    }
    public static class AddFavouriteProductMessage {
        public String type;
        public AddFavouriteProductPayload payload;
        public String timestamp;
        public AddFavouriteProductMessage(String type, AddFavouriteProductPayload payload, String timestamp) {
            this.type = type;
            this.payload = payload;
            this.timestamp = timestamp;
        }
    }
    public static class AddFavouriteProductPayload {
        private Integer productCode;
        private Integer id;
        private String nombre;
        public AddFavouriteProductPayload(Integer productCode, Integer id, String nombre) {
            this.productCode = productCode;
            this.id = id;
            this.nombre = nombre;
        }
        public Integer getProductCode() { return productCode; }
        public Integer getId() { return id; }
        public String getNombre() { return nombre; }
    }
    // Mock para listado de productos favoritos
    public List<AddFavouriteProductMessage> getAddFavouriteProductsMock() {
        return List.of(
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1003, 13, "Auriculares Bluetooth"), "2025-09-04T08:06:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1003, 13, "Auriculares Bluetooth"), "2025-09-04T08:06:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1004, 14, "Smartwatch"), "2025-09-04T08:07:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1004, 14, "Smartwatch"), "2025-09-04T08:07:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1004, 14, "Smartwatch"), "2025-09-04T08:07:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1001, 11, "Smartphone"), "2025-09-04T08:04:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1002, 12, "Laptop"), "2025-09-04T08:05:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1002, 12, "Laptop"), "2025-09-04T08:05:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1005, 15, "Tablet"), "2025-09-04T08:08:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1005, 15, "Tablet"), "2025-09-04T08:08:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1006, 16, "Monitor LED"), "2025-09-04T08:09:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1007, 17, "Impresora Láser"), "2025-09-04T08:10:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1008, 18, "Mouse inalámbrico"), "2025-09-04T08:11:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1008, 18, "Mouse inalámbrico"), "2025-09-04T08:11:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1009, 19, "Teclado mecánico"), "2025-09-04T08:12:39.410475900"),
                new AddFavouriteProductMessage("ADD_FAVOURITE_PRODUCT", new AddFavouriteProductPayload(1010, 20, "Notebook Gamer"), "2025-09-04T08:13:39.410475900")
        );
    }

    // Mock para listado de reviews de productos
    public List<ProductReviewMockMessage> getProductReviewMockList() {
        return List.of(
            // Producto 11
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(11, 4.5f, "Excelente producto, muy recomendable!"), java.time.LocalDateTime.now().toString()),
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(11, 2.0f, "No funcionó como esperaba."), java.time.LocalDateTime.now().toString()),
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(11, 3.5f, "Buen diseño pero caro."), java.time.LocalDateTime.now().toString()),
//            // Producto 12
//            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(12, 5.0f, "La mejor compra que hice este año."), java.time.LocalDateTime.now().toString()),
//            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(12, 4.2f, "Muy rápido y eficiente."), java.time.LocalDateTime.now().toString()),
//            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(12, 1.8f, "Se rompió a los dos meses."), java.time.LocalDateTime.now().toString()),
//            // Producto 13
//            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(13, 3.9f, "Sonido aceptable, batería regular."), java.time.LocalDateTime.now().toString()),
//            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(13, 4.7f, "Muy cómodos para usar todo el día."), java.time.LocalDateTime.now().toString()),
//            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(13, 2.5f, "No se conectan bien al bluetooth."), java.time.LocalDateTime.now().toString()),
//            // Producto 14
//            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(14, 4.0f, "Muy útil para el día a día."), java.time.LocalDateTime.now().toString()),
//            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(14, 2.8f, "La batería dura poco."), java.time.LocalDateTime.now().toString()),
//            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(14, 3.6f, "Buen precio, cumple su función."), java.time.LocalDateTime.now().toString()),
//            // Producto 15
//            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(15, 2.5f, "Esperaba m��s calidad."), java.time.LocalDateTime.now().toString()),
//            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(15, 3.9f, "Ideal para estudiar."), java.time.LocalDateTime.now().toString()),
//            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(15, 4.8f, "Muy buena pantalla y batería."), java.time.LocalDateTime.now().toString()),
//            // Producto 16
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(16, 4.8f, "Excelente definición de imagen."), java.time.LocalDateTime.now().toString()),
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(16, 3.5f, "Buen monitor pero el soporte es débil."), java.time.LocalDateTime.now().toString()),
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(16, 2.2f, "Se apagó solo varias veces."), java.time.LocalDateTime.now().toString()),
            // Producto 17
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(17, 4.1f, "Imprime rápido y sin problemas."), java.time.LocalDateTime.now().toString()),
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(17, 2.0f, "Se atasca seguido."), java.time.LocalDateTime.now().toString()),
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(17, 3.7f, "Fácil de instalar y configurar."), java.time.LocalDateTime.now().toString()),
            // Producto 18
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(18, 4.6f, "Muy cómodo y preciso."), java.time.LocalDateTime.now().toString()),
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(18, 3.2f, "La batería podría durar más."), java.time.LocalDateTime.now().toString()),
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(18, 1.5f, "Se rompió el botón a la semana."), java.time.LocalDateTime.now().toString()),
            // Producto 19
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(19, 5.0f, "Teclado espectacular para gaming."), java.time.LocalDateTime.now().toString()),
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(19, 4.3f, "Las teclas son muy suaves."), java.time.LocalDateTime.now().toString()),
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(19, 2.9f, "El RGB no funciona bien."), java.time.LocalDateTime.now().toString()),
            // Producto 20
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(20, 4.9f, "Notebook muy potente."), java.time.LocalDateTime.now().toString()),
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(20, 3.7f, "Se calienta bastante."), java.time.LocalDateTime.now().toString()),
            new ProductReviewMockMessage("ProductReview", new ProductReviewMockPayload(20, 2.0f, "El cargador dejó de funcionar."), java.time.LocalDateTime.now().toString())
        );
    }

    // Devuelve un listado de eventos de venta mockeados
    public List<SaleEventMock> getSaleEventMockList() {
        return List.of(
            getSaleEventMock(),
            getSaleEventMock13(),
            getSaleEventMock14(),
            getSaleEventMock15()
        );
    }
    // Devuelve una lista de eventos mock de edición simple de producto (cambios de stock)
    public List<EditProductSimpleMessage> getEditProductMockSimpleList() {
        return List.of(
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1001, 100, 350f),
                java.time.LocalDateTime.now().minusMinutes(25).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1002, 35, 1200f),
                java.time.LocalDateTime.now().minusMinutes(24).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1003, 10, 80f),
                java.time.LocalDateTime.now().minusMinutes(23).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1004, 12, 200f),
                java.time.LocalDateTime.now().minusMinutes(22).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1005, 26, 400f),
                java.time.LocalDateTime.now().minusMinutes(21).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1006, 18, 250f),
                java.time.LocalDateTime.now().minusMinutes(20).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1007, 10, 180f),
                java.time.LocalDateTime.now().minusMinutes(19).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1008, 60, 35f),
                java.time.LocalDateTime.now().minusMinutes(18).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1009, 38, 70f),
                java.time.LocalDateTime.now().minusMinutes(17).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1010, 2, 2200f),
                java.time.LocalDateTime.now().minusMinutes(16).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1011, 35, 180f),
                java.time.LocalDateTime.now().minusMinutes(15).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1012, 80, 90f),
                java.time.LocalDateTime.now().minusMinutes(14).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1013, 7, 1500f),
                java.time.LocalDateTime.now().minusMinutes(13).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1014, 15, 2200f),
                java.time.LocalDateTime.now().minusMinutes(12).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1015, 22, 350f),
                java.time.LocalDateTime.now().minusMinutes(11).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1016, 60, 120f),
                java.time.LocalDateTime.now().minusMinutes(10).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1017, 18, 250f),
                java.time.LocalDateTime.now().minusMinutes(9).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1018, 40, 70f),
                java.time.LocalDateTime.now().minusMinutes(8).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1019, 25, 95f),
                java.time.LocalDateTime.now().minusMinutes(7).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1020, 10, 2200f),
                java.time.LocalDateTime.now().minusMinutes(6).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1021, 30, 800f),
                java.time.LocalDateTime.now().minusMinutes(5).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1022, 45, 180f),
                java.time.LocalDateTime.now().minusMinutes(4).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1023, 28, 210f),
                java.time.LocalDateTime.now().minusMinutes(3).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1024, 12, 350f),
                java.time.LocalDateTime.now().minusMinutes(2).toString()
            ),
            new EditProductSimpleMessage(
                "EditProductSimple",
                new EditProductSimplePayload(1025, 20, 600f),
                java.time.LocalDateTime.now().minusMinutes(1).toString()
            )
        );
    }
}
