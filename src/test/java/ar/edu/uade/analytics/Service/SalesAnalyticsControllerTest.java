package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesAnalyticsControllerTest {
    SalesAnalyticsController ctrl = new SalesAnalyticsController();

    @org.junit.jupiter.api.BeforeEach
    void init() throws Exception {
        // Assign a minimal PurchaseService inline so the controller has a non-null dependency
        ctrl.purchaseService = new PurchaseService() {
            @Override public java.util.List<Purchase> getAllPurchases(){ return java.util.List.of(); }
            @Override public Optional<Purchase> getPurchaseById(Integer id){ return Optional.empty(); }
            @Override public Purchase savePurchase(Purchase purchase){return purchase;}
            @Override public void deletePurchase(Integer id){}
            @Override public java.util.List<Purchase> findAll(){return java.util.List.of();}
            @Override public void save(Purchase purchase){}
            @Override public ar.edu.uade.analytics.Repository.ProductRepository getProductRepository(){ return null; }
        };
    }

    @Test
    void getSalesSummary_emptyPurchases_returnsZeros() {
        Map<String,Object> r = ctrl.getSalesSummary(null, null);
        assertEquals(0, r.get("totalVentas"));
        assertEquals(0f, r.get("facturacionTotal"));
    }

    @Test
    void getTrend_handlesEmptyPurchases() {
        var r = ctrl.getTrend(LocalDateTime.now().minusDays(1), LocalDateTime.now());
        assertTrue(r.containsKey("current"));
    }

    @Test
    void getTopProducts_handlesMissingProductTitle() throws Exception {
        // create a purchase with product id 5 and quantity 2
        Purchase p = new Purchase(); p.setStatus(Purchase.Status.CONFIRMED); p.setDate(LocalDateTime.now());
        Cart c = new Cart(); c.setFinalPrice(100f); CartItem it = new CartItem(); Product prod = new Product(); prod.setId(5); prod.setTitle(null); it.setProduct(prod); it.setQuantity(2); c.setItems(List.of(it)); p.setCart(c);

        // crear un ProductRepository simulado usando Mockito
        ar.edu.uade.analytics.Repository.ProductRepository productRepo = mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        org.mockito.Mockito.lenient().when(productRepo.findById(5)).thenReturn(Optional.of(prod));

        // assign directly to controller (same package)
        ctrl.purchaseService = new PurchaseService() {
            @Override public java.util.List<Purchase> getAllPurchases(){ return List.of(p); }
            @Override public Optional<Purchase> getPurchaseById(Integer id){ return Optional.empty(); }
            @Override public Purchase savePurchase(Purchase purchase){return purchase;}
            @Override public void deletePurchase(Integer id){}
            @Override public java.util.List<Purchase> findAll(){return List.of(p);}
            @Override public void save(Purchase purchase){}
            @Override public ar.edu.uade.analytics.Repository.ProductRepository getProductRepository(){ return productRepo; }
        };

        var top = ctrl.getTopProducts(5, null, null);
        assertTrue(top.containsKey("data"));
        @SuppressWarnings("unchecked")
        var data = (java.util.List<Map<String,Object>>) top.get("data");
        assertEquals(1, data.size());
        assertEquals("ID 5", data.get(0).get("title"));
    }
}
