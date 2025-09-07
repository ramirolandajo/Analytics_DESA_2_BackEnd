package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {
}

