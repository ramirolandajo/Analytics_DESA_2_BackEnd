package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Product;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<Product> getAllProducts();
    Optional<Product> getProductById(Integer id);
    Product saveProduct(Product product);
    void deleteProduct(Integer id);
}

