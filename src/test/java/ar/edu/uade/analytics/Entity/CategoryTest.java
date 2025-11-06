package ar.edu.uade.analytics.Entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    @Test
    void defaultConstructorSetsActiveTrue() {
        Category c = new Category();
        assertTrue(c.isActive(), "Default Category should be active");
    }

    @Test
    void settersAndGetters() {
        Category c = new Category();
        c.setId(5);
        c.setName("Electrónica");
        c.setCategoryCode(123);
        c.setActive(false);

        assertEquals(5, c.getId());
        assertEquals("Electrónica", c.getName());
        assertEquals(123, c.getCategoryCode());
        assertFalse(c.isActive());
    }
}

