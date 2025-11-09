package ar.edu.uade.analytics.Entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseTest {

    @Test
    void enumStatusAndSetters() {
        Purchase p = new Purchase();
        p.setStatus(Purchase.Status.CONFIRMED);
        assertEquals(Purchase.Status.CONFIRMED, p.getStatus());

        LocalDateTime d = LocalDateTime.of(2022,1,1,12,0);
        p.setDate(d);
        p.setReservationTime(d);
        assertEquals(d, p.getDate());
        assertEquals(d, p.getReservationTime());
    }

    @Test
    void cartAndUserBackrefs() {
        Purchase p = new Purchase();
        Cart c = new Cart();
        c.setId(9);
        p.setCart(c);
        User u = new User();
        u.setId(3);
        p.setUser(u);

        assertEquals(9, p.getCart().getId());
        assertEquals(3, p.getUser().getId());
    }
}

