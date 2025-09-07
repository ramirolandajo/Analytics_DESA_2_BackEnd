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
public class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl service;

    @Test
    void testGetAllCategories() {
        Category c = new Category(); c.setId(1); c.setName("Cat1");
        when(categoryRepository.findAll()).thenReturn(List.of(c));
        var res = service.getAllCategories();
        assertNotNull(res);
        assertEquals(1, res.size());
        verify(categoryRepository).findAll();
    }

    @Test
    void testGetCategoryById() {
        Category c = new Category(); c.setId(2); c.setName("Cat2");
        when(categoryRepository.findById(2)).thenReturn(Optional.of(c));
        var opt = service.getCategoryById(2);
        assertTrue(opt.isPresent());
        assertEquals(2, opt.get().getId());
        verify(categoryRepository).findById(2);
    }

    @Test
    void testSaveAndDeleteCategory() {
        Category c = new Category(); c.setId(3);
        when(categoryRepository.save(c)).thenReturn(c);
        var saved = service.saveCategory(c);
        assertEquals(3, saved.getId());
        service.deleteCategory(3);
        verify(categoryRepository).deleteById(3);
        verify(categoryRepository).save(c);
    }
}

