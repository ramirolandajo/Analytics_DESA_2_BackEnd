package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Brand;
import ar.edu.uade.analytics.Repository.BrandRepository;
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
class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandServiceImpl brandService;

    @Test
    void getAllBrands_delegatesToRepository() {
        List<Brand> expected = List.of(new Brand());
        when(brandRepository.findAll()).thenReturn(expected);
        assertEquals(expected, brandService.getAllBrands());
        verify(brandRepository).findAll();
    }

    @Test
    void getBrandById_delegatesToRepository() {
        Brand b = new Brand();
        when(brandRepository.findById(Integer.valueOf(1))).thenReturn(Optional.of(b));
        Optional<Brand> res = brandService.getBrandById(Integer.valueOf(1));
        assertTrue(res.isPresent());
        assertSame(b, res.get());
        verify(brandRepository).findById(Integer.valueOf(1));
    }

    @Test
    void saveBrand_delegatesToRepository() {
        Brand b = new Brand();
        when(brandRepository.save(b)).thenReturn(b);
        assertSame(b, brandService.saveBrand(b));
        verify(brandRepository).save(b);
    }

    @Test
    void deleteBrand_delegatesToRepository() {
        brandService.deleteBrand(Integer.valueOf(5));
        verify(brandRepository).deleteById(Integer.valueOf(5));
    }
}
