package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Purchase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AEDServiceOutliersTest {

    @Test
    void outliers_singleOutlier_detected() {
        AEDServiceImpl svc = new AEDServiceImpl();
        // Create many normal purchases (finalPrice ~10) and one extreme outlier (1000)
        java.util.List<Purchase> purchases = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Purchase p = new Purchase();
            Cart c = new Cart();
            c.setFinalPrice(10f + (i % 3)); // small variation
            CartItem it = new CartItem();
            it.setQuantity(1);
            it.setCart(c);
            c.setItems(List.of(it));
            p.setCart(c);
            purchases.add(p);
        }
        // Add a clear outlier
        Purchase pout = new Purchase();
        Cart cout = new Cart();
        cout.setFinalPrice(1000f);
        CartItem iout = new CartItem(); iout.setQuantity(1); iout.setCart(cout); cout.setItems(List.of(iout));
        pout.setCart(cout);
        purchases.add(pout);

        Map<String,Object> out = svc.getOutliers(purchases);
        assertTrue(out.containsKey("outliers_finalPrice"), "Missing key outliers_finalPrice in result: " + out);
        // also expect bounds
        assertTrue(out.containsKey("upperBound"), "Missing upperBound in result: " + out);
        assertTrue(out.containsKey("lowerBound"), "Missing lowerBound in result: " + out);
        double upper = ((Number) out.get("upperBound")).doubleValue();
        // sanity: the outlier 1000 should be greater than upper
        final double expected = 1000.0;
        assertTrue(expected > upper, "Expected test outlier " + expected + " to be > upperBound=" + upper + ", result=" + out);

        var list = (java.util.List<?>) out.get("outliers_finalPrice");
        boolean has = list.stream().anyMatch(o -> {
            double v;
            if (o instanceof Number) v = ((Number) o).doubleValue();
            else {
                try { v = Double.parseDouble(o.toString()); } catch (Exception e) { return false; }
            }
            return v > upper - 1e-9;
        });
        assertTrue(has, "Expected an outlier value > upperBound=" + upper + " in the outliers list; actual outliers=" + list);
    }
}
