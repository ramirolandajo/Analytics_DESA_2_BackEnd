package ar.edu.uade.analytics.Communication;

import ar.edu.uade.analytics.Communication.KafkaMockService.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaMockServiceMessagesTest {

    @Test
    void addProductMock_hasProductAndFields() {
        KafkaMockService svc = new KafkaMockService();
        AddProductMessage m = svc.getAddProductMock();
        assertNotNull(m);
        assertEquals("AddProduct", m.type);
        assertNotNull(m.payload);
        assertNotNull(m.payload.product);
        assertEquals(9999, m.payload.product.getProductCode());
        assertEquals("Nuevo Producto Mock", m.payload.product.getTitle());
    }

    @Test
    void editProductSimpleMock_containsStockAndPrice() {
        KafkaMockService svc = new KafkaMockService();
        EditProductSimpleMessage m = svc.getEditProductMockSimple();
        assertNotNull(m);
        assertEquals("EditProductSimple", m.type);
        assertNotNull(m.payload);
        assertEquals(1021, m.payload.productCode);
        assertEquals(999, m.payload.stock);
        assertEquals(199.99f, m.payload.price);
    }

    @Test
    void editProductFullMock_includesBrandAndCategories() {
        KafkaMockService svc = new KafkaMockService();
        EditProductFullMessage m = svc.getEditProductMockFull();
        assertNotNull(m);
        assertEquals("EditProductFull", m.type);
        assertNotNull(m.payload);
        assertEquals(30, m.payload.getProductCode());
        assertNotNull(m.payload.getBrand());
        assertNotNull(m.payload.getCategories());
        assertFalse(m.payload.getCategories().isEmpty());
    }

    @Test
    void activateDeactivateProductMock_haveIds() {
        KafkaMockService svc = new KafkaMockService();
        ActivateProductMessage a = svc.getActivateProductMock();
        DeactivateProductMessage d = svc.getDeactivateProductMock();
        assertNotNull(a);
        assertNotNull(d);
        assertEquals("ActivateProduct", a.type);
        assertEquals("DeactivateProduct", d.type);
        assertNotNull(a.payload);
        assertNotNull(d.payload);
        assertEquals(29L, a.payload.id);
        assertEquals(29L, d.payload.id);
    }

    @Test
    void productReviewMock_containsProductAndRating() {
        KafkaMockService svc = new KafkaMockService();
        ProductReviewMockMessage m = svc.getProductReviewMock();
        assertNotNull(m);
        assertEquals("ProductReview", m.type);
        assertNotNull(m.payload);
        assertEquals(11, m.payload.productId);
        assertEquals(4.5f, m.payload.calification);
    }

    @Test
    void dailyProductViewsMock_hasManyEntries_and_productCodes() {
        KafkaMockService svc = new KafkaMockService();
        DailyProductViewsMessage m = svc.getDailyProductViewsMock();
        assertNotNull(m);
        assertEquals("DAILY_PRODUCT_VIEWS", m.type);
        assertNotNull(m.payload);
        java.util.List<KafkaMockService.ProductViewDTO> products = m.payload.products;
        assertNotNull(products);
        assertTrue(products.size() >= 10);
        // check a sample product
        assertNotNull(products.get(0));
        assertNotNull(products.get(0).productCode);
    }
}
