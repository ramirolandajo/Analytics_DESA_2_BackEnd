package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.StockChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockChangeLogRepository extends JpaRepository<StockChangeLog, Long> {
    List<StockChangeLog> findByProductIdOrderByChangedAtAsc(Integer productId);
}

