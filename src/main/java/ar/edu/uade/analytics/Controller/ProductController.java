package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Communication.KafkaMockService;
import ar.edu.uade.analytics.DTO.BrandDTO;
import ar.edu.uade.analytics.DTO.CategoryDTO;
import ar.edu.uade.analytics.DTO.ProductDTO;
import ar.edu.uade.analytics.Entity.Brand;
import ar.edu.uade.analytics.Entity.Category;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.Review;
import ar.edu.uade.analytics.Entity.FavouriteProducts;
import ar.edu.uade.analytics.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    KafkaMockService kafkaMockService;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    ar.edu.uade.analytics.Repository.BrandRepository brandRepository;
    @Autowired
    ar.edu.uade.analytics.Repository.CategoryRepository categoryRepository;
    @Autowired
    ar.edu.uade.analytics.Repository.ReviewRepository reviewRepository;
    @Autowired
    ar.edu.uade.analytics.Repository.FavouriteProductsRepository favouriteProductsRepository;
    @Autowired
    ar.edu.uade.analytics.Repository.StockChangeLogRepository stockChangeLogRepository;


    // Sincroniza productos desde el mock
    @Transactional(timeout = 60)
    @GetMapping("/sync")
    public List<ProductDTO> syncProductsFromMock() {
        KafkaMockService.ProductSyncMessage message = kafkaMockService.getProductsMock();
        List<ProductDTO> mockProducts = message.payload.products;
        for (ProductDTO dto : mockProducts) {
            if (dto.getProductCode() == null) continue;
            Product existing = productRepository.findAll().stream()
                    .filter(p -> p.getProductCode() != null && p.getProductCode().equals(dto.getProductCode()))
                    .findFirst().orElse(null);
            StringBuilder errorMsg = new StringBuilder();
            Brand brandEntity = null;
            if (dto.getBrand() != null && dto.getBrand().getId() != null) {
                brandEntity = brandRepository.findById((int) dto.getBrand().getId().intValue()).orElse(null);
                if (brandEntity == null) {
                    errorMsg.append("Marca no encontrada (ID: " + dto.getBrand().getId() + ") para producto: " + dto.getTitle() + " (productCode: " + dto.getProductCode() + "). ");
                }
            }
            Set<Category> cats = null;
            if (dto.getCategories() != null && !dto.getCategories().isEmpty()) {
                // Filtrar elementos nulos antes de mapear
                List<CategoryDTO> filteredCats = dto.getCategories().stream().filter(catDto -> catDto != null && catDto.getId() != null).toList();
                cats = filteredCats.stream()
                        .map(catDto -> categoryRepository.findById((int) catDto.getId().intValue()).orElse(null))
                        .collect(Collectors.toSet());
                // Revisar si alguna categoría no existe
                for (int i = 0; i < filteredCats.size(); i++) {
                    if (cats.toArray()[i] == null) {
                        errorMsg.append("Categoría no encontrada para producto: " + dto.getTitle() + " (productCode: " + dto.getProductCode() + "). ");
                    }
                }
            }
            if (errorMsg.length() > 0) {
                throw new RuntimeException(errorMsg.toString());
            }
            // Al asignar mediaSrc, usar nueva lista mutable
            List<String> mediaSrcMutable = dto.getMediaSrc() != null ? new java.util.ArrayList<>(dto.getMediaSrc()) : new java.util.ArrayList<>();
            if (existing == null) {
                Product product = new Product();
                product.setTitle(dto.getTitle());
                product.setDescription(dto.getDescription());
                product.setPrice(dto.getPrice());
                product.setStock(dto.getStock());
                product.setMediaSrc(mediaSrcMutable);
                product.setNew(dto.getIsNew() != null ? dto.getIsNew() : false);
                product.setBestseller(dto.getIsBestseller() != null ? dto.getIsBestseller() : false);
                product.setFeatured(dto.getIsFeatured() != null ? dto.getIsFeatured() : false);
                product.setHero(dto.getHero() != null ? dto.getHero() : false);
                product.setActive(dto.getActive() != null ? dto.getActive() : true);
                product.setDiscount(dto.getDiscount());
                product.setPriceUnit(dto.getPriceUnit());
                product.setProductCode(dto.getProductCode());
                product.setBrand(brandEntity);
                if (cats != null) {
                    product.setCategories(new java.util.HashSet<>(cats.stream().filter(c -> c != null).toList()));
                }
                product.setCalification(dto.getCalification() != null ? dto.getCalification() : 0f);
                productRepository.save(product);
            } else {
                existing.setTitle(dto.getTitle());
                existing.setDescription(dto.getDescription());
                existing.setPrice(dto.getPrice());
                existing.setStock(dto.getStock());
                existing.setMediaSrc(mediaSrcMutable);
                existing.setNew(dto.getIsNew() != null ? dto.getIsNew() : false);
                existing.setBestseller(dto.getIsBestseller() != null ? dto.getIsBestseller() : false);
                existing.setFeatured(dto.getIsFeatured() != null ? dto.getIsFeatured() : false);
                existing.setHero(dto.getHero() != null ? dto.getHero() : false);
                existing.setActive(dto.getActive() != null ? dto.getActive() : true);
                existing.setDiscount(dto.getDiscount());
                existing.setPriceUnit(dto.getPriceUnit());
                existing.setProductCode(dto.getProductCode());
                existing.setBrand(brandEntity);
                if (cats != null) {
                    existing.setCategories(new java.util.HashSet<>(cats.stream().filter(c -> c != null).toList()));
                } else {
                    existing.setCategories(null);
                }
                existing.setCalification(dto.getCalification() != null ? dto.getCalification() : 0f);
                productRepository.save(existing);
            }
        }
        // Retornar todos los productos como DTO
        return productRepository.findAll().stream()
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Obtiene todos los productos
    @GetMapping
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Agrega un producto específico usando mensaje mockeado
    @PostMapping
    public ProductDTO addProduct() {
        KafkaMockService.AddProductMessage msg = kafkaMockService.getAddProductMock();
        ProductDTO dto = msg.payload.product;
        Product product = new Product();
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setMediaSrc(dto.getMediaSrc() != null ? dto.getMediaSrc() : List.of());
        product.setNew(dto.getIsNew() != null ? dto.getIsNew() : false);
        product.setBestseller(dto.getIsBestseller() != null ? dto.getIsBestseller() : false);
        product.setFeatured(dto.getIsFeatured() != null ? dto.getIsFeatured() : false);
        product.setHero(dto.getHero() != null ? dto.getHero() : false);
        product.setActive(dto.getActive() != null ? dto.getActive() : true);
        product.setDiscount(dto.getDiscount());
        product.setPriceUnit(dto.getPriceUnit());
        product.setProductCode(dto.getProductCode());
        product.setBrand(dto.getBrand() != null ? brandRepository.findById((int) dto.getBrand().getId().intValue()).orElse(null) : null);
        if (dto.getCategories() != null && !dto.getCategories().isEmpty()) {
            Set<Category> cats = dto.getCategories().stream()
                    .map(catDto -> categoryRepository.findById((int) catDto.getId().intValue()).orElse(null))
                    .filter(c -> c != null)
                    .collect(Collectors.toSet());
            product.setCategories(new java.util.HashSet<>(cats));
        } else {
            product.setCategories(null);
        }
        product.setCalification(dto.getCalification() != null ? dto.getCalification() : 0f);
        Product saved = productRepository.save(product);
        return toDTO(saved);
    }

    // Edita solo precio y stock usando mensaje mockeado
    @PatchMapping("/simple")
    public ProductDTO editProductSimple() {
        KafkaMockService.EditProductSimpleMessage msg = kafkaMockService.getEditProductMockSimple();
        KafkaMockService.EditProductSimplePayload dto = msg.payload;
        Integer productCode = dto.productCode;
        Product product = productRepository.findByProductCode(productCode);
        if (product == null) throw new RuntimeException("Producto no encontrado");
        if (dto.price != null) {
            product.setPrice(dto.price);
            Float discount = product.getDiscount() != null ? product.getDiscount() : 0f;
            Float priceUnit = dto.price / (1 - (discount / 100f));
            product.setPriceUnit(priceUnit);
        }
        if (dto.stock != null) product.setStock(dto.stock);
        Product updated = productRepository.save(product);
        return toDTO(updated);
    }

    // Edita el producto completo usando mensaje mockeado
    @PatchMapping
    public ProductDTO editProduct() {
        KafkaMockService.EditProductFullMessage msg = kafkaMockService.getEditProductMockFull();
        ProductDTO dto = msg.payload;
        Integer id = Math.toIntExact(dto.getId());
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) throw new RuntimeException("Producto no encontrado");
        Product product = productOpt.get();
        // Campos simples
        if (dto.getTitle() != null) product.setTitle(dto.getTitle());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getStock() != null) product.setStock(dto.getStock());
        // MediaSrc: si es null, setear lista vacía
        if (dto.getMediaSrc() == null) {
            product.setMediaSrc(List.of());
        } else {
            product.setMediaSrc(new java.util.ArrayList<>(dto.getMediaSrc()));
        }
        // Booleanos: solo asignar si no son null
        if (dto.getIsNew() != null) product.setNew(dto.getIsNew());
        if (dto.getIsBestseller() != null) product.setBestseller(dto.getIsBestseller());
        if (dto.getIsFeatured() != null) product.setIsFeatured(dto.getIsFeatured());
        if (dto.getHero() != null) product.setHero(dto.getHero());
        // Active
        if (dto.getActive() != null) product.setActive(dto.getActive());
        // ProductCode
        if (dto.getProductCode() == null) {
            product.setProductCode(null);
        } else {
            product.setProductCode(dto.getProductCode());
        }
        // PriceUnit y Discount: primero setear ambos
        if (dto.getPriceUnit() != null) product.setPriceUnit(dto.getPriceUnit());
        if (dto.getDiscount() != null) product.setDiscount(dto.getDiscount());
        // Recalcular price si priceUnit o discount se tocan y ambos existen (usar los valores ya actualizados)
        Float priceUnit = product.getPriceUnit();
        Float discount = product.getDiscount();
        if ((dto.getPriceUnit() != null || dto.getDiscount() != null) && priceUnit != null && discount != null) {
            Float price = priceUnit - (priceUnit * (discount / 100f));
            product.setPrice(price);
        } else if (dto.getPrice() != null) {
            product.setPrice(dto.getPrice());
        }
        // Marca
        if (dto.getBrand() != null && dto.getBrand().getId() != null) {
            product.setBrand(brandRepository.findById((int) dto.getBrand().getId().intValue()).orElse(null));
        } else {
            product.setBrand(null);
        }
        // Categorías
        if (dto.getCategories() != null && !dto.getCategories().isEmpty()) {
            // Filtrar elementos nulos antes de mapear y obtener entidades existentes
            List<CategoryDTO> filteredCats = dto.getCategories().stream()
                    .filter(catDto -> catDto != null && catDto.getId() != null)
                    .toList();
            Set<Category> cats = filteredCats.stream()
                    .map(catDto -> categoryRepository.findById((int) catDto.getId().intValue()).orElse(null))
                    .filter(c -> c != null)
                    .collect(Collectors.toSet());
            product.setCategories(new java.util.HashSet<>(cats));
        } else {
            product.setCategories(null);
        }
        // Calificación
        product.setCalification(dto.getCalification() != null ? dto.getCalification() : 0f);
        Product updated = productRepository.save(product);
        return toDTO(updated);
    }

    // Activar producto usando mensaje mockeado
    @PatchMapping("/activate")
    public ProductDTO activateProduct() {
        KafkaMockService.ActivateProductMessage msg = kafkaMockService.getActivateProductMock();
        Long id = msg.payload.id;
        Optional<Product> productOpt = productRepository.findById(id.intValue());
        if (productOpt.isEmpty()) throw new RuntimeException("Producto no encontrado");
        Product product = productOpt.get();
        product.setActive(true);
        Product updated = productRepository.save(product);
        return toDTO(updated);
    }

    // Desactivar producto usando mensaje mockeado
    @PatchMapping("/deactivate")
    public ProductDTO deactivateProduct() {
        KafkaMockService.DeactivateProductMessage msg = kafkaMockService.getDeactivateProductMock();
        Long id = msg.payload.id;
        Optional<Product> productOpt = productRepository.findById(id.intValue());
        if (productOpt.isEmpty()) throw new RuntimeException("Producto no encontrado");
        Product product = productOpt.get();
        product.setActive(false);
        Product updated = productRepository.save(product);
        return toDTO(updated);
    }

    // DTO para request de review
    public static class ReviewRequest {
        private float calification;
        private String description;
        public float getCalification() { return calification; }
        public void setCalification(float calification) { this.calification = calification; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    // DTO para response de reviews
    public static class ReviewDTO {
        private Long id;
        private float calification;
        private String description;
        public ReviewDTO(Long id, float calification, String description) {
            this.id = id;
            this.calification = calification;
            this.description = description;
        }
        public Long getId() { return id; }
        public float getCalification() { return calification; }
        public String getDescription() { return description; }
    }

    public static class ReviewResponse {
        private Integer productId;
        private String productTitle;
        private float promedio;
        private List<ReviewDTO> reviews;
        public ReviewResponse(Integer productId, String productTitle, float promedio, List<ReviewDTO> reviews) {
            this.productId = productId;
            this.productTitle = productTitle;
            this.promedio = promedio;
            this.reviews = reviews;
        }
        public Integer getProductId() { return productId; }
        public String getProductTitle() { return productTitle; }
        public float getPromedio() { return promedio; }
        public List<ReviewDTO> getReviews() { return reviews; }
    }

    // Califica un producto (ahora crea una review desde evento simulado)
    @PostMapping("/review/simulate")
    public ReviewResponse addReviewFromEvent() {
        // Simular recepción de evento de review desde KafkaMockService
        KafkaMockService.ProductReviewMockMessage event = kafkaMockService.getProductReviewMock();
        Integer productId = event.payload.productId;
        float calification = event.payload.calification;
        String description = event.payload.description;
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) throw new RuntimeException("Producto no encontrado");
        Product product = productOpt.get();
        Review review = new Review();
        review.setProduct(product);
        review.setCalification(calification);
        review.setDescription(description);
        reviewRepository.save(review);
        List<Review> reviews = reviewRepository.findByProduct(product);
        float promedio = (float) reviews.stream().mapToDouble(Review::getCalification).average().orElse(0.0);
        // Actualizar el campo calification en Product
        product.setCalification(promedio);
        productRepository.save(product);
        List<ReviewDTO> reviewDTOs = reviews.stream()
                .map(r -> new ReviewDTO(r.getId(), r.getCalification(), r.getDescription()))
                .collect(Collectors.toList());
        return new ReviewResponse(product.getId(), product.getTitle(), promedio, reviewDTOs);
    }

    // Sincroniza una review desde evento mockeado tipo Kafka
    @PostMapping("/review/sync-mock")
    public ReviewResponse syncReviewFromMock() {
        KafkaMockService.ProductReviewMockMessage event = kafkaMockService.getProductReviewMock();
        // Extraer datos del evento
        Integer productId = event.payload.productId;
        float calification = event.payload.calification;
        String description = event.payload.description;
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) throw new RuntimeException("Producto no encontrado");
        Product product = productOpt.get();
        Review review = new Review();
        review.setProduct(product);
        review.setCalification(calification);
        review.setDescription(description);
        reviewRepository.save(review);
        List<Review> reviews = reviewRepository.findByProduct(product);
        float promedio = (float) reviews.stream().mapToDouble(Review::getCalification).average().orElse(0.0);
        // Actualizar el campo calification en Product
        product.setCalification(promedio);
        productRepository.save(product);
        List<ReviewDTO> reviewDTOs = reviews.stream()
                .map(r -> new ReviewDTO(r.getId(), r.getCalification(), r.getDescription()))
                .collect(Collectors.toList());
        return new ReviewResponse(product.getId(), product.getTitle(), promedio, reviewDTOs);
    }

    // Sincroniza un listado de reviews desde eventos mockeados tipo Kafka
    @Transactional(timeout = 120)
    @PostMapping("/review/sync-mock-list")
    public ReviewResponse syncReviewListFromMock() {
        List<KafkaMockService.ProductReviewMockMessage> events = kafkaMockService.getProductReviewMockList();
        ReviewResponse lastResponse = null;
        for (KafkaMockService.ProductReviewMockMessage event : events) {
            Integer productId = event.payload.productId;
            float calification = event.payload.calification;
            String description = event.payload.description;
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) continue; // Omitir si no existe
            Product product = productOpt.get();
            Review review = new Review();
            review.setProduct(product);
            review.setCalification(calification);
            review.setDescription(description);
            reviewRepository.save(review);
            List<Review> reviews = reviewRepository.findByProduct(product);
            float promedio = (float) reviews.stream().mapToDouble(Review::getCalification).average().orElse(0.0);
            product.setCalification(promedio);
            productRepository.save(product);
            List<ReviewDTO> reviewDTOs = reviews.stream()
                    .map(r -> new ReviewDTO(r.getId(), r.getCalification(), r.getDescription()))
                    .collect(Collectors.toList());
            lastResponse = new ReviewResponse(product.getId(), product.getTitle(), promedio, reviewDTOs);
        }
        return lastResponse;
    }

    // Obtener producto por id (solo el producto, sin relacionados)
    @GetMapping("/{id}")
    public ProductDTO getProductById(@PathVariable("id") Long id) {
        Optional<Product> productOpt = productRepository.findById(id.intValue());
        if (productOpt.isEmpty()) throw new RuntimeException("Producto no encontrado");
        Product product = productOpt.get();
        return toDTO(product);
    }



    // Conversión a DTO
    ProductDTO toDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId() != null ? Long.valueOf(product.getId()) : null);
        dto.setTitle(product.getTitle());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setMediaSrc(product.getMediaSrc());
        dto.setCalification(product.getCalification());
        dto.setDiscount(product.getDiscount());
        dto.setPriceUnit(product.getPriceUnit());
        dto.setProductCode(product.getProductCode());
        // Si el campo active es null, setear null en el DTO
        dto.setActive(product.getActive() != null ? product.getActive() : null);
        // Devuelve el nombre de la marca y categorías
        if (product.getBrand() != null) {
            dto.setBrand(new BrandDTO(Long.valueOf(product.getBrand().getId()), product.getBrand().getName(), product.getBrand().isActive()));
        } else {
            dto.setBrand(null);
        }
        if (product.getCategories() != null && !product.getCategories().isEmpty()) {
            dto.setCategories(product.getCategories().stream()
                    .map(cat -> new CategoryDTO(Long.valueOf(cat.getId()), cat.getName(), cat.isActive()))
                    .collect(Collectors.toList()));
        } else {
            dto.setCategories(null);
        }
        dto.setIsNew(product.getIsNew());
        dto.setIsBestseller(product.isIsBestseller());
        dto.setIsFeatured(product.isIsFeatured());
        dto.setHero(product.isHero());
        return dto;
    }

    // Endpoint para sincronizar producto favorito desde evento mockeado
    @PostMapping("/sync-mock-favourite")
    public ResponseEntity<String> syncMockFavouriteProduct() {
        KafkaMockService.AddFavouriteProductMessage event = kafkaMockService.getAddFavouriteProductMock();
        if (event == null || event.payload == null) {
            return ResponseEntity.badRequest().body("No se encontró evento de producto favorito");
        }
        Integer productCode = event.payload.getProductCode();
        Integer id = event.payload.getId();
        String nombre = event.payload.getNombre();
        // Buscar si el producto existe
        Product product = productRepository.findByProductCode(productCode);
        if (product == null) {
            product = new Product();
            product.setId(id);
            product.setProductCode(productCode);
            product.setTitle(nombre);
            productRepository.save(product);
        }
        // Persistir como favorito en FavouriteProducts
        FavouriteProducts favourite = new FavouriteProducts();
        favourite.setProduct(product);
        favourite.setProductCode(productCode);
        // Guardar en base de datos
        favouriteProductsRepository.save(favourite);
        return ResponseEntity.ok("Producto favorito sincronizado correctamente");
    }

    // Endpoint para sincronizar productos favoritos desde evento mockeado (listado)
    @Transactional (timeout = 60)
    @PostMapping("/sync-mock-favourites")
    public ResponseEntity<String> syncMockFavouriteProducts() {
        List<KafkaMockService.AddFavouriteProductMessage> events = kafkaMockService.getAddFavouriteProductsMock();
        if (events == null || events.isEmpty()) {
            return ResponseEntity.badRequest().body("No se encontraron eventos de productos favoritos");
        }
        for (KafkaMockService.AddFavouriteProductMessage event : events) {
            if (event == null || event.payload == null) continue;
            Integer productCode = event.payload.getProductCode();
            Integer id = event.payload.getId();
            String nombre = event.payload.getNombre();
            Product product = productRepository.findByProductCode(productCode);
            if (product == null) {
                product = new Product();
                product.setId(id);
                product.setProductCode(productCode);
                product.setTitle(nombre);
                productRepository.save(product);
            }
            FavouriteProducts favourite = new FavouriteProducts();
            favourite.setProduct(product);
            favourite.setProductCode(productCode);
            favouriteProductsRepository.save(favourite);
        }
        return ResponseEntity.ok("Productos favoritos sincronizados correctamente");
    }

    // Sincroniza cambios de stock desde el mock usando StockChangeLog
    @Transactional(timeout = 60)
    @PostMapping("/sync-mock-stock-changes")
    public ResponseEntity<String> syncMockStockChanges(@RequestBody(required = false) List<java.util.Map<String, Object>> events) {
        if (events == null || events.isEmpty()) {
            return ResponseEntity.badRequest().body("no se encontraron eventos");
        }
        int procesados = 0;
        for (java.util.Map<String, Object> event : events) {
            if (event == null) continue;
            String type = event.get("type") != null ? event.get("type").toString() : null;
            String timestampStr = event.get("timestamp") != null ? event.get("timestamp").toString() : null;
            java.time.LocalDateTime timestamp;
            try {
                timestamp = timestampStr != null ? java.time.LocalDateTime.parse(timestampStr) : java.time.LocalDateTime.now();
            } catch (Exception e) {
                timestamp = java.time.LocalDateTime.now();
            }
            Object payloadObj = event.get("payload");
            if (!(payloadObj instanceof java.util.Map)) continue;
            java.util.Map<String, Object> payload = (java.util.Map<String, Object>) payloadObj;
            Integer productCode = null;
            Integer nuevoStock = null;
            try {
                Object pc = payload.get("productCode");
                Object ns = payload.get("stock");
                if (pc != null) productCode = Integer.valueOf(pc.toString());
                if (ns != null) nuevoStock = Integer.valueOf(ns.toString());
            } catch (Exception e) {
                continue;
            }
            if (productCode == null || nuevoStock == null) continue;
            Product product = productRepository.findByProductCode(productCode);
            if (product == null) continue;
            Integer stockAnterior = product.getStock();
            product.setStock(nuevoStock);
            productRepository.save(product);
            // Registrar en StockChangeLog
            ar.edu.uade.analytics.Entity.StockChangeLog log = new ar.edu.uade.analytics.Entity.StockChangeLog();
            log.setProduct(product);
            log.setOldStock(stockAnterior);
            log.setNewStock(nuevoStock);
            log.setQuantityChanged(nuevoStock - (stockAnterior != null ? stockAnterior : 0));
            log.setChangedAt(timestamp);
            log.setReason(type != null ? type : "Cambio de stock");
            stockChangeLogRepository.save(log);
            procesados++;
        }
        if (procesados == 0) {
            return ResponseEntity.badRequest().body("No se pudo procesar ningún cambio de stock válido");
        }
        return ResponseEntity.ok("Cambios de stock procesados correctamente: " + procesados);
    }

    // Sincroniza cambios de stock desde una lista enviada por el cliente (JSON)
    @Transactional(timeout = 60)
    @PostMapping("/sync-stock-changes")
    public ResponseEntity<String> syncStockChanges(@RequestBody(required = false) List<java.util.Map<String, Object>> events) {
        if (events == null || events.isEmpty()) {
            return ResponseEntity.badRequest().body("no se encontraron eventos");
        }
        int procesados = 0;
        for (java.util.Map<String, Object> event : events) {
            if (event == null) continue;
            String type = event.get("type") != null ? event.get("type").toString() : null;
            String timestampStr = event.get("timestamp") != null ? event.get("timestamp").toString() : null;
            java.time.LocalDateTime timestamp;
            try {
                timestamp = timestampStr != null ? java.time.LocalDateTime.parse(timestampStr) : java.time.LocalDateTime.now();
            } catch (Exception e) {
                timestamp = java.time.LocalDateTime.now();
            }
            Object payloadObj = event.get("payload");
            if (!(payloadObj instanceof java.util.Map)) continue;
            java.util.Map<String, Object> payload = (java.util.Map<String, Object>) payloadObj;
            Integer productCode = null;
            Integer nuevoStock = null;
            try {
                Object pc = payload.get("productCode");
                Object ns = payload.get("stock");
                if (pc != null) productCode = Integer.valueOf(pc.toString());
                if (ns != null) nuevoStock = Integer.valueOf(ns.toString());
            } catch (Exception e) {
                continue;
            }
            if (productCode == null || nuevoStock == null) continue;
            Product product = productRepository.findByProductCode(productCode);
            if (product == null) continue;
            Integer stockAnterior = product.getStock();
            product.setStock(nuevoStock);
            productRepository.save(product);
            // Registrar en StockChangeLog
            ar.edu.uade.analytics.Entity.StockChangeLog log = new ar.edu.uade.analytics.Entity.StockChangeLog();
            log.setProduct(product);
            log.setOldStock(stockAnterior);
            log.setNewStock(nuevoStock);
            log.setQuantityChanged(nuevoStock - (stockAnterior != null ? stockAnterior : 0));
            log.setChangedAt(timestamp);
            log.setReason(type != null ? type : "Cambio de stock");
            stockChangeLogRepository.save(log);
            procesados++;
        }
        if (procesados == 0) {
            return ResponseEntity.badRequest().body("No se pudo procesar ningún cambio de stock válido");
        }
        return ResponseEntity.ok("Cambios de stock procesados correctamente: " + procesados);
    }

    // Sincroniza cambios de stock desde el mock usando eventos simples (productCode, stock, price)
    @Transactional(timeout = 120)
    @PostMapping("/sync-mock-stock-changes-simple")
    public ResponseEntity<String> syncMockStockChangesSimple() {
        List<KafkaMockService.EditProductSimpleMessage> events = kafkaMockService.getEditProductMockSimpleList();
        if (events == null || events.isEmpty()) {
            return ResponseEntity.badRequest().body("no se encontraron eventos");
        }
        int procesados = 0;
        for (KafkaMockService.EditProductSimpleMessage event : events) {
            if (event == null || event.payload == null) continue;
            Integer productCode = event.payload.productCode;
            Integer nuevoStock = event.payload.stock;
            Float nuevoPrecio = event.payload.price;
            String type = event.type;
            java.time.LocalDateTime timestamp;
            try {
                timestamp = event.timestamp != null ? java.time.LocalDateTime.parse(event.timestamp) : java.time.LocalDateTime.now();
            } catch (Exception e) {
                timestamp = java.time.LocalDateTime.now();
            }
            Product product = productRepository.findByProductCode(productCode);
            if (product == null) continue;
            Integer stockAnterior = product.getStock();
            // Registrar en StockChangeLog antes de modificar Product
            ar.edu.uade.analytics.Entity.StockChangeLog log = new ar.edu.uade.analytics.Entity.StockChangeLog();
            log.setProduct(product);
            log.setOldStock(stockAnterior);
            log.setNewStock(nuevoStock);
            log.setQuantityChanged((nuevoStock != null && stockAnterior != null) ? (nuevoStock - stockAnterior) : 0);
            log.setChangedAt(timestamp);
            log.setReason(type != null ? type : "Cambio de stock");
            stockChangeLogRepository.save(log);
            // Actualizar Product
            if (nuevoStock != null) product.setStock(nuevoStock);
            if (nuevoPrecio != null) product.setPrice(nuevoPrecio);
            productRepository.save(product);
            procesados++;
        }
        if (procesados == 0) {
            return ResponseEntity.badRequest().body("No se pudo procesar ningún cambio de stock válido");
        }
        return ResponseEntity.ok("Cambios de stock procesados correctamente: " + procesados);
    }


    // Endpoint para obtener todos los productos, categorías y marcas juntos
        @GetMapping("/all-data")
        public ResponseEntity<java.util.Map<String, Object>> getAllProductsCategoriesBrands() {
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("products", productRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList()));
            result.put("categories", categoryRepository.findAll().stream()
                    .map(cat -> new CategoryDTO(Long.valueOf(cat.getId()), cat.getName(), cat.isActive()))
                    .collect(Collectors.toList()));
            result.put("brands", brandRepository.findAll().stream()
                    .map(brand -> new BrandDTO(Long.valueOf(brand.getId()), brand.getName(), brand.isActive()))
                    .collect(Collectors.toList()));
            return ResponseEntity.ok(result);
        }



    @GetMapping("/low-stock")
    public List<ProductDTO> getLowStockProducts() {
        List<Product> lowStockProducts = productRepository.findByStockLessThanEqual(10);
        return lowStockProducts.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());}



    //get por por id de producto
    @GetMapping("/by-code/{productCode}")
    public ProductDTO getProductByCode(@PathVariable("productCode") Integer productCode) {
        Product product = productRepository.findByProductCode(productCode);
        if (product == null) throw new RuntimeException("Producto no encontrado");
        return toDTO(product);
    }
}


