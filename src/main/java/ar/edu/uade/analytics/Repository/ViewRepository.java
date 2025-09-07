package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.View;
import ar.edu.uade.analytics.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ViewRepository extends JpaRepository<View, Long> {
    List<View> findByProduct(Product product);
}

