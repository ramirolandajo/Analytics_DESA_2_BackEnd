package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Purchase;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AEDServiceImplTest {

    private final AEDServiceImpl svc = new AEDServiceImpl();

    private Purchase makePurchase(int id, double finalPrice, int productId, int quantity) {
        Purchase p = new Purchase();
        p.setId(id);
        Cart c = new Cart();
        c.setFinalPrice((float) finalPrice);
        CartItem it = new CartItem();
        it.setQuantity(quantity);
        ar.edu.uade.analytics.Entity.Product pr = new ar.edu.uade.analytics.Entity.Product();
        pr.setId(productId);
        it.setProduct(pr);
        c.setItems(List.of(it));
        p.setCart(c);
        p.setDate(LocalDateTime.now());
        p.setStatus(Purchase.Status.CONFIRMED);
        return p;
    }

    @Test
    void getHistogramData_multipleValues_returnsBinsAndCounts() {
        List<Purchase> purchases = List.of(
                makePurchase(1, 10.0, 11, 1),
                makePurchase(2, 20.0, 12, 2),
                makePurchase(3, 30.0, 13, 1),
                makePurchase(4, 40.0, 14, 3)
        );
        Map<String, Object> res = svc.getHistogramData(purchases);
        assertNotNull(res);
        assertTrue(res.containsKey("bins"));
        assertTrue(res.containsKey("counts"));
        assertEquals(10, res.get("bins"));
    }

    @Test
    void getCorrelationData_withMultiplePurchases_computesCorrelation() {
        List<Purchase> purchases = List.of(
                makePurchase(1, 10.0, 11, 1),
                makePurchase(2, 20.0, 12, 2),
                makePurchase(3, 30.0, 13, 3)
        );
        Map<String, Object> res = svc.getCorrelationData(purchases);
        assertNotNull(res);
        assertTrue(res.containsKey("correlation_finalPrice_quantity"));
        assertInstanceOf(Double.class, res.get("correlation_finalPrice_quantity"));
    }

    @Test
    void getOutliers_detectsSingleHighOutlier() {
        List<Purchase> purchases = List.of(
                makePurchase(1, 10.0, 11, 1),
                makePurchase(2, 12.0, 12, 1),
                makePurchase(3, 500.0, 13, 1)
        );
        Map<String, Object> out = svc.getOutliers(purchases);
        assertNotNull(out);
        @SuppressWarnings("unchecked")
        java.util.List<Double> list = (java.util.List<Double>) out.get("outliers_finalPrice");
        assertNotNull(list);
        assertFalse(list.isEmpty());
    }

    @Test
    void getPurchasesTable_handlesNulls_gracefully() {
        Purchase p = new Purchase();
        p.setId(null);
        p.setCart(null);
        var table = svc.getPurchasesTable(List.of(p));
        assertNotNull(table);
        assertEquals(0, table.rowCount());
    }
}
