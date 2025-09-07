package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.FavouriteProducts;
import ar.edu.uade.analytics.Repository.FavouriteProductsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FavouriteProductsServiceImpl implements FavouriteProductsService {

    private final FavouriteProductsRepository favouriteProductsRepository;

    @Autowired
    public FavouriteProductsServiceImpl(FavouriteProductsRepository favouriteProductsRepository) {
        this.favouriteProductsRepository = favouriteProductsRepository;
    }

    @Override
    public List<FavouriteProducts> getAllFavouriteProducts() {
        return favouriteProductsRepository.findAll();
    }

    @Override
    public Optional<FavouriteProducts> getFavouriteProductById(Long id) {
        return favouriteProductsRepository.findById(id);
    }

    @Override
    public FavouriteProducts saveFavouriteProduct(FavouriteProducts favouriteProducts) {
        return favouriteProductsRepository.save(favouriteProducts);
    }

    @Override
    public void deleteFavouriteProduct(Long id) {
        favouriteProductsRepository.deleteById(id);
    }
}

