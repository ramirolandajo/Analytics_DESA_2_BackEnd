package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Purchase;
import ar.edu.uade.analytics.Repository.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PurchaseServiceImplTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private PurchaseServiceImpl service;

    @Test
    void testGetAllPurchases_delegatesToRepository() {
        Purchase p = new Purchase(); p.setId(1);
        when(purchaseRepository.findAll()).thenReturn(List.of(p));
        List<Purchase> res = service.getAllPurchases();
        assertNotNull(res);
        assertEquals(1, res.size());
        verify(purchaseRepository).findAll();
    }

    @Test
    void testGetPurchaseById_returnsOptional() {
        Purchase p = new Purchase(); p.setId(2);
        when(purchaseRepository.findById(2)).thenReturn(Optional.of(p));
        Optional<Purchase> res = service.getPurchaseById(2);
        assertTrue(res.isPresent());
        assertEquals(2, res.get().getId());
        verify(purchaseRepository).findById(2);
    }

    @Test
    void testSaveAndDeletePurchase() {
        Purchase p = new Purchase(); p.setId(3);
        when(purchaseRepository.save(p)).thenReturn(p);
        Purchase saved = service.savePurchase(p);
        assertEquals(3, saved.getId());
        service.deletePurchase(3);
        verify(purchaseRepository).deleteById(3);
        verify(purchaseRepository).save(p);
    }

    @Test
    void testFindAll_and_save_void_wrapper() {
        Purchase p = new Purchase(); p.setId(4);
        when(purchaseRepository.findAll()).thenReturn(List.of(p));
        List<Purchase> res = service.findAll();
        assertEquals(1, res.size());
        service.save(p); // void
        verify(purchaseRepository).save(p);
    }
}

