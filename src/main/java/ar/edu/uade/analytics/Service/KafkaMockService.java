package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.DTO.BrandDTO;
import ar.edu.uade.analytics.DTO.CategoryDTO;
import ar.edu.uade.analytics.DTO.ProductDTO;

import java.util.List;

public class KafkaMockService {


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

}
