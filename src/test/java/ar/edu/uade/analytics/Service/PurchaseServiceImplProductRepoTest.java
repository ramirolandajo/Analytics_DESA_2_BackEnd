package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Repository.ProductRepository;
import ar.edu.uade.analytics.Repository.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceImplProductRepoTest {

    @Mock ProductRepository productRepository;
    @Mock PurchaseRepository purchaseRepository;
    PurchaseServiceImpl svc;

    @BeforeEach
    void setUp() throws Exception {
        svc = new PurchaseServiceImpl(purchaseRepository);
        var f = PurchaseServiceImpl.class.getDeclaredField("productRepository"); f.setAccessible(true); f.set(svc, productRepository);
    }

    @Test
    void getProductRepository_returnsConfiguredRepo() {
        var pr = svc.getProductRepository();
        assertNotNull(pr);
    }
}
