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
class FavouriteProductsServiceImplTest {

    @Mock
    private FavouriteProductsRepository repo;

    @InjectMocks
    private FavouriteProductsServiceImpl svc;

    @Test
    void getAllFavouriteProducts() {
        when(repo.findAll()).thenReturn(List.of(new FavouriteProducts()));
        assertFalse(svc.getAllFavouriteProducts().isEmpty());
    }

    @Test
    void getById_and_save_and_delete() {
        FavouriteProducts f = new FavouriteProducts();
        when(repo.findById(Long.valueOf(1L))).thenReturn(Optional.of(f));
        assertTrue(svc.getFavouriteProductById(Long.valueOf(1L)).isPresent());
        when(repo.save(f)).thenReturn(f);
        assertSame(f, svc.saveFavouriteProduct(f));
        svc.deleteFavouriteProduct(Long.valueOf(1L));
        verify(repo).deleteById(Long.valueOf(1L));
    }
}
