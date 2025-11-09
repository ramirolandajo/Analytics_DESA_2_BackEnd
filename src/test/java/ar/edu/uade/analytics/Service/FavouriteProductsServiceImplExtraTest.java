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
class FavouriteProductsServiceImplExtraTest {

    @Mock private FavouriteProductsRepository repo;
    @InjectMocks private FavouriteProductsServiceImpl svc;

    @Test
    void basicCrudOperations_delegateToRepository() {
        FavouriteProducts f = new FavouriteProducts();
        when(repo.findAll()).thenReturn(List.of(f));
        assertEquals(1, svc.getAllFavouriteProducts().size());

        when(repo.findById(1L)).thenReturn(Optional.of(f));
        assertTrue(svc.getFavouriteProductById(1L).isPresent());

        when(repo.save(f)).thenReturn(f);
        assertSame(f, svc.saveFavouriteProduct(f));

        svc.deleteFavouriteProduct(5L);
        verify(repo).deleteById(5L);
    }
}

