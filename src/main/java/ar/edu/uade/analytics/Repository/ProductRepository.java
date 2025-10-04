package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    Product findByProductCode(Integer productCode);

    @Query("select p.stock from Product p where p.productCode = :productCode")
    Integer findStockByProductCode(@Param("productCode") Integer productCode);
}
