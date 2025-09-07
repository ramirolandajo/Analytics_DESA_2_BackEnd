package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    Product findByProductCode(Integer productCode);
}

