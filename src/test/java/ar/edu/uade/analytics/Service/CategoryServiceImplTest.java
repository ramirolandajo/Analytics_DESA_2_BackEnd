package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Category;
import ar.edu.uade.analytics.Repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void getAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(new Category()));
        assertFalse(categoryService.getAllCategories().isEmpty());
    }

    @Test
    void getSaveDelete() {
        Category c = new Category();
        when(categoryRepository.findById(Integer.valueOf(1))).thenReturn(Optional.of(c));
        assertTrue(categoryService.getCategoryById(Integer.valueOf(1)).isPresent());
        when(categoryRepository.save(c)).thenReturn(c);
        assertSame(c, categoryService.saveCategory(c));
        categoryService.deleteCategory(Integer.valueOf(1));
        verify(categoryRepository).deleteById(Integer.valueOf(1));
    }
}
