package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Purchase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AEDServiceImplExtraTest {

    @Test
    void nullProduct_inCartItem_isHandled_gracefully() {
        AEDServiceImpl svc = new AEDServiceImpl();
        Purchase p = new Purchase(); p.setId(1);
        Cart c = new Cart(); c.setFinalPrice(10f);
        CartItem it = new CartItem(); it.setProduct(null); it.setQuantity(1);
        it.setCart(c);
        c.setItems(List.of(it));
        p.setCart(c);

        Map<String,Object> stats = svc.getPurchaseStatistics(List.of(p));
        // should not throw and return empty (no product id means no meaningful stats)
        assertNotNull(stats);
    }

    @Test
    void correlation_withMixedData_returnsMapEvenWhenSinglePoint() {
        AEDServiceImpl svc = new AEDServiceImpl();
        Purchase p = new Purchase(); p.setId(10); p.setStatus(Purchase.Status.CONFIRMED);
        Cart c = new Cart(); c.setFinalPrice(20f);
        CartItem it = new CartItem(); ar.edu.uade.analytics.Entity.Product pr = new ar.edu.uade.analytics.Entity.Product(); pr.setId(5); it.setProduct(pr); it.setQuantity(2);
        it.setCart(c); c.setItems(List.of(it)); p.setCart(c);
        var corr = svc.getCorrelationData(List.of(p));
        assertNotNull(corr);
    }
}

