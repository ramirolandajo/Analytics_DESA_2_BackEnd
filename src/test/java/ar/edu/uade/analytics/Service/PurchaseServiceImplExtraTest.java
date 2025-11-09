package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Purchase;
import ar.edu.uade.analytics.Repository.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseServiceImplExtraTest {
    @Mock PurchaseRepository purchaseRepository;
    PurchaseServiceImpl service;

    @Captor ArgumentCaptor<Purchase> captor;

    @BeforeEach
    void setUp() {
        service = new PurchaseServiceImpl(purchaseRepository);
    }

    @Test
    void savePurchase_savesAndReturns() {
        Purchase p = new Purchase();
        p.setId(1);
        service.savePurchase(p);
        verify(purchaseRepository).save(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getId());
    }

    @Test
    void getAllPurchases_delegatesToRepo() {
        service.getAllPurchases();
        verify(purchaseRepository).findAll();
    }
}
