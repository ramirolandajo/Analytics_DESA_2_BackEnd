package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Integer> {
    Brand findByBrandCode(Integer brandCode);
    Brand findByNameIgnoreCase(String name);
}
