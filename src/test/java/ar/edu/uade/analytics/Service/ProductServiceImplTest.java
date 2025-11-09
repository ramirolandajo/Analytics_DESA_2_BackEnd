package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    private ProductServiceImpl svc;

    @BeforeEach
    void setUp() {
        svc = new ProductServiceImpl(productRepository);
    }

    @Test
    void getAllProducts_returnsListFromRepository() {
        Product p1 = new Product(); p1.setId(1);
        Product p2 = new Product(); p2.setId(2);
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<Product> res = svc.getAllProducts();

        assertNotNull(res);
        assertEquals(2, res.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProductById_whenPresent_returnsOptional() {
        Product p = new Product(); p.setId(10);
        when(productRepository.findById(10)).thenReturn(Optional.of(p));

        Optional<Product> res = svc.getProductById(10);

        assertTrue(res.isPresent());
        assertEquals(10, res.get().getId());
        verify(productRepository).findById(10);
    }

    @Test
    void getProductById_whenAbsent_returnsEmptyOptional() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Product> res = svc.getProductById(99);

        assertFalse(res.isPresent());
        verify(productRepository).findById(99);
    }

    @Test
    void saveProduct_savesAndReturnsEntity() {
        Product p = new Product(); p.setId(5); // no need to set name/title in this test
        when(productRepository.save(p)).thenReturn(p);

        Product saved = svc.saveProduct(p);

        assertSame(p, saved);
        verify(productRepository).save(p);
    }

    @Test
    void deleteProduct_callsRepositoryDelete() {
        svc.deleteProduct(7);
        verify(productRepository).deleteById(7);
    }
}
