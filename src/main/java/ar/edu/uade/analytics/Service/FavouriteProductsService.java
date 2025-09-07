package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.FavouriteProducts;
import java.util.List;
import java.util.Optional;

public interface FavouriteProductsService {
    List<FavouriteProducts> getAllFavouriteProducts();
    Optional<FavouriteProducts> getFavouriteProductById(Long id);
    FavouriteProducts saveFavouriteProduct(FavouriteProducts favouriteProducts);
    void deleteFavouriteProduct(Long id);
}

