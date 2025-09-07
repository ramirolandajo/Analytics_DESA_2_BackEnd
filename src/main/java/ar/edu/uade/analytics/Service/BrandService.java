package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Brand;
import java.util.List;
import java.util.Optional;

public interface BrandService {
    List<Brand> getAllBrands();
    Optional<Brand> getBrandById(Integer id);
    Brand saveBrand(Brand brand);
    void deleteBrand(Integer id);
}

