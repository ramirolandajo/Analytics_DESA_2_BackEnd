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
public class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandServiceImpl service;

    @Test
    void testGetAllBrands() {
        Brand b = new Brand(); b.setId(1); b.setName("B1");
        when(brandRepository.findAll()).thenReturn(List.of(b));
        var res = service.getAllBrands();
        assertNotNull(res);
        assertEquals(1, res.size());
        verify(brandRepository).findAll();
    }

    @Test
    void testGetBrandById() {
        Brand b = new Brand(); b.setId(2); b.setName("B2");
        when(brandRepository.findById(2)).thenReturn(Optional.of(b));
        var opt = service.getBrandById(2);
        assertTrue(opt.isPresent());
        assertEquals(2, opt.get().getId());
        verify(brandRepository).findById(2);
    }

    @Test
    void testSaveAndDeleteBrand() {
        Brand b = new Brand(); b.setId(3);
        when(brandRepository.save(b)).thenReturn(b);
        var saved = service.saveBrand(b);
        assertEquals(3, saved.getId());
        service.deleteBrand(3);
        verify(brandRepository).deleteById(3);
        verify(brandRepository).save(b);
    }
}

