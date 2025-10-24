package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    Product findByProductCode(Integer productCode);

    @Query("select p.stock from Product p where p.productCode = :productCode")
    Integer findStockByProductCode(@Param("productCode") Integer productCode);

    List<Product> findByStockLessThanEqual(int i);

    // Proyección para precargar stocks por código
    interface CodeStock {
        Integer getProductCode();
        Integer getStock();
    }

    List<CodeStock> findByProductCodeIn(Collection<Integer> productCodes);
}
