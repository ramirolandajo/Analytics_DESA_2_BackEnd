package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.View;
import ar.edu.uade.analytics.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ViewRepository extends JpaRepository<View, Long> {
    List<View> findByProduct(Product product);

    interface ProductViewsCount {
        Integer getProductCode();
        Long getTotalViews();
    }

    @Query("""
            SELECT v.productCode AS productCode, COUNT(v) AS totalViews
            FROM View v
            WHERE (:from IS NULL OR v.viewedAt >= :from)
              AND (:to IS NULL OR v.viewedAt <= :to)
              AND v.productCode IS NOT NULL
            GROUP BY v.productCode
            """)
    List<ProductViewsCount> countViewsByProductCode(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
