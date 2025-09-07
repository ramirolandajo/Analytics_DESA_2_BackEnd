package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> getAllCategories();
    Optional<Category> getCategoryById(Integer id);
    Category saveCategory(Category category);
    void deleteCategory(Integer id);
}

