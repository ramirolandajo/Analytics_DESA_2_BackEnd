package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Purchase;
import ar.edu.uade.analytics.Repository.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceImplMoreTest {

    @Mock PurchaseRepository purchaseRepository;
    PurchaseServiceImpl svc;

    @BeforeEach
    void setUp() {
        svc = new PurchaseServiceImpl(purchaseRepository);
    }

    @Test
    void findAll_delegatesToRepo() {
        when(purchaseRepository.findAll()).thenReturn(List.of(new Purchase()));
        var res = svc.findAll();
        assertNotNull(res);
        verify(purchaseRepository).findAll();
    }

    @Test
    void save_delegatesToRepo() {
        Purchase p = new Purchase();
        svc.save(p);
        verify(purchaseRepository).save(p);
    }
}

