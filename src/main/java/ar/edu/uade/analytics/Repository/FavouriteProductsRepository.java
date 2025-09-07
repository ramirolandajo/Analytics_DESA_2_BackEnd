package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.FavouriteProducts;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavouriteProductsRepository extends JpaRepository<FavouriteProducts, Long> {
}

