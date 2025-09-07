package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Purchase;
import java.util.List;
import java.util.Optional;

public interface PurchaseService {
    List<Purchase> getAllPurchases();
    Optional<Purchase> getPurchaseById(Integer id);
    Purchase savePurchase(Purchase purchase);
    void deletePurchase(Integer id);

    List<Purchase> findAll();

    void save(Purchase purchase);

    ar.edu.uade.analytics.Repository.ProductRepository getProductRepository();
}
