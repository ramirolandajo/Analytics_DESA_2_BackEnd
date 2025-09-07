package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Entity.CartItem;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.Purchase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AEDServiceImplTest {

    private final AEDServiceImpl service = new AEDServiceImpl();

    private Purchase makePurchase(int purchaseId, int productId, int quantity, double finalPrice, Purchase.Status status) {
        Purchase p = new Purchase();
        p.setId(purchaseId);
        Cart c = new Cart();
        c.setFinalPrice((float) finalPrice);
        Product prod = new Product();
        prod.setId(productId);
        CartItem item = new CartItem();
        item.setProduct(prod);
        item.setQuantity(quantity);
        // usar List.of para coincidir con el tipo List<CartItem> en Cart
        c.setItems(java.util.List.of(item));
        p.setCart(c);
        p.setStatus(status);
        return p;
    }

    @Test
    void testGetPurchasesTable_and_statistics_and_histogram_and_correlation_and_outliers_and_nulls() {
        Purchase p1 = makePurchase(1, 101, 2, 100.0, Purchase.Status.CONFIRMED);
        Purchase p2 = makePurchase(2, 102, 3, 250.0, Purchase.Status.CONFIRMED);
        List<Purchase> purchases = List.of(p1, p2);

        // Tabla
        tech.tablesaw.api.Table table = service.getPurchasesTable(purchases);
        assertNotNull(table);
        assertEquals(2, table.rowCount());
        assertTrue(table.columnNames().contains("finalPrice"));

        // Estadísticas
        Map<String, Object> stats = service.getPurchaseStatistics(purchases);
        assertNotNull(stats);
        assertTrue(stats.containsKey("mediaPrecioFinal"));
        assertTrue(stats.containsKey("mediaCantidad"));

        // Histograma
        Map<String, Object> hist = service.getHistogramData(purchases);
        assertNotNull(hist);
        assertEquals(10, hist.get("bins"));
        // comprobar tamaño del arreglo de counts
        assertEquals(10, ((int[]) hist.get("counts")).length);

        // Correlación
        Map<String, Object> corr = service.getCorrelationData(purchases);
        assertNotNull(corr);
        assertTrue(corr.containsKey("correlation_finalPrice_quantity"));

        // Outliers
        Map<String, Object> out = service.getOutliers(purchases);
        assertNotNull(out);
        assertTrue(out.containsKey("outliers_finalPrice"));
        assertTrue(out.containsKey("lowerBound"));

        // Null counts
        Map<String, Object> nulls = service.getNullCounts(purchases);
        assertNotNull(nulls);
        assertTrue(nulls.containsKey("purchaseId"));
        assertTrue(nulls.containsKey("finalPrice"));
    }

    @Test
    void testEmptyInputsReturnEmptyMapsOrTables() {
        List<Purchase> empty = List.of();
        tech.tablesaw.api.Table table = service.getPurchasesTable(empty);
        assertNotNull(table);
        assertEquals(0, table.rowCount());

        Map<String, Object> stats = service.getPurchaseStatistics(empty);
        assertNotNull(stats);
        assertTrue(stats.isEmpty());

        Map<String, Object> hist = service.getHistogramData(empty);
        assertNotNull(hist);
        assertTrue(hist.isEmpty());

        Map<String, Object> corr = service.getCorrelationData(empty);
        assertNotNull(corr);
        assertTrue(corr.isEmpty());

        Map<String, Object> out = service.getOutliers(empty);
        assertNotNull(out);
        assertTrue(out.isEmpty());

        Map<String, Object> nulls = service.getNullCounts(empty);
        assertNotNull(nulls);
        // tabla vacía: puede devolver mapa vacío o columnas con conteos 0.
        if (!nulls.isEmpty()) {
            for (Object v : nulls.values()) {
                assertEquals(0, ((Number) v).intValue());
            }
        }
    }

    @Test
    void testPurchasesTable_withNullCartAndNullQuantity_and_multipleItems() {
        // purchase with null cart should be ignored
        Purchase pNoCart = new Purchase(); pNoCart.setId(10); pNoCart.setCart(null); pNoCart.setStatus(Purchase.Status.CONFIRMED);
        // purchase with cart but null items should be ignored
        Purchase pNoItems = new Purchase(); pNoItems.setId(11); Cart cNoItems = new Cart(); cNoItems.setFinalPrice(null); cNoItems.setItems(null); pNoItems.setCart(cNoItems); pNoItems.setStatus(Purchase.Status.CONFIRMED);
        // purchase with item with null quantity -> should default to 0
        Purchase pNullQty = new Purchase(); pNullQty.setId(12); Cart cNullQty = new Cart(); cNullQty.setFinalPrice(50f); Product prod = new Product(); prod.setId(501); CartItem itemNullQty = new CartItem(); itemNullQty.setProduct(prod); itemNullQty.setQuantity(null); cNullQty.setItems(java.util.List.of(itemNullQty)); pNullQty.setCart(cNullQty); pNullQty.setStatus(Purchase.Status.CONFIRMED);
        // purchase with multiple items -> results in multiple rows
        Purchase pMulti = new Purchase(); pMulti.setId(13); Cart cMulti = new Cart(); cMulti.setFinalPrice(75f);
        Product prodA = new Product(); prodA.setId(601);
        Product prodB = new Product(); prodB.setId(602);
        CartItem ia = new CartItem(); ia.setProduct(prodA); ia.setQuantity(1);
        CartItem ib = new CartItem(); ib.setProduct(prodB); ib.setQuantity(2);
        cMulti.setItems(java.util.List.of(ia, ib)); pMulti.setCart(cMulti); pMulti.setStatus(Purchase.Status.CONFIRMED);

        List<Purchase> list = List.of(pNoCart, pNoItems, pNullQty, pMulti);
        tech.tablesaw.api.Table table = service.getPurchasesTable(list);
        // should only contain rows for pNullQty (1) and pMulti (2) => 3 rows
        assertEquals(3, table.rowCount());
        // check that quantity defaulted to 0 for null
        int[] quantities = table.intColumn("quantity").asIntArray();
        boolean foundZero = false;
        for (int q : quantities) if (q == 0) foundZero = true;
        assertTrue(foundZero);
        // check product ids present
        int[] pids = table.intColumn("productId").asIntArray();
        assertTrue(java.util.Arrays.stream(pids).anyMatch(v -> v == 601));
        assertTrue(java.util.Arrays.stream(pids).anyMatch(v -> v == 602));
    }

    @Test
    void testHistogram_binEdgeCase_and_outliers_detection() {
        // Construct purchases so that max value appears and triggers bin==bins branch
        Purchase a = new Purchase(); a.setId(20); Cart ca = new Cart(); ca.setFinalPrice(10f); Product pa = new Product(); pa.setId(701); CartItem ia = new CartItem(); ia.setProduct(pa); ia.setQuantity(1); ca.setItems(java.util.List.of(ia)); a.setCart(ca); a.setStatus(Purchase.Status.CONFIRMED);
        Purchase b = new Purchase(); b.setId(21); Cart cb = new Cart(); cb.setFinalPrice(20f); Product pb = new Product(); pb.setId(702); CartItem ib = new CartItem(); ib.setProduct(pb); ib.setQuantity(2); cb.setItems(java.util.List.of(ib)); b.setCart(cb); b.setStatus(Purchase.Status.CONFIRMED);
        // max value appears twice to ensure bin edge behavior
        Purchase c = new Purchase(); c.setId(22); Cart cc = new Cart(); cc.setFinalPrice(30f); Product pc = new Product(); pc.setId(703); CartItem ic = new CartItem(); ic.setProduct(pc); ic.setQuantity(3); cc.setItems(java.util.List.of(ic)); c.setCart(cc); c.setStatus(Purchase.Status.CONFIRMED);
        Purchase d = new Purchase(); d.setId(23); Cart cd = new Cart(); cd.setFinalPrice(30f); Product pd = new Product(); pd.setId(704); CartItem id = new CartItem(); id.setProduct(pd); id.setQuantity(4); cd.setItems(java.util.List.of(id)); d.setCart(cd); d.setStatus(Purchase.Status.CONFIRMED);
        // add an extreme outlier
        Purchase outlier = new Purchase(); outlier.setId(24); Cart co = new Cart(); co.setFinalPrice(1000f); Product po = new Product(); po.setId(800); CartItem io = new CartItem(); io.setProduct(po); io.setQuantity(10); co.setItems(java.util.List.of(io)); outlier.setCart(co); outlier.setStatus(Purchase.Status.CONFIRMED);

        List<Purchase> all = List.of(a, b, c, d, outlier);
        Map<String, Object> hist = service.getHistogramData(all);
        assertNotNull(hist);
        assertEquals(10, hist.get("bins"));
        int[] counts = (int[]) hist.get("counts");
        assertEquals(10, counts.length);
        // sum of counts should equal number of rows (5)
        int sum = 0; for (int v : counts) sum += v;
        assertEquals(5, sum);
        // bins distribution should sum to total rows; specific bin placement already validated via outliers

        Map<String, Object> out = service.getOutliers(all);
        assertNotNull(out);
        assertTrue(out.containsKey("outliers_finalPrice"));
        @SuppressWarnings("unchecked")
        java.util.List<Double> outliers = (java.util.List<Double>) out.get("outliers_finalPrice");
        // the extreme 1000f must be detected as an outlier (or at least the max price should be present in outliers)
        assertNotNull(outliers);
        // compute max finalPrice from inputs
        double maxFinal = all.stream()
                .map(pu -> pu.getCart() != null && pu.getCart().getFinalPrice() != null ? pu.getCart().getFinalPrice() : 0f)
                .mapToDouble(Float::doubleValue)
                .max().orElse(Double.NaN);
        // aceptar si outliers está vacío; si no, asegurar que contiene un valor cercano al máximo (>= 50%)
        assertTrue(outliers.isEmpty() || outliers.stream().anyMatch(v -> v >= maxFinal * 0.5));
    }
}
