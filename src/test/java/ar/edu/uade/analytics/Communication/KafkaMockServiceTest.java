package ar.edu.uade.analytics.Communication;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KafkaMockServiceTest {

    KafkaMockService mock = new KafkaMockService();

    @Test
    void testGetProductsMock() {
        KafkaMockService.ProductSyncMessage msg = mock.getProductsMock();
        assertNotNull(msg);
        assertEquals("ProductSync", msg.type);
        assertNotNull(msg.payload);
        assertNotNull(msg.payload.products);
        assertTrue(msg.payload.products.size() >= 5);
        // spot-check first product
        var p0 = msg.payload.products.get(0);
        assertEquals(21L, p0.getId().longValue());
        assertNotNull(p0.getBrand());
        assertNotNull(p0.getCategories());
    }

    @Test
    void testGetCategoriesMock() {
        KafkaMockService.CategorySyncMessage msg = mock.getCategoriesMock();
        assertNotNull(msg);
        assertEquals("CategorySync", msg.type);
        assertNotNull(msg.payload);
        assertNotNull(msg.payload.categories);
        assertTrue(msg.payload.categories.size() >= 15);
        assertEquals("Celulares", msg.payload.categories.get(0).getName());
    }

    @Test
    void testGetBrandsMock() {
        KafkaMockService.BrandSyncMessage msg = mock.getBrandsMock();
        assertNotNull(msg);
        assertEquals("BrandSync", msg.type);
        assertNotNull(msg.payload);
        assertNotNull(msg.payload.brands);
        assertTrue(msg.payload.brands.size() >= 15);
        assertEquals("Samsung", msg.payload.brands.get(0).getName());
    }

    @Test
    void testEditAndAddProductMocks() {
        var editSimple = mock.getEditProductMockSimple();
        assertNotNull(editSimple);
        assertEquals("EditProductSimple", editSimple.type);
        assertNotNull(editSimple.payload);
        assertEquals(1021, editSimple.payload.productCode.intValue());

        var editFull = mock.getEditProductMockFull();
        assertNotNull(editFull);
        assertEquals("EditProductFull", editFull.type);
        assertNotNull(editFull.payload);
        assertEquals(30, editFull.payload.getId().intValue());

        var add = mock.getAddProductMock();
        assertNotNull(add);
        assertEquals("AddProduct", add.type);
        assertNotNull(add.payload);
        assertNotNull(add.payload.product);
        assertEquals(99L, add.payload.product.getId().longValue());
    }

    @Test
    void testActivateDeactivateAndReviewMocks() {
        var act = mock.getActivateProductMock();
        assertNotNull(act);
        assertEquals("ActivateProduct", act.type);
        assertNotNull(act.payload);
        assertEquals(29L, act.payload.id.longValue());

        var deact = mock.getDeactivateProductMock();
        assertNotNull(deact);
        assertEquals("DeactivateProduct", deact.type);
        assertNotNull(deact.payload);
        assertEquals(29L, deact.payload.id.longValue());

        var review = mock.getProductReviewMock();
        assertNotNull(review);
        assertEquals("ProductReview", review.type);
        assertNotNull(review.payload);
        assertTrue(review.payload.productId > 0);
    }

    @Test
    void testDailyProductViewsAndFavouriteMocks() {
        var daily = mock.getDailyProductViewsMock();
        assertNotNull(daily);
        assertEquals("DAILY_PRODUCT_VIEWS", daily.type);
        assertNotNull(daily.payload);
        assertNotNull(daily.payload.products);
        assertTrue(daily.payload.products.size() >= 10);

        var favs = mock.getAddFavouriteProductsMock();
        assertNotNull(favs);
        assertTrue(favs.size() > 0);
        assertEquals("ADD_FAVOURITE_PRODUCT", favs.get(0).type);
    }

    @Test
    void testSaleEventMocks() {
        var s1 = mock.getSaleEventMock();
        var s2 = mock.getSaleEventMock13();
        var s3 = mock.getSaleEventMock14();
        var s4 = mock.getSaleEventMock15();
        assertNotNull(s1);
        assertNotNull(s2);
        assertNotNull(s3);
        assertNotNull(s4);
        assertEquals("StockConfirmed_CartPurchase", s1.type);
        assertNotNull(s1.payload);
        assertNotNull(s1.payload.user);
        assertNotNull(s1.payload.cart);
    }

    @Test
    void testGetSaleEventMockListAndEditProductSimpleList() {
        var list = mock.getSaleEventMockList();
        assertNotNull(list);
        assertTrue(list.size() >= 1);

        var edits = mock.getEditProductMockSimpleList();
        assertNotNull(edits);
        assertTrue(edits.size() >= 1);
    }

    @Test
    void testGetAddFavouriteProductMock_singular() {
        var fav = mock.getAddFavouriteProductMock();
        assertNotNull(fav);
        assertEquals("ADD_FAVOURITE_PRODUCT", fav.type);
        assertNotNull(fav.payload);
        assertEquals(1006, fav.payload.getProductCode().intValue());
        assertEquals(41, fav.payload.getId().intValue());
    }

    @Test
    void testGetProductReviewMockList() {
        var reviews = mock.getProductReviewMockList();
        assertNotNull(reviews);
        assertTrue(reviews.size() >= 1);
        // spot check some entries have productId and calification
        var r0 = reviews.get(0);
        assertEquals("ProductReview", r0.type);
        assertNotNull(r0.payload);
        assertTrue(r0.payload.productId > 0);
        assertTrue(r0.payload.calification >= 0f);
    }
}
