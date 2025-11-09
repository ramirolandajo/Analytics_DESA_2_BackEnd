package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesAnalyticsControllerMoreTest {
    @Mock PurchaseService purchaseService;
    SalesAnalyticsController ctrl;

    @BeforeEach
    void setUp() throws Exception {
        ctrl = new SalesAnalyticsController();
        java.lang.reflect.Field f = SalesAnalyticsController.class.getDeclaredField("purchaseService"); f.setAccessible(true); f.set(ctrl, purchaseService);
    }

    @Test
    void getTopProducts_withTitlePresent_returnsTitle() {
        Purchase p = new Purchase(); p.setStatus(Purchase.Status.CONFIRMED); p.setDate(LocalDateTime.now());
        Cart c = new Cart(); c.setFinalPrice(10f); CartItem it = new CartItem(); Product prod = new Product(); prod.setId(5); prod.setTitle("X"); it.setProduct(prod); it.setQuantity(2); c.setItems(List.of(it)); p.setCart(c);

        ar.edu.uade.analytics.Repository.ProductRepository productRepo = mock(ar.edu.uade.analytics.Repository.ProductRepository.class);
        when(productRepo.findById(5)).thenReturn(Optional.of(prod));

        ctrl.purchaseService = new PurchaseService() {
            @Override public java.util.List<Purchase> getAllPurchases(){ return List.of(p); }
            @Override public Optional<Purchase> getPurchaseById(Integer id){ return Optional.empty(); }
            @Override public Purchase savePurchase(Purchase purchase){return purchase;}
            @Override public void deletePurchase(Integer id){}
            @Override public java.util.List<Purchase> findAll(){return List.of(p);}
            @Override public void save(Purchase purchase){}
            @Override public ar.edu.uade.analytics.Repository.ProductRepository getProductRepository(){ return productRepo; }
        };

        Map<String,Object> top = ctrl.getTopProducts(5, null, null);
        assertTrue(top.containsKey("data"));
        @SuppressWarnings("unchecked")
        var data = (java.util.List<Map<String,Object>>) top.get("data");
        assertEquals("X", data.get(0).get("title"));
    }
}

