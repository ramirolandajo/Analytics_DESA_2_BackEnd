package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Purchase;
import ar.edu.uade.analytics.Repository.PurchaseRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseServiceImplTest {

    @Test
    void getAllPurchases_delegatesToRepo() {
        PurchaseRepository repo = mock(PurchaseRepository.class);
        Purchase p = new Purchase();
        when(repo.findAll()).thenReturn(List.of(p));
        PurchaseServiceImpl svc = new PurchaseServiceImpl(repo);
        var res = svc.getAllPurchases();
        assertEquals(1, res.size());
    }

    @Test
    void savePurchase_callsRepoSave() {
        PurchaseRepository repo = mock(PurchaseRepository.class);
        Purchase p = new Purchase();
        when(repo.save(p)).thenReturn(p);
        PurchaseServiceImpl svc = new PurchaseServiceImpl(repo);
        var out = svc.savePurchase(p);
        assertSame(p, out);
        verify(repo, times(1)).save(p);
    }

    @Test
    void deletePurchase_callsRepoDeleteById() {
        PurchaseRepository repo = mock(PurchaseRepository.class);
        PurchaseServiceImpl svc = new PurchaseServiceImpl(repo);
        svc.deletePurchase(5);
        verify(repo, times(1)).deleteById(5);
    }
}
