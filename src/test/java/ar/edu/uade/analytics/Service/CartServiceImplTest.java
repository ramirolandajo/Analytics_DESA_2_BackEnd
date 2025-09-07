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
public class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartServiceImpl service;

    @Test
    void testGetAllCarts() {
        Cart c = new Cart(); c.setId(1);
        when(cartRepository.findAll()).thenReturn(List.of(c));
        var res = service.getAllCarts();
        assertNotNull(res);
        assertEquals(1, res.size());
        verify(cartRepository).findAll();
    }

    @Test
    void testGetCartById() {
        Cart c = new Cart(); c.setId(2);
        when(cartRepository.findById(2)).thenReturn(Optional.of(c));
        var opt = service.getCartById(2);
        assertTrue(opt.isPresent());
        assertEquals(2, opt.get().getId());
        verify(cartRepository).findById(2);
    }

    @Test
    void testSaveAndDeleteCart() {
        Cart c = new Cart(); c.setId(3);
        when(cartRepository.save(c)).thenReturn(c);
        var saved = service.saveCart(c);
        assertEquals(3, saved.getId());
        service.deleteCart(3);
        verify(cartRepository).deleteById(3);
        verify(cartRepository).save(c);
    }
}

