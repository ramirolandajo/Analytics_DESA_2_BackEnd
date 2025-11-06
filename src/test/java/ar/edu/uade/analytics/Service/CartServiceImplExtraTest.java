package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Cart;
import ar.edu.uade.analytics.Repository.CartRepository;
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
class CartServiceImplExtraTest {

    @Mock private CartRepository repo;
    @InjectMocks private CartServiceImpl svc;

    @Test
    void basicCrudOperations_delegateToRepository() {
        Cart c = new Cart();
        when(repo.findAll()).thenReturn(List.of(c));
        assertEquals(1, svc.getAllCarts().size());

        when(repo.findById(2)).thenReturn(Optional.of(c));
        assertTrue(svc.getCartById(2).isPresent());

        when(repo.save(c)).thenReturn(c);
        assertSame(c, svc.saveCart(c));

        svc.deleteCart(9);
        verify(repo).deleteById(9);
    }
}

