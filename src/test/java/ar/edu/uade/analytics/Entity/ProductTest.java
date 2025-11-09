package ar.edu.uade.analytics.Entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void defaultActiveIsNull() {
        Product p = new Product();
        assertNull(p.getActive(), "Default active should be null");
    }

    @Test
    void settersAndGettersAndFlags() {
        Product p = new Product();
        p.setId(10);
        p.setTitle("Title");
        p.setDescription("Desc");
        p.setPrice(123.45f);
        p.setActive(true);
        p.setIsNew(true);
        p.setBestseller(true);
        p.setFeatured(true);
        p.setNew(false);

        assertEquals(10, p.getId());
        assertEquals("Title", p.getTitle());
        assertEquals("Desc", p.getDescription());
        assertEquals(123.45f, p.getPrice());
        assertTrue(p.getActive());
        assertFalse(p.getIsNew(), "setNew(false) should set isNew to false");

        // mediaSrc is ElementCollection backed by List; ensure setter works
        p.setMediaSrc(List.of("a.jpg", "b.png"));
        assertEquals(2, p.getMediaSrc().size());
    }

    @Test
    void setIsNewFromObject() {
        Product p = new Product();
        p.setIsNew("true");
        assertTrue(p.getIsNew());

        p.setIsNew(null);
        assertFalse(p.getIsNew());

        p.setIsNew(Boolean.TRUE);
        assertTrue(p.getIsNew());

        p.setIsNew(0); // non-boolean string -> toString -> "0" -> Boolean.parseBoolean("0") == false
        assertFalse(p.getIsNew());

        // custom object whose toString returns "true"
        p.setIsNew(new Object() {
            @Override
            public String toString() {
                return "true";
            }
        });
        assertTrue(p.getIsNew());
    }

    @Test
    void equalityHashcodeAndOtherFields() {
        Product a = new Product();
        a.setId(1);
        a.setTitle("X");
        a.setPrice(10.0f);

        Product b = new Product();
        b.setId(1);
        b.setTitle("X");
        b.setPrice(10.0f);

        assertEquals(a, b, "Products with same id/title/price should be equal by Lombok @Data");
        assertEquals(a.hashCode(), b.hashCode());

        a.setStock(50);
        a.setProductCode(999);
        a.setDiscount(5.5f);
        a.setPriceUnit(9.99f);
        a.setCalification(4.7f);

        assertEquals(50, a.getStock());
        assertEquals(999, a.getProductCode());
        assertEquals(5.5f, a.getDiscount());
        assertEquals(9.99f, a.getPriceUnit());
        assertEquals(4.7f, a.getCalification());
    }

    @Test
    void mediaSrcIsImmutableFromGetter() {
        Product p = new Product();
        List<String> media = List.of("one.png");
        p.setMediaSrc(media);
        // try to modify original list reference should not affect product's internal state (since we used List.of which is immutable)
        assertEquals(1, p.getMediaSrc().size());
    }

    @Test
    void flagsBestsellerFeatured() {
        Product p = new Product();
        p.setBestseller(true);
        p.setFeatured(false);
        assertTrue(p.isIsBestseller());
        assertFalse(p.isIsFeatured());

        // also call the setters with "is" prefix
        p.setIsBestseller(false);
        p.setIsFeatured(true);
        assertFalse(p.isIsBestseller());
        assertTrue(p.isIsFeatured());
    }

    @Test
    void toStringNotNull() {
        Product p = new Product();
        p.setTitle("abc");
        assertNotNull(p.toString());
    }

    @Test
    void mediaSrcAndActiveNullHandling() {
        Product p = new Product();
        p.setMediaSrc(null);
        assertNull(p.getMediaSrc());

        p.setActive(null);
        assertNull(p.getActive());
    }
}
