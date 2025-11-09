package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Category findByCategoryCode(Integer categoryCode);
    Category findByNameIgnoreCase(String name);
    List<Category> findByCategoryCodeIn(List<Integer> categoryCodes);
    List<Category> findByIdIn(List<Integer> ids);
}
