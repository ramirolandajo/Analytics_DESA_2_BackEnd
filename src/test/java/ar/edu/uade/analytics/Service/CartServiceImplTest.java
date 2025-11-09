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
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    void basicCRUD() {
        when(cartRepository.findAll()).thenReturn(List.of(new Cart()));
        assertFalse(cartService.getAllCarts().isEmpty());
        Cart c = new Cart();
        when(cartRepository.findById(Integer.valueOf(1))).thenReturn(Optional.of(c));
        assertTrue(cartService.getCartById(Integer.valueOf(1)).isPresent());
        when(cartRepository.save(c)).thenReturn(c);
        assertSame(c, cartService.saveCart(c));
        cartService.deleteCart(Integer.valueOf(1));
        verify(cartRepository).deleteById(Integer.valueOf(1));
    }
}
