package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}

