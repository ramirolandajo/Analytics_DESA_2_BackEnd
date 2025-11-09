package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Purchase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AEDServiceNullSafetyTest {

    @Test
    void getNullCounts_handlesEmptyInput_andNulls() {
        AEDServiceImpl svc = new AEDServiceImpl();
        Purchase p = new Purchase(); p.setId(null);
        Cart c = new Cart(); c.setFinalPrice(null);
        CartItem it = new CartItem(); it.setProduct(null); it.setQuantity(null);
        it.setCart(c); c.setItems(List.of(it)); p.setCart(c);
        Map<String,Object> nullCounts = svc.getNullCounts(List.of(p));
        assertNotNull(nullCounts);
        // should contain columns even if missing values
        assertTrue(nullCounts.containsKey("productId"));
    }
}

