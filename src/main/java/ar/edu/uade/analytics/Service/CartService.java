package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Cart;
import java.util.List;
import java.util.Optional;

public interface CartService {
    List<Cart> getAllCarts();
    Optional<Cart> getCartById(Integer id);
    Cart saveCart(Cart cart);
    void deleteCart(Integer id);
}

