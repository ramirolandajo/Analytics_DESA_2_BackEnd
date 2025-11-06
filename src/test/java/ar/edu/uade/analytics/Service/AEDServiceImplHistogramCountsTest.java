package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.Purchase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AEDServiceImplHistogramCountsTest {
    AEDServiceImpl svc = new AEDServiceImpl();

    @Test
    void histogram_counts_sumEqualsNumberOfPurchases() {
        Purchase p1 = new Purchase(); Cart c1 = new Cart(); CartItem it1 = new CartItem(); Product pr1 = new Product(); pr1.setId(1); it1.setProduct(pr1); it1.setQuantity(1); c1.setItems(List.of(it1)); c1.setFinalPrice(10f); p1.setCart(c1);
        Purchase p2 = new Purchase(); Cart c2 = new Cart(); CartItem it2 = new CartItem(); Product pr2 = new Product(); pr2.setId(2); it2.setProduct(pr2); it2.setQuantity(1); c2.setItems(List.of(it2)); c2.setFinalPrice(90f); p2.setCart(c2);

        Map<String,Object> hist = svc.getHistogramData(List.of(p1,p2));
        assertTrue(hist.containsKey("counts"));
        int[] counts = (int[]) hist.get("counts");
        int sum = 0; for (int v : counts) sum += v;
        assertEquals(2, sum);
    }
}

