package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.FavouriteProducts;
import ar.edu.uade.analytics.Repository.FavouriteProductsRepository;
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
public class FavouriteProductsServiceImplTest {

    @Mock
    private FavouriteProductsRepository favouriteProductsRepository;

    @InjectMocks
    private FavouriteProductsServiceImpl service;

    @Test
    void testGetAllFavouriteProducts() {
        FavouriteProducts f = new FavouriteProducts(); f.setId(1L); f.setProductCode(100);
        when(favouriteProductsRepository.findAll()).thenReturn(List.of(f));
        var res = service.getAllFavouriteProducts();
        assertNotNull(res);
        assertEquals(1, res.size());
        verify(favouriteProductsRepository).findAll();
    }

    @Test
    void testGetFavouriteProductById() {
        FavouriteProducts f = new FavouriteProducts(); f.setId(2L); f.setProductCode(101);
        when(favouriteProductsRepository.findById(2L)).thenReturn(Optional.of(f));
        var opt = service.getFavouriteProductById(2L);
        assertTrue(opt.isPresent());
        assertEquals(2, opt.get().getId());
        verify(favouriteProductsRepository).findById(2L);
    }

    @Test
    void testSaveAndDeleteFavouriteProduct() {
        FavouriteProducts f = new FavouriteProducts(); f.setId(3L);
        when(favouriteProductsRepository.save(f)).thenReturn(f);
        var saved = service.saveFavouriteProduct(f);
        assertEquals(3, saved.getId());
        service.deleteFavouriteProduct(3L);
        verify(favouriteProductsRepository).deleteById(3L);
        verify(favouriteProductsRepository).save(f);
    }
}

