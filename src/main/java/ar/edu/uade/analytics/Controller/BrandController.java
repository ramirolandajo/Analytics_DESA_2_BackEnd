package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Communication.KafkaMockService;
import ar.edu.uade.analytics.DTO.BrandDTO;
import ar.edu.uade.analytics.Entity.Brand;
import ar.edu.uade.analytics.Service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/brand")
public class BrandController {
    @Autowired
    private KafkaMockService kafkaMockService;

    @Autowired
    private BrandService brandService;

    @GetMapping("/sync")
    public List<BrandDTO> syncBrandsFromMock() {
        KafkaMockService.BrandSyncMessage message = kafkaMockService.getBrandsMock();
        List<BrandDTO> mockBrands = message.payload.brands;
        List<Brand> existingBrands = brandService.getAllBrands();
        for (BrandDTO dto : mockBrands) {
            Brand existing = existingBrands.stream()
                    .filter(b -> b.getName() != null && b.getName().equalsIgnoreCase(dto.getName()))
                    .findFirst()
                    .orElse(null);
            if (existing == null) {
                Brand b = new Brand();
                b.setName(dto.getName());
                b.setActive(dto.getActive() != null ? dto.getActive() : true);
                brandService.saveBrand(b);
            }
        }
        // Imprimir el mensaje recibido del mock en formato core de mensajería
        System.out.println("Mensaje recibido del core de mensajería:");
        System.out.println("{" +
                "type='" + message.type + "', " +
                "payload=" + message.payload + ", " +
                "timestamp=" + message.timestamp +
                "}");
        return brandService.getAllBrands().stream()
                .map(b -> new BrandDTO(Long.valueOf(b.getId()), b.getName(), b.isActive()))
                .collect(Collectors.toList());
    }

    @GetMapping
    public List<BrandDTO> getAllBrands() {
        return brandService.getAllBrands().stream()
                .map(b -> new BrandDTO(Long.valueOf(b.getId()), b.getName()))
                .collect(Collectors.toList());
    }

    @PostMapping("/mock/add")
    public BrandDTO addBrandFromMock() {
        KafkaMockService.BrandSyncMessage message = kafkaMockService.getBrandsMock();
        // Tomar la primera marca del mock como ejemplo
        BrandDTO brandDTO = message.payload.brands.get(0);
        Brand existing = brandService.getAllBrands().stream()
                .filter(b -> (b.getName() == null && brandDTO.getName() == null) ||
                        (b.getName() != null && b.getName().equalsIgnoreCase(brandDTO.getName())))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return new BrandDTO(Long.valueOf(existing.getId()), existing.getName(), existing.isActive());
        }
        Brand brand = new Brand();
        brand.setName(brandDTO.getName());
        brand.setActive(brandDTO.getActive() != null ? brandDTO.getActive() : true);
        Brand saved = brandService.saveBrand(brand);
        return new BrandDTO(Long.valueOf(saved.getId()), saved.getName(), saved.isActive());
    }

    @PatchMapping("/mock/activate")
    public BrandDTO activateBrandFromMock() {
        KafkaMockService.BrandSyncMessage message = kafkaMockService.getBrandsMock();
        // Tomar la primera marca del mock como ejemplo
        BrandDTO brandDTO = message.payload.brands.get(0);
        Brand brand = brandService.getAllBrands().stream()
                .filter(b -> b.getName() != null && b.getName().equalsIgnoreCase(brandDTO.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Marca no encontrada"));
        brand.setActive(true);
        Brand updated = brandService.saveBrand(brand);
        return new BrandDTO(Long.valueOf(updated.getId()), updated.getName(), updated.isActive());
    }

    @PatchMapping("/mock/deactivate")
    public BrandDTO deactivateBrandFromMock() {
        KafkaMockService.BrandSyncMessage message = kafkaMockService.getBrandsMock();
        // Tomar la primera marca del mock como ejemplo
        BrandDTO brandDTO = message.payload.brands.get(0);
        Brand brand = brandService.getAllBrands().stream()
                .filter(b -> b.getName() != null && b.getName().equalsIgnoreCase(brandDTO.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Marca no encontrada"));
        brand.setActive(false);
        Brand updated = brandService.saveBrand(brand);
        return new BrandDTO(Long.valueOf(updated.getId()), updated.getName(), updated.isActive());
    }

    @PatchMapping("/mock/update")
    public BrandDTO updateBrandFromMock() {
        KafkaMockService.BrandSyncMessage message = kafkaMockService.getBrandsMock();
        // Tomar la primera marca del mock como ejemplo
        BrandDTO brandDTO = message.payload.brands.get(0);
        Brand brand = brandService.getAllBrands().stream()
                .filter(b -> b.getName() != null && b.getName().equalsIgnoreCase(brandDTO.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Marca no encontrada"));
        // Actualizar el nombre y el estado activo según el mock
        brand.setName(brandDTO.getName());
        if (brandDTO.getActive() != null) {
            brand.setActive(brandDTO.getActive());
        }
        Brand updated = brandService.saveBrand(brand);
        return new BrandDTO(Long.valueOf(updated.getId()), updated.getName(), updated.isActive());
    }
}
