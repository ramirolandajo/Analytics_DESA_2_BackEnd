package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Repository.ProductRepository;
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
public class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl service;

    @Test
    void testGetAllProducts() {
        Product p = new Product(); p.setId(1);
        when(productRepository.findAll()).thenReturn(List.of(p));
        var res = service.getAllProducts();
        assertNotNull(res);
        assertEquals(1, res.size());
        verify(productRepository).findAll();
    }

    @Test
    void testGetProductById() {
        Product p = new Product(); p.setId(2);
        when(productRepository.findById(2)).thenReturn(Optional.of(p));
        var opt = service.getProductById(2);
        assertTrue(opt.isPresent());
        assertEquals(2, opt.get().getId());
        verify(productRepository).findById(2);
    }

    @Test
    void testSaveAndDeleteProduct() {
        Product p = new Product(); p.setId(3);
        when(productRepository.save(p)).thenReturn(p);
        var saved = service.saveProduct(p);
        assertEquals(3, saved.getId());
        service.deleteProduct(3);
        verify(productRepository).deleteById(3);
        verify(productRepository).save(p);
    }
}

