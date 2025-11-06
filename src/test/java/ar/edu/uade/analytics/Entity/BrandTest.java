package ar.edu.uade.analytics.Entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrandTest {

    @Test
    void defaultActiveTrue() {
        Brand b = new Brand();
        assertTrue(b.isActive());
    }

    @Test
    void settersAndGetters() {
        Brand b = new Brand();
        b.setId(2);
        b.setName("MarcaX");
        b.setBrandCode(777);
        b.setActive(false);

        assertEquals(2, b.getId());
        assertEquals("MarcaX", b.getName());
        assertEquals(777, b.getBrandCode());
        assertFalse(b.isActive());
    }
}

