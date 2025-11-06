package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Purchase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AEDServiceHistogramTest {

    @Test
    void getHistogramData_computesBins_and_counts() {
        AEDServiceImpl svc = new AEDServiceImpl();
        Purchase p1 = new Purchase(); Cart c1 = new Cart(); c1.setFinalPrice(10f); CartItem i1 = new CartItem(); i1.setQuantity(1); i1.setCart(c1); c1.setItems(List.of(i1)); p1.setCart(c1);
        Purchase p2 = new Purchase(); Cart c2 = new Cart(); c2.setFinalPrice(20f); CartItem i2 = new CartItem(); i2.setQuantity(1); i2.setCart(c2); c2.setItems(List.of(i2)); p2.setCart(c2);
        Purchase p3 = new Purchase(); Cart c3 = new Cart(); c3.setFinalPrice(30f); CartItem i3 = new CartItem(); i3.setQuantity(1); i3.setCart(c3); c3.setItems(List.of(i3)); p3.setCart(c3);
        Map<String, Object> out = svc.getHistogramData(List.of(p1,p2,p3));
        assertTrue(out.containsKey("bins"));
        assertTrue(out.containsKey("counts"));
        int[] counts = (int[]) out.get("counts");
        assertNotNull(counts);
    }
}

