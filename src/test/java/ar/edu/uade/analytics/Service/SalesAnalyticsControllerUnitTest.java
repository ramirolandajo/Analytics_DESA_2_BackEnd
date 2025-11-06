package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SalesAnalyticsControllerUnitTest {
    SalesAnalyticsController controller;
    @Mock PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        controller = new SalesAnalyticsController();
        controller.purchaseService = purchaseService;
    }

    @Test
    void getSalesSummary_emptyPurchases_returnsZeros() {
        when(purchaseService.getAllPurchases()).thenReturn(List.of());
        Map<String, Object> summary = controller.getSalesSummary(null, null);
        assertEquals(0, summary.get("totalVentas"));
        assertEquals(0f, (Float)summary.get("facturacionTotal"));
        assertEquals(0, summary.get("productosVendidos"));
        assertEquals(0, summary.get("clientesActivos"));
    }

    @Test
    void getTopProducts_counts_and_resolvesTitleFallback() {
        // create purchases with items
        Product p1 = new Product(); p1.setId(11); p1.setTitle(null);
        CartItem ci = new CartItem(); ci.setProduct(p1); ci.setQuantity(2);
        Cart cart = new Cart(); cart.setItems(List.of(ci));
        Purchase purchase = new Purchase(); purchase.setStatus(Purchase.Status.CONFIRMED); purchase.setDate(LocalDateTime.now()); purchase.setCart(cart);

        when(purchaseService.getAllPurchases()).thenReturn(List.of(purchase));
        // purchaseService.getProductRepository().findById should be used in controller: create a fake repository via PurchaseService mock
        ar.edu.uade.analytics.Repository.ProductRepository prodRepo = mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        when(purchaseService.getProductRepository()).thenReturn(prodRepo);
        when(prodRepo.findById(11)).thenReturn(java.util.Optional.empty());

        Map<String, Object> top = controller.getTopProducts(10, null, null);
        List<?> data = (List<?>) top.get("data");
        assertEquals(1, data.size());
        Map<?,?> entry = (Map<?,?>) data.get(0);
        assertEquals(11, entry.get("productId"));
        assertEquals(2, entry.get("cantidadVendida"));
        assertTrue(((String)entry.get("title")).startsWith("ID "));
    }
}

