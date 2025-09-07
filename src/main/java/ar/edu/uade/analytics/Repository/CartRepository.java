package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Integer> {
}

