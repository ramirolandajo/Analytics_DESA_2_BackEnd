package ar.edu.uade.analytics.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ar.edu.uade.analytics.Communication.KafkaMockService kafkaMockService;
    @MockBean
    private ar.edu.uade.analytics.Repository.ProductRepository productRepository;
    @MockBean
    private ar.edu.uade.analytics.Repository.BrandRepository brandRepository;
    @MockBean
    private ar.edu.uade.analytics.Repository.CategoryRepository categoryRepository;
    @MockBean
    private ar.edu.uade.analytics.Repository.ReviewRepository reviewRepository;
    @MockBean
    private ar.edu.uade.analytics.Repository.FavouriteProductsRepository favouriteProductsRepository;
    @MockBean
    private ar.edu.uade.analytics.Repository.StockChangeLogRepository stockChangeLogRepository;

    @Test
    void contextLoads() {
        // Test de carga de contexto
    }

    @Test
    void testGetProductById_found() throws Exception {
        // Solo mockear lo necesario y usar un ID que no exista en la base real
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(1001); // Usar un ID alto para evitar colisiones
        product.setTitle("Producto Test");
        product.setDescription("Desc");
        product.setPrice(100f);
        product.setStock(10);
        product.setMediaSrc(java.util.List.of("img1.jpg"));
        product.setCalification(4.5f);
        product.setDiscount(0f);
        product.setPriceUnit(100f);
        product.setProductCode(123);
        product.setActive(true);
        product.setNew(false);
        product.setBestseller(false);
        product.setFeatured(false);
        product.setHero(false);
        when(productRepository.findById(1001)).thenReturn(java.util.Optional.of(product));
        mockMvc.perform(get("/products/1001"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto Test")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("img1.jpg")));
    }

    @Test
    void testGetProductById_notFound() throws Exception {
        // No mock necesario, solo probar el 404
        mockMvc.perform(get("/products/999999"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testSyncProductsFromMock_success() throws Exception {
        // Mockear solo lo necesario
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setProductCode(123);
        dto.setTitle("Producto Mock");
        dto.setMediaSrc(java.util.List.of("img1.jpg"));
        dto.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(1L, "Marca", true));
        dto.setCategories(java.util.List.of(new ar.edu.uade.analytics.DTO.CategoryDTO(1L, "Cat", true)));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload(java.util.List.of(dto));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage("type", payload, "timestamp");
        when(kafkaMockService.getProductsMock()).thenReturn(msg);
        // Mock repositorios solo para los IDs usados
        ar.edu.uade.analytics.Entity.Brand brand = new ar.edu.uade.analytics.Entity.Brand();
        brand.setId(1);
        brand.setName("Marca");
        brand.setActive(true);
        when(brandRepository.findById(1)).thenReturn(java.util.Optional.of(brand));
        ar.edu.uade.analytics.Entity.Category category = new ar.edu.uade.analytics.Entity.Category();
        category.setId(1);
        category.setName("Cat");
        category.setActive(true);
        when(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category));
        when(productRepository.findAll()).thenReturn(java.util.List.of());
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        // Simular findAll después de guardar
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(2001);
        product.setTitle("Producto Mock");
        product.setMediaSrc(java.util.List.of("img1.jpg"));
        when(productRepository.findAll()).thenReturn(java.util.List.of(product));
        mockMvc.perform(get("/products/sync"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto Mock")));
    }

    @Test
    void testSyncProductsFromMock_brandNotFound() throws Exception {
        // Producto con marca inexistente
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setProductCode(999);
        dto.setTitle("Producto sin marca");
        dto.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(999L, "MarcaInexistente", true));
        dto.setCategories(java.util.List.of(new ar.edu.uade.analytics.DTO.CategoryDTO(1L, "Cat", true)));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload(java.util.List.of(dto));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage("type", payload, "timestamp");
        when(kafkaMockService.getProductsMock()).thenReturn(msg);
        // Marca no existe
        when(brandRepository.findById(999)).thenReturn(java.util.Optional.empty());
        // Categoría sí existe
        ar.edu.uade.analytics.Entity.Category category = new ar.edu.uade.analytics.Entity.Category();
        category.setId(1);
        category.setName("Cat");
        category.setActive(true);
        when(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category));
        when(productRepository.findAll()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/products/sync"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSyncProductsFromMock_categoryNotFound() throws Exception {
        // Producto con categoría inexistente
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setProductCode(888);
        dto.setTitle("Producto sin categoria");
        dto.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(1L, "Marca", true));
        dto.setCategories(java.util.List.of(new ar.edu.uade.analytics.DTO.CategoryDTO(999L, "CatInexistente", true)));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload(java.util.List.of(dto));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage("type", payload, "timestamp");
        when(kafkaMockService.getProductsMock()).thenReturn(msg);
        // Marca sí existe
        ar.edu.uade.analytics.Entity.Brand brand = new ar.edu.uade.analytics.Entity.Brand();
        brand.setId(1);
        brand.setName("Marca");
        brand.setActive(true);
        when(brandRepository.findById(1)).thenReturn(java.util.Optional.of(brand));
        // Categoría no existe
        when(categoryRepository.findById(999)).thenReturn(java.util.Optional.empty());
        when(productRepository.findAll()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/products/sync"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testEditProduct_success() throws Exception {
        // Mock del mensaje de Kafka para edición completa con los datos esperados
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setId(4001L);
        dto.setTitle("Producto Editado");
        dto.setDescription("Desc Editado");
        dto.setPrice(150f);
        dto.setStock(20);
        dto.setMediaSrc(java.util.List.of("imgEdit.jpg"));
        dto.setIsNew(true);
        dto.setIsBestseller(true);
        dto.setIsFeatured(true);
        dto.setHero(true);
        dto.setActive(true);
        dto.setDiscount(10f);
        dto.setPriceUnit(166.67f);
        dto.setProductCode(123);
        dto.setCalification(4.8f);
        dto.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(1L, "Marca", true));
        dto.setCategories(java.util.List.of(new ar.edu.uade.analytics.DTO.CategoryDTO(1L, "Cat", true)));
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage(
                    "type",
                    new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload(
                        dto.getId() != null ? dto.getId().intValue() : null,
                        dto.getTitle(),
                        dto.getDescription(),
                        dto.getPrice(),
                        dto.getStock(),
                        dto.getMediaSrc(),
                        dto.getBrand(),
                        dto.getCategories(),
                        dto.getIsNew(),
                        dto.getIsBestseller(),
                        dto.getIsFeatured(),
                        dto.getHero(),
                        dto.getActive(),
                        dto.getDiscount(),
                        dto.getPriceUnit(),
                        dto.getCalification(),
                        dto.getProductCode()
                    ),
                    "timestamp"
                );
        when(kafkaMockService.getEditProductMockFull()).thenReturn(msg);
        // Mock producto existente
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(4001);
        product.setTitle("Producto Viejo");
        product.setDescription("Desc Viejo");
        product.setPrice(100f);
        product.setStock(10);
        product.setMediaSrc(java.util.List.of("imgViejo.jpg"));
        product.setNew(false);
        product.setBestseller(false);
        product.setFeatured(false);
        product.setHero(false);
        product.setActive(false);
        product.setDiscount(0f);
        product.setPriceUnit(100f);
        product.setProductCode(123);
        product.setCalification(3.0f);
        // Marca y categoría
        ar.edu.uade.analytics.Entity.Brand brand = new ar.edu.uade.analytics.Entity.Brand();
        brand.setId(1);
        brand.setName("Marca");
        brand.setActive(true);
        product.setBrand(brand);
        ar.edu.uade.analytics.Entity.Category category = new ar.edu.uade.analytics.Entity.Category();
        category.setId(1);
        category.setName("Cat");
        category.setActive(true);
        java.util.Set<ar.edu.uade.analytics.Entity.Category> catSet = new java.util.HashSet<>();
        catSet.add(category);
        product.setCategories(catSet);
        when(productRepository.findById(4001)).thenReturn(java.util.Optional.of(product));
        when(brandRepository.findById(1)).thenReturn(java.util.Optional.of(brand));
        when(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto Editado")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("imgEdit.jpg")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Marca")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cat")));
    }

    @Test
    void testEditProduct_notFound() throws Exception {
        // Simula mensaje de edición con un ID inexistente
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setId(99999L);
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage(
                        "type",
                        new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload(
                                99999,
                                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
                        ),
                        "timestamp"
                );
        when(kafkaMockService.getEditProductMockFull()).thenReturn(msg);
        when(productRepository.findById(99999)).thenReturn(java.util.Optional.empty());
        mockMvc.perform(patch("/products"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAddProduct_success() throws Exception {
        // Mock del mensaje de Kafka con los datos esperados
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setTitle("Nuevo Producto");
        dto.setDescription("Desc Nuevo");
        dto.setPrice(200f);
        dto.setStock(5);
        dto.setMediaSrc(java.util.List.of("img2.jpg"));
        dto.setIsNew(true);
        dto.setIsBestseller(false);
        dto.setIsFeatured(false);
        dto.setHero(false);
        dto.setActive(true);
        dto.setDiscount(0f);
        dto.setPriceUnit(200f);
        dto.setProductCode(456);
        dto.setCalification(0f);
        dto.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(2L, "Marca2", true));
        dto.setCategories(java.util.List.of(new ar.edu.uade.analytics.DTO.CategoryDTO(2L, "Cat2", true)));
        ar.edu.uade.analytics.Communication.KafkaMockService.AddProductPayload payload = new ar.edu.uade.analytics.Communication.KafkaMockService.AddProductPayload(dto);
        ar.edu.uade.analytics.Communication.KafkaMockService.AddProductMessage msg = new ar.edu.uade.analytics.Communication.KafkaMockService.AddProductMessage("type", payload, "timestamp");
        when(kafkaMockService.getAddProductMock()).thenReturn(msg);
        // Mock repositorios solo para los IDs usados
        ar.edu.uade.analytics.Entity.Brand brand = new ar.edu.uade.analytics.Entity.Brand();
        brand.setId(2);
        brand.setName("Marca2");
        brand.setActive(true);
        when(brandRepository.findById(2)).thenReturn(java.util.Optional.of(brand));
        ar.edu.uade.analytics.Entity.Category category = new ar.edu.uade.analytics.Entity.Category();
        category.setId(2);
        category.setName("Cat2");
        category.setActive(true);
        when(categoryRepository.findById(2)).thenReturn(java.util.Optional.of(category));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> {
            ar.edu.uade.analytics.Entity.Product p = i.getArgument(0);
            p.setId(3001); // Simular ID generado
            return p;
        });
        mockMvc.perform(post("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nuevo Producto")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("img2.jpg")));
    }

    @Test
    void testAddProduct_brandNotFound() throws Exception {
        // Simula que la marca no existe
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setTitle("Producto sin marca");
        dto.setDescription("Desc");
        dto.setPrice(100f);
        dto.setStock(10);
        dto.setMediaSrc(java.util.List.of("img.jpg"));
        dto.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(999L, "MarcaInexistente", true));
        dto.setCategories(java.util.List.of(new ar.edu.uade.analytics.DTO.CategoryDTO(1L, "Cat", true)));
        ar.edu.uade.analytics.Communication.KafkaMockService.AddProductPayload payload = new ar.edu.uade.analytics.Communication.KafkaMockService.AddProductPayload(dto);
        ar.edu.uade.analytics.Communication.KafkaMockService.AddProductMessage msg = new ar.edu.uade.analytics.Communication.KafkaMockService.AddProductMessage("type", payload, "timestamp");
        when(kafkaMockService.getAddProductMock()).thenReturn(msg);
        when(brandRepository.findById(999)).thenReturn(java.util.Optional.empty());
        ar.edu.uade.analytics.Entity.Category category = new ar.edu.uade.analytics.Entity.Category();
        category.setId(1);
        category.setName("Cat");
        category.setActive(true);
        when(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(post("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto sin marca")));
    }

    @Test
    void testAddProduct_categoryNotFound() throws Exception {
        // Simula que la categoría no existe
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setTitle("Producto sin categoria");
        dto.setDescription("Desc");
        dto.setPrice(100f);
        dto.setStock(10);
        dto.setMediaSrc(java.util.List.of("img.jpg"));
        dto.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(1L, "Marca", true));
        dto.setCategories(java.util.List.of(new ar.edu.uade.analytics.DTO.CategoryDTO(999L, "CatInexistente", true)));
        ar.edu.uade.analytics.Communication.KafkaMockService.AddProductPayload payload = new ar.edu.uade.analytics.Communication.KafkaMockService.AddProductPayload(dto);
        ar.edu.uade.analytics.Communication.KafkaMockService.AddProductMessage msg = new ar.edu.uade.analytics.Communication.KafkaMockService.AddProductMessage("type", payload, "timestamp");
        when(kafkaMockService.getAddProductMock()).thenReturn(msg);
        ar.edu.uade.analytics.Entity.Brand brand = new ar.edu.uade.analytics.Entity.Brand();
        brand.setId(1);
        brand.setName("Marca");
        brand.setActive(true);
        when(brandRepository.findById(1)).thenReturn(java.util.Optional.of(brand));
        when(categoryRepository.findById(999)).thenReturn(java.util.Optional.empty());
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(post("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto sin categoria")));
    }

    @Test
    void testAddProduct_mediaSrcNull_categoriesNull() throws Exception {
        // Simula mediaSrc null y categories null
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setTitle("Producto sin media ni categorias");
        dto.setDescription("Desc");
        dto.setPrice(100f);
        dto.setStock(10);
        dto.setMediaSrc(null);
        dto.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(1L, "Marca", true));
        dto.setCategories(null);
        ar.edu.uade.analytics.Communication.KafkaMockService.AddProductPayload payload = new ar.edu.uade.analytics.Communication.KafkaMockService.AddProductPayload(dto);
        ar.edu.uade.analytics.Communication.KafkaMockService.AddProductMessage msg = new ar.edu.uade.analytics.Communication.KafkaMockService.AddProductMessage("type", payload, "timestamp");
        when(kafkaMockService.getAddProductMock()).thenReturn(msg);
        ar.edu.uade.analytics.Entity.Brand brand = new ar.edu.uade.analytics.Entity.Brand();
        brand.setId(1);
        brand.setName("Marca");
        brand.setActive(true);
        when(brandRepository.findById(1)).thenReturn(java.util.Optional.of(brand));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(post("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto sin media ni categorias")));
    }

    @Test
    void testGetAllProducts_empty() throws Exception {
        when(productRepository.findAll()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[]")));
    }

    @Test
    void testGetAllProducts_withProducts() throws Exception {
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(1);
        product.setTitle("Producto1");
        product.setDescription("Desc1");
        product.setPrice(10f);
        product.setStock(5);
        product.setMediaSrc(java.util.List.of("img1.jpg"));
        product.setCalification(4.5f);
        product.setDiscount(0f);
        product.setPriceUnit(10f);
        product.setProductCode(123);
        product.setActive(true);
        product.setNew(false);
        product.setBestseller(false);
        product.setFeatured(false);
        product.setHero(false);
        when(productRepository.findAll()).thenReturn(java.util.List.of(product));
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto1")));
    }

    @Test
    void testGetAllProducts_withBrandAndCategories() throws Exception {
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(7001);
        product.setTitle("ProductoFull");
        ar.edu.uade.analytics.Entity.Brand brand = new ar.edu.uade.analytics.Entity.Brand();
        brand.setId(10);
        brand.setName("MarcaFull");
        brand.setActive(true);
        product.setBrand(brand);
        ar.edu.uade.analytics.Entity.Category cat = new ar.edu.uade.analytics.Entity.Category();
        cat.setId(20);
        cat.setName("CatFull");
        cat.setActive(true);
        java.util.Set<ar.edu.uade.analytics.Entity.Category> cats = new java.util.HashSet<>();
        cats.add(cat);
        product.setCategories(cats);
        when(productRepository.findAll()).thenReturn(java.util.List.of(product));
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ProductoFull")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MarcaFull")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CatFull")));
    }

    @Test
    void testEditProduct_withCategoriesContainingNull() throws Exception {
        // Preparar payload con categorías: [null, Cat7]
        java.util.List<ar.edu.uade.analytics.DTO.CategoryDTO> cats = new java.util.ArrayList<>();
        cats.add(null);
        cats.add(new ar.edu.uade.analytics.DTO.CategoryDTO(7L, "Cat7", true));
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload(
                        6000,
                        null, null, null, null, null, null, cats,
                        null, null, null, null, null, null, null, null, null
                );
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage("type", payload, "timestamp");
        when(kafkaMockService.getEditProductMockFull()).thenReturn(msg);
        // Producto existente
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(6000);
        product.setTitle("Prod6000");
        when(productRepository.findById(6000)).thenReturn(java.util.Optional.of(product));
        // Categoria existente
        ar.edu.uade.analytics.Entity.Category cat = new ar.edu.uade.analytics.Entity.Category();
        cat.setId(7);
        cat.setName("Cat7");
        cat.setActive(true);
        when(categoryRepository.findById(7)).thenReturn(java.util.Optional.of(cat));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cat7")));
    }

    @Test
    void testSyncMockStockChanges_mixedEvents_processesOnlyValid() throws Exception {
        // Producto válido
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(8001);
        product.setProductCode(4444);
        product.setStock(5);
        when(productRepository.findByProductCode(4444)).thenReturn(product);
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        when(stockChangeLogRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        // Eventos: uno válido y uno con payload no map
        String eventosJson = "[ { \"type\": \"MANUAL\", \"timestamp\": \"2025-09-07T12:00:00\", \"payload\": {\"productCode\": 4444, \"stock\": 10} }, { \"type\": \"MANUAL\", \"timestamp\": \"2025-09-07T12:00:00\", \"payload\": 123 } ]";
        mockMvc.perform(post("/products/sync-mock-stock-changes")
                .contentType("application/json")
                .content(eventosJson))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("procesados correctamente: 1")));
    }

    @Test
    void testSyncMockFavouriteProduct_success() throws Exception {
        // Mock del mensaje de Kafka para producto favorito
        ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductPayload(111, 9999, "Producto Favorito");
        ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductMessage(
                        "ADD_FAVOURITE",
                        payload,
                        java.time.LocalDateTime.now().toString()
                );
        when(kafkaMockService.getAddFavouriteProductMock()).thenReturn(msg);
        // Mock producto existente
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(111);
        product.setProductCode(9999);
        product.setTitle("Producto Favorito");
        when(productRepository.findByProductCode(9999)).thenReturn(product);
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        when(favouriteProductsRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(post("/products/sync-mock-favourite"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("favorito sincronizado correctamente")));
    }

    @Test
    void testSyncMockFavouriteProducts_success() throws Exception {
        // Mock de lista de mensajes de Kafka para productos favoritos
        ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductPayload payload1 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductPayload(112, 8888, "Producto Fav 1");
        ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductMessage msg1 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductMessage(
                        "ADD_FAVOURITE",
                        payload1,
                        java.time.LocalDateTime.now().toString()
                );
        ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductPayload payload2 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductPayload(113, 7777, "Producto Fav 2");
        ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductMessage msg2 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductMessage(
                        "ADD_FAVOURITE",
                        payload2,
                        java.time.LocalDateTime.now().toString()
                );
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductMessage> events = java.util.List.of(msg1, msg2);
        when(kafkaMockService.getAddFavouriteProductsMock()).thenReturn(events);
        // Mock productos existentes
        ar.edu.uade.analytics.Entity.Product product1 = new ar.edu.uade.analytics.Entity.Product();
        product1.setId(112);
        product1.setProductCode(8888);
        product1.setTitle("Producto Fav 1");
        ar.edu.uade.analytics.Entity.Product product2 = new ar.edu.uade.analytics.Entity.Product();
        product2.setId(113);
        product2.setProductCode(7777);
        product2.setTitle("Producto Fav 2");
        when(productRepository.findByProductCode(8888)).thenReturn(product1);
        when(productRepository.findByProductCode(7777)).thenReturn(product2);
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        when(favouriteProductsRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(post("/products/sync-mock-favourites"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("favoritos sincronizados correctamente")));
    }

    @Test
    void testActivateProduct_success() throws Exception {
        // Mock del mensaje de Kafka para activar producto
        ar.edu.uade.analytics.Communication.KafkaMockService.ActivateProductPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ActivateProductPayload(1234L);
        ar.edu.uade.analytics.Communication.KafkaMockService.ActivateProductMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ActivateProductMessage(
                        "ACTIVATE_PRODUCT",
                        payload,
                        java.time.LocalDateTime.now().toString()
                );
        when(kafkaMockService.getActivateProductMock()).thenReturn(msg);
        // Mock producto existente
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(1234);
        product.setActive(false);
        when(productRepository.findById(1234)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products/activate"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("true")));
    }

    @Test
    void testDeactivateProduct_success() throws Exception {
        // Mock del mensaje de Kafka para desactivar producto
        ar.edu.uade.analytics.Communication.KafkaMockService.DeactivateProductPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.DeactivateProductPayload(1234L);
        ar.edu.uade.analytics.Communication.KafkaMockService.DeactivateProductMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.DeactivateProductMessage(
                        "DEACTIVATE_PRODUCT",
                        payload,
                        java.time.LocalDateTime.now().toString()
                );
        when(kafkaMockService.getDeactivateProductMock()).thenReturn(msg);
        // Mock producto existente
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(1234);
        product.setActive(true);
        when(productRepository.findById(1234)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products/deactivate"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("false")));
    }

    @Test
    void testSyncProductsFromMock_productCodeNull() throws Exception {
        // Producto con productCode null debe ser ignorado
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setProductCode(null);
        dto.setTitle("Producto sin código");
        dto.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(1L, "Marca", true));
        dto.setCategories(java.util.List.of(new ar.edu.uade.analytics.DTO.CategoryDTO(1L, "Cat", true)));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload(java.util.List.of(dto));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage("type", payload, "timestamp");
        when(kafkaMockService.getProductsMock()).thenReturn(msg);
        // Marca y categoría sí existen
        ar.edu.uade.analytics.Entity.Brand brand = new ar.edu.uade.analytics.Entity.Brand();
        brand.setId(1);
        brand.setName("Marca");
        brand.setActive(true);
        when(brandRepository.findById(1)).thenReturn(java.util.Optional.of(brand));
        ar.edu.uade.analytics.Entity.Category category = new ar.edu.uade.analytics.Entity.Category();
        category.setId(1);
        category.setName("Cat");
        category.setActive(true);
        when(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category));
        when(productRepository.findAll()).thenReturn(java.util.List.of());
        // No debe lanzar excepción y la lista de productos debe seguir vacía
        mockMvc.perform(get("/products/sync"))
                .andExpect(status().isOk());
    }

    @Test
    void testEditProduct_allFieldsNull() throws Exception {
        // Simula mensaje de edición con todos los campos null excepto el id
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setId(5002L);
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage(
                        "type",
                        new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload(
                                5002,
                                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
                        ),
                        "timestamp"
                );
        when(kafkaMockService.getEditProductMockFull()).thenReturn(msg);
        // Producto existente con valores previos
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(5002);
        product.setTitle("Viejo");
        product.setDescription("Desc vieja");
        product.setPrice(100f);
        product.setStock(10);
        product.setMediaSrc(java.util.List.of("vieja.jpg"));
        product.setNew(true);
        product.setBestseller(true);
        product.setFeatured(true);
        product.setHero(true);
        product.setActive(true);
        product.setDiscount(5f);
        product.setPriceUnit(105f);
        product.setProductCode(555);
        product.setBrand(null);
        product.setCategories(null);
        product.setCalification(3.5f);
        when(productRepository.findById(5002)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Viejo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[]"))) // mediaSrc vacía
                .andExpect(content().string(org.hamcrest.Matchers.containsString("0.0"))); // calification a 0f
    }

    @Test
    void testEditProduct_partialFields() throws Exception {
        // Simula mensaje de edición con solo title y price seteados
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setId(5003L);
        dto.setTitle("Nuevo Título");
        dto.setPrice(777f);
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage(
                        "type",
                        new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload(
                                5003,
                                "Nuevo Título", null, 777f, null, null, null, null, null, null, null, null, null, null, null, null, null
                        ),
                        "timestamp"
                );
        when(kafkaMockService.getEditProductMockFull()).thenReturn(msg);
        // Producto existente con valores previos
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(5003);
        product.setTitle("Viejo");
        product.setDescription("Desc vieja");
        product.setPrice(100f);
        product.setStock(10);
        product.setMediaSrc(java.util.List.of("vieja.jpg"));
        product.setNew(true);
        product.setBestseller(true);
        product.setFeatured(true);
        product.setHero(true);
        product.setActive(true);
        product.setDiscount(5f);
        product.setPriceUnit(105f);
        product.setProductCode(555);
        product.setBrand(null);
        product.setCategories(null);
        product.setCalification(3.5f);
        when(productRepository.findById(5003)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nuevo Título")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("777.0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Desc vieja"))) // no cambia
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[]"))); // mediaSrc vacía
    }

    @Test
    void testEditProduct_priceUnitAndDiscount() throws Exception {
        // Simula mensaje de edición con priceUnit y discount seteados, price no
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setId(5004L);
        dto.setPriceUnit(200f);
        dto.setDiscount(10f);
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage(
                        "type",
                        new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload(
                                5004,
                                null, null, null, null, null, null, null, null, null, null, null, null,
                                null, // calification
                                10f,  // discount
                                200f, // priceUnit
                                null  // productCode
                        ),
                        "timestamp"
                );
        when(kafkaMockService.getEditProductMockFull()).thenReturn(msg);
        // Producto existente con valores previos
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(5004);
        product.setTitle("Viejo");
        product.setDescription("Desc vieja");
        product.setPrice(100f);
        product.setStock(10);
        product.setMediaSrc(java.util.List.of("vieja.jpg"));
        product.setNew(true);
        product.setBestseller(true);
        product.setFeatured(true);
        product.setHero(true);
        product.setActive(true);
        product.setDiscount(5f);
        product.setPriceUnit(105f);
        product.setProductCode(555);
        product.setBrand(null);
        product.setCategories(null);
        product.setCalification(3.5f);
        when(productRepository.findById(5004)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("180.0"))) // price recalculado
                .andExpect(content().string(org.hamcrest.Matchers.containsString("200.0"))) // priceUnit
                .andExpect(content().string(org.hamcrest.Matchers.containsString("10.0"))); // discount
    }

    @Test
    void testEditProduct_onlyPrice() throws Exception {
        // Simula mensaje de edición con solo price seteado, priceUnit y discount no
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setId(5005L);
        dto.setPrice(555f);
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage(
                        "type",
                        new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload(
                                5005,
                                null, null, 555f, null, null, null, null, null, null, null, null, null, null, null, null, null
                        ),
                        "timestamp"
                );
        when(kafkaMockService.getEditProductMockFull()).thenReturn(msg);
        // Producto existente con valores previos
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(5005);
        product.setTitle("Viejo");
        product.setDescription("Desc vieja");
        product.setPrice(100f);
        product.setStock(10);
        product.setMediaSrc(java.util.List.of("vieja.jpg"));
        product.setNew(true);
        product.setBestseller(true);
        product.setFeatured(true);
        product.setHero(true);
        product.setActive(true);
        product.setDiscount(null);
        product.setPriceUnit(null);
        product.setProductCode(555);
        product.setBrand(null);
        product.setCategories(null);
        product.setCalification(3.5f);
        when(productRepository.findById(5005)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("555.0"))) // price actualizado
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Viejo")));
    }

    @Test
    void testEditProduct_brandNull() throws Exception {
        // Simula mensaje de edición con brand null
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setId(5006L);
        dto.setBrand(null);
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage(
                        "type",
                        new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload(
                                5006,
                                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
                        ),
                        "timestamp"
                );
        when(kafkaMockService.getEditProductMockFull()).thenReturn(msg);
        // Producto existente con marca previa
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(5006);
        product.setTitle("Viejo");
        product.setBrand(new ar.edu.uade.analytics.Entity.Brand());
        product.getBrand().setId(99);
        product.getBrand().setName("MarcaVieja");
        product.getBrand().setActive(true);
        when(productRepository.findById(5006)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("MarcaVieja"))));
    }

    @Test
    void testEditProduct_categoriesNullOrEmpty() throws Exception {
        // Simula mensaje de edición con categories null
        ar.edu.uade.analytics.DTO.ProductDTO dtoNull = new ar.edu.uade.analytics.DTO.ProductDTO();
        dtoNull.setId(5007L);
        dtoNull.setCategories(null);
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage msgNull =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage(
                        "type",
                        new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload(
                                5007,
                                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
                        ),
                        "timestamp"
                );
        when(kafkaMockService.getEditProductMockFull()).thenReturn(msgNull);
        // Producto existente with categorías previas
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(5007);
        java.util.Set<ar.edu.uade.analytics.Entity.Category> catSet = new java.util.HashSet<>();
        ar.edu.uade.analytics.Entity.Category cat = new ar.edu.uade.analytics.Entity.Category();
        cat.setId(1);
        cat.setName("CatAntigua");
        cat.setActive(true);
        catSet.add(cat);
        product.setCategories(catSet);
        when(productRepository.findById(5007)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("CatAntigua"))));

        // Simula mensaje de edición con categories vacío
        ar.edu.uade.analytics.DTO.ProductDTO dtoEmpty = new ar.edu.uade.analytics.DTO.ProductDTO();
        dtoEmpty.setId(5008L);
        dtoEmpty.setCategories(java.util.List.of());
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage msgEmpty =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage(
                        "type",
                        new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload(
                                5008,
                                null, null, null, null, null, null, java.util.List.of(), null, null, null, null, null, null, null, null, null
                        ),
                        "timestamp"
                );
        when(kafkaMockService.getEditProductMockFull()).thenReturn(msgEmpty);
        ar.edu.uade.analytics.Entity.Product product2 = new ar.edu.uade.analytics.Entity.Product();
        product2.setId(5008);
        java.util.Set<ar.edu.uade.analytics.Entity.Category> catSet2 = new java.util.HashSet<>();
        ar.edu.uade.analytics.Entity.Category cat2 = new ar.edu.uade.analytics.Entity.Category();
        cat2.setId(2);
        cat2.setName("CatAntigua2");
        cat2.setActive(true);
        catSet2.add(cat2);
        product2.setCategories(catSet2);
        when(productRepository.findById(5008)).thenReturn(java.util.Optional.of(product2));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("CatAntigua2"))));
    }

    @Test
    void testSyncProductsFromMock_brandIdNull_categoryIdNull_emptyCats_emptyMediaSrc() throws Exception {
        // Brand id null
        ar.edu.uade.analytics.DTO.ProductDTO dtoBrandNull = new ar.edu.uade.analytics.DTO.ProductDTO();
        dtoBrandNull.setProductCode(10001);
        dtoBrandNull.setTitle("Producto BrandIdNull");
        dtoBrandNull.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(null, "MarcaNull", true));
        dtoBrandNull.setCategories(java.util.List.of(new ar.edu.uade.analytics.DTO.CategoryDTO(1L, "Cat", true)));
        // Category id null
        ar.edu.uade.analytics.DTO.ProductDTO dtoCatNull = new ar.edu.uade.analytics.DTO.ProductDTO();
        dtoCatNull.setProductCode(10002);
        dtoCatNull.setTitle("Producto CatIdNull");
        dtoCatNull.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(1L, "Marca", true));
        dtoCatNull.setCategories(java.util.List.of(new ar.edu.uade.analytics.DTO.CategoryDTO(null, "CatNull", true)));
        // Categorías vacías
        ar.edu.uade.analytics.DTO.ProductDTO dtoEmptyCats = new ar.edu.uade.analytics.DTO.ProductDTO();
        dtoEmptyCats.setProductCode(10003);
        dtoEmptyCats.setTitle("Producto CatsEmpty");
        dtoEmptyCats.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(1L, "Marca", true));
        dtoEmptyCats.setCategories(java.util.List.of());
        // mediaSrc vacía
        ar.edu.uade.analytics.DTO.ProductDTO dtoEmptyMedia = new ar.edu.uade.analytics.DTO.ProductDTO();
        dtoEmptyMedia.setProductCode(10004);
        dtoEmptyMedia.setTitle("Producto MediaEmpty");
        dtoEmptyMedia.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(1L, "Marca", true));
        dtoEmptyMedia.setCategories(java.util.List.of(new ar.edu.uade.analytics.DTO.CategoryDTO(1L, "Cat", true)));
        dtoEmptyMedia.setMediaSrc(java.util.List.of());
        // Producto existente para updateCatsNull
        ar.edu.uade.analytics.DTO.ProductDTO dtoUpdateCatsNull = new ar.edu.uade.analytics.DTO.ProductDTO();
        dtoUpdateCatsNull.setProductCode(10005);
        dtoUpdateCatsNull.setTitle("Producto UpdateCatsNull");
        dtoUpdateCatsNull.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(1L, "Marca", true));
        dtoUpdateCatsNull.setCategories(null);
        // Mock message
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload(java.util.List.of(dtoBrandNull, dtoCatNull, dtoEmptyCats, dtoEmptyMedia, dtoUpdateCatsNull));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage("type", payload, "timestamp");
        when(kafkaMockService.getProductsMock()).thenReturn(msg);
        // Marca y categoría existen
        ar.edu.uade.analytics.Entity.Brand brand = new ar.edu.uade.analytics.Entity.Brand();
        brand.setId(1);
        brand.setName("Marca");
        brand.setActive(true);
        when(brandRepository.findById(1)).thenReturn(java.util.Optional.of(brand));
        ar.edu.uade.analytics.Entity.Category category = new ar.edu.uade.analytics.Entity.Category();
        category.setId(1);
        category.setName("Cat");
        category.setActive(true);
        when(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category));
        // Producto existente para updateCatsNull
        ar.edu.uade.analytics.Entity.Product existing = new ar.edu.uade.analytics.Entity.Product();
        existing.setId(10005);
        existing.setProductCode(10005);
        existing.setTitle("Viejo");
        java.util.Set<ar.edu.uade.analytics.Entity.Category> catSet = new java.util.HashSet<>();
        catSet.add(category);
        existing.setCategories(catSet);
        // Simular base de datos en memoria
        java.util.List<ar.edu.uade.analytics.Entity.Product> savedProducts = new java.util.ArrayList<>();
        savedProducts.add(existing); // producto existente
        when(productRepository.findAll()).thenAnswer(inv -> savedProducts);
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> {
            ar.edu.uade.analytics.Entity.Product p = i.getArgument(0);
            // Si no está, agregarlo
            if (savedProducts.stream().noneMatch(prod -> prod.getProductCode() != null && prod.getProductCode().equals(p.getProductCode()))) {
                savedProducts.add(p);
            } else {
                // Si ya está, actualizarlo
                for (int j = 0; j < savedProducts.size(); j++) {
                    if (savedProducts.get(j).getProductCode() != null && savedProducts.get(j).getProductCode().equals(p.getProductCode())) {
                        savedProducts.set(j, p);
                    }
                }
            }
            return p;
        });
        // No debe lanzar excepción, y los productos deben estar en la respuesta
        mockMvc.perform(get("/products/sync"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto BrandIdNull")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto CatIdNull")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto CatsEmpty")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto MediaEmpty")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto UpdateCatsNull")));
    }

    @Test
    void testEditProductSimple_onlyStock_onlyPrice_discountNull() throws Exception {
        // Sólo stock
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimplePayload payloadStock =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimplePayload(2001, 99, null);
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimpleMessage msgStock =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimpleMessage(
                        "STOCK_UPDATE_SIMPLE", payloadStock, java.time.LocalDateTime.now().toString());
        ar.edu.uade.analytics.Entity.Product productStock = new ar.edu.uade.analytics.Entity.Product();
        productStock.setId(201);
        productStock.setProductCode(2001);
        productStock.setStock(10);
        productStock.setPrice(100f);
        productStock.setDiscount(10f);
        when(kafkaMockService.getEditProductMockSimple()).thenReturn(msgStock);
        when(productRepository.findByProductCode(2001)).thenReturn(productStock);
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products/simple"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("99")));
        // Sólo price, discount null
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimplePayload payloadPrice =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimplePayload(2002, null, 555f);
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimpleMessage msgPrice =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimpleMessage(
                        "STOCK_UPDATE_SIMPLE", payloadPrice, java.time.LocalDateTime.now().toString());
        ar.edu.uade.analytics.Entity.Product productPrice = new ar.edu.uade.analytics.Entity.Product();
        productPrice.setId(202);
        productPrice.setProductCode(2002);
        productPrice.setStock(10);
        productPrice.setPrice(100f);
        productPrice.setDiscount(null); // discount null
        when(kafkaMockService.getEditProductMockSimple()).thenReturn(msgPrice);
        when(productRepository.findByProductCode(2002)).thenReturn(productPrice);
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products/simple"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("555.0")));
    }

    @Test
    void testSyncMockStockChanges_success_and_errors() throws Exception {
        // Simular base de datos en memoria
        java.util.List<ar.edu.uade.analytics.Entity.Product> savedProducts = new java.util.ArrayList<>();
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(1);
        product.setProductCode(111);
        product.setStock(10);
        savedProducts.add(product);
        when(productRepository.findByProductCode(111)).thenReturn(product);
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        // Mock StockChangeLog
        when(stockChangeLogRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        // Evento válido
        String eventosJson = "[ { \"type\": \"MANUAL\", \"timestamp\": \"2025-09-07T12:00:00\", \"payload\": {\"productCode\": 111, \"stock\": 20} } ]";
        mockMvc.perform(post("/products/sync-mock-stock-changes")
                .contentType("application/json")
                .content(eventosJson))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("procesados correctamente: 1")));
        // Evento con producto inexistente
        String eventosJson2 = "[ { \"type\": \"MANUAL\", \"timestamp\": \"2025-09-07T12:00:00\", \"payload\": {\"productCode\": 999, \"stock\": 20} } ]";
        when(productRepository.findByProductCode(999)).thenReturn(null);
        mockMvc.perform(post("/products/sync-mock-stock-changes")
                .contentType("application/json")
                .content(eventosJson2))
                .andExpect(status().isBadRequest());
        // Lista vacía
        mockMvc.perform(post("/products/sync-mock-stock-changes")
                .contentType("application/json")
                .content("[]"))
                .andExpect(status().isBadRequest());
        // Lista nula
        mockMvc.perform(post("/products/sync-mock-stock-changes"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSyncStockChanges_success_and_errors() throws Exception {
        // Simular base de datos en memoria
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(2);
        product.setProductCode(222);
        product.setStock(5);
        when(productRepository.findByProductCode(222)).thenReturn(product);
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        when(stockChangeLogRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        // Evento válido
        String eventosJson = "[ { \"type\": \"API\", \"timestamp\": \"2025-09-07T12:00:00\", \"payload\": {\"productCode\": 222, \"stock\": 15} } ]";
        mockMvc.perform(post("/products/sync-stock-changes")
                .contentType("application/json")
                .content(eventosJson))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("procesados correctamente: 1")));
        // Evento con producto inexistente
        String eventosJson2 = "[ { \"type\": \"API\", \"timestamp\": \"2025-09-07T12:00:00\", \"payload\": {\"productCode\": 999, \"stock\": 15} } ]";
        when(productRepository.findByProductCode(999)).thenReturn(null);
        mockMvc.perform(post("/products/sync-stock-changes")
                .contentType("application/json")
                .content(eventosJson2))
                .andExpect(status().isBadRequest());
        // Lista vacía
        mockMvc.perform(post("/products/sync-stock-changes")
                .contentType("application/json")
                .content("[]"))
                .andExpect(status().isBadRequest());
        // Lista nula
        mockMvc.perform(post("/products/sync-stock-changes"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSyncMockStockChangesSimple_success_and_errors() throws Exception {
        // Simular base de datos en memoria
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(3);
        product.setProductCode(333);
        product.setStock(7);
        product.setPrice(100f);
        when(productRepository.findByProductCode(333)).thenReturn(product);
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        when(stockChangeLogRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        // Evento válido
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimplePayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimplePayload(333, 17, 200f);
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimpleMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimpleMessage(
                        "STOCK_UPDATE_SIMPLE", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getEditProductMockSimpleList()).thenReturn(java.util.List.of(msg));
        mockMvc.perform(post("/products/sync-mock-stock-changes-simple"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("procesados correctamente: 1")));
        // Evento con producto inexistente
        when(productRepository.findByProductCode(999)).thenReturn(null);
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimplePayload payload2 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimplePayload(999, 17, 200f);
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimpleMessage msg2 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimpleMessage(
                        "STOCK_UPDATE_SIMPLE", payload2, "2025-09-07T12:00:00");
        when(kafkaMockService.getEditProductMockSimpleList()).thenReturn(java.util.List.of(msg2));
        mockMvc.perform(post("/products/sync-mock-stock-changes-simple"))
                .andExpect(status().isBadRequest());
        // Lista vacía
        when(kafkaMockService.getEditProductMockSimpleList()).thenReturn(java.util.List.of());
        mockMvc.perform(post("/products/sync-mock-stock-changes-simple"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddReviewFromEvent_success_and_error() throws Exception {
        // Producto existente
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(4);
        product.setTitle("Producto Review");
        when(productRepository.findById(4)).thenReturn(java.util.Optional.of(product));
        // Mock reviewRepository
        when(reviewRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.findByProduct(product)).thenReturn(java.util.List.of());
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        // Mock evento
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockPayload(4, 5.0f, "Excelente");
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockMessage("REVIEW", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getProductReviewMock()).thenReturn(msg);
        mockMvc.perform(post("/products/review/simulate"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto Review")));
        // Producto no existe
        when(productRepository.findById(999)).thenReturn(java.util.Optional.empty());
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockPayload payload2 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockPayload(999, 4.0f, "No existe");
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockMessage msg2 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockMessage("REVIEW", payload2, "2025-09-07T12:00:00");
        when(kafkaMockService.getProductReviewMock()).thenReturn(msg2);
        mockMvc.perform(post("/products/review/simulate"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSyncReviewFromMock_success_and_error() throws Exception {
        // Producto existente
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(5);
        product.setTitle("Producto Review Sync");
        when(productRepository.findById(5)).thenReturn(java.util.Optional.of(product));
        when(reviewRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.findByProduct(product)).thenReturn(java.util.List.of());
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockPayload(5, 3.0f, "Bueno");
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockMessage("REVIEW", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getProductReviewMock()).thenReturn(msg);
        mockMvc.perform(post("/products/review/sync-mock"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto Review Sync")));
        // Producto no existe
        when(productRepository.findById(999)).thenReturn(java.util.Optional.empty());
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockPayload payload2 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockPayload(999, 2.0f, "No existe");
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockMessage msg2 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockMessage("REVIEW", payload2, "2025-09-07T12:00:00");
        when(kafkaMockService.getProductReviewMock()).thenReturn(msg2);
        mockMvc.perform(post("/products/review/sync-mock"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSyncReviewListFromMock_success_and_error() throws Exception {
        // Producto existente
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(6);
        product.setTitle("Producto Review List");
        when(productRepository.findById(6)).thenReturn(java.util.Optional.of(product));
        when(reviewRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.findByProduct(product)).thenReturn(java.util.List.of());
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockPayload(6, 4.0f, "Muy bueno");
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockMessage("REVIEW", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getProductReviewMockList()).thenReturn(java.util.List.of(msg));
        mockMvc.perform(post("/products/review/sync-mock-list"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto Review List")));
        // Producto no existe
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockPayload payload2 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockPayload(999, 1.0f, "No existe");
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockMessage msg2 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductReviewMockMessage("REVIEW", payload2, "2025-09-07T12:00:00");
        when(productRepository.findById(999)).thenReturn(java.util.Optional.empty());
        when(kafkaMockService.getProductReviewMockList()).thenReturn(java.util.List.of(msg2));
        mockMvc.perform(post("/products/review/sync-mock-list"))
                .andExpect(status().isOk()); // No error, simplemente ignora el producto inexistente
        // Lista vacía
        when(kafkaMockService.getProductReviewMockList()).thenReturn(java.util.List.of());
        mockMvc.perform(post("/products/review/sync-mock-list"))
                .andExpect(status().isOk());
    }

    @Test
    void testSyncProductsFromMock_categoryIdNull() throws Exception {
        // Producto con categoría id null
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setProductCode(12345);
        dto.setTitle("Producto CatIdNull");
        dto.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(1L, "Marca", true));
        dto.setCategories(java.util.List.of(new ar.edu.uade.analytics.DTO.CategoryDTO(null, "CatNull", true)));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload(java.util.List.of(dto));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage("type", payload, "timestamp");
        when(kafkaMockService.getProductsMock()).thenReturn(msg);
        ar.edu.uade.analytics.Entity.Brand brand = new ar.edu.uade.analytics.Entity.Brand();
        brand.setId(1);
        brand.setName("Marca");
        brand.setActive(true);
        when(brandRepository.findById(1)).thenReturn(java.util.Optional.of(brand));
        // Simular almacenamiento en memoria
        java.util.List<ar.edu.uade.analytics.Entity.Product> savedProducts = new java.util.ArrayList<>();
        when(productRepository.findAll()).thenAnswer(inv -> savedProducts);
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> {
            ar.edu.uade.analytics.Entity.Product p = i.getArgument(0);
            savedProducts.add(p);
            return p;
        });
        mockMvc.perform(get("/products/sync"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto CatIdNull")));
    }

    @Test
    void testActivateDeactivateProduct_notFound() throws Exception {
        // activateProduct
        ar.edu.uade.analytics.Communication.KafkaMockService.ActivateProductPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ActivateProductPayload(99999L);
        ar.edu.uade.analytics.Communication.KafkaMockService.ActivateProductMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ActivateProductMessage(
                        "ACTIVATE_PRODUCT", payload, java.time.LocalDateTime.now().toString());
        when(kafkaMockService.getActivateProductMock()).thenReturn(msg);
        when(productRepository.findById(99999)).thenReturn(java.util.Optional.empty());
        mockMvc.perform(patch("/products/activate"))
                .andExpect(status().is4xxClientError());
        // deactivateProduct
        ar.edu.uade.analytics.Communication.KafkaMockService.DeactivateProductPayload payload2 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.DeactivateProductPayload(88888L);
        ar.edu.uade.analytics.Communication.KafkaMockService.DeactivateProductMessage msg2 =
                new ar.edu.uade.analytics.Communication.KafkaMockService.DeactivateProductMessage(
                        "DEACTIVATE_PRODUCT", payload2, java.time.LocalDateTime.now().toString());
        when(kafkaMockService.getDeactivateProductMock()).thenReturn(msg2);
        when(productRepository.findById(88888)).thenReturn(java.util.Optional.empty());
        mockMvc.perform(patch("/products/deactivate"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testSyncProductsFromMock_categoryListWithNullElement() throws Exception {
        // Producto con una categoría nula en la lista
        ar.edu.uade.analytics.DTO.ProductDTO dto = new ar.edu.uade.analytics.DTO.ProductDTO();
        dto.setProductCode(1234567);
        dto.setTitle("Producto CatNullElement");
        dto.setBrand(new ar.edu.uade.analytics.DTO.BrandDTO(1L, "Marca", true));
        java.util.List<ar.edu.uade.analytics.DTO.CategoryDTO> categories = new java.util.ArrayList<>();
        categories.add(null); // categoría nula
        categories.add(new ar.edu.uade.analytics.DTO.CategoryDTO(1L, "Cat", true));
        dto.setCategories(categories);
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncPayload(java.util.List.of(dto));
        ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.ProductSyncMessage("type", payload, "timestamp");
        when(kafkaMockService.getProductsMock()).thenReturn(msg);
        ar.edu.uade.analytics.Entity.Brand brand = new ar.edu.uade.analytics.Entity.Brand();
        brand.setId(1);
        brand.setName("Marca");
        brand.setActive(true);
        when(brandRepository.findById(1)).thenReturn(java.util.Optional.of(brand));
        ar.edu.uade.analytics.Entity.Category category = new ar.edu.uade.analytics.Entity.Category();
        category.setId(1);
        category.setName("Cat");
        category.setActive(true);
        when(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category));
        // Simular almacenamiento en memoria
        java.util.List<ar.edu.uade.analytics.Entity.Product> savedProducts = new java.util.ArrayList<>();
        when(productRepository.findAll()).thenAnswer(inv -> savedProducts);
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> {
            ar.edu.uade.analytics.Entity.Product p = i.getArgument(0);
            savedProducts.add(p);
            return p;
        });
        mockMvc.perform(get("/products/sync"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto CatNullElement")));
    }

    @Test
    void testSyncMockStockChanges_nullEventInList() throws Exception {
        // Lista con un evento nulo en el mock
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.EditProductSimpleMessage> eventos = new java.util.ArrayList<>();
        eventos.add(null);
        when(kafkaMockService.getEditProductMockSimpleList()).thenReturn(eventos);
        mockMvc.perform(post("/products/sync-mock-stock-changes-simple"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSyncMockFavouriteProducts_nullEventInList() throws Exception {
        // Lista con un evento nulo
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.AddFavouriteProductMessage> eventos = new java.util.ArrayList<>();
        eventos.add(null);
        when(kafkaMockService.getAddFavouriteProductsMock()).thenReturn(eventos);
        mockMvc.perform(post("/products/sync-mock-favourites"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Productos favoritos sincronizados correctamente")));
    }

    @Test
    void testProductDTO_fromEntity_mapsBrandAndCategories() {
        // Crear entidad Product con brand y varias categorias
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(9001);
        product.setTitle("Producto DTO Test");
        ar.edu.uade.analytics.Entity.Brand brand = new ar.edu.uade.analytics.Entity.Brand();
        brand.setId(55);
        brand.setName("MarcaDTO");
        brand.setActive(true);
        product.setBrand(brand);
        ar.edu.uade.analytics.Entity.Category c1 = new ar.edu.uade.analytics.Entity.Category();
        c1.setId(101);
        c1.setName("CatA");
        c1.setActive(true);
        ar.edu.uade.analytics.Entity.Category c2 = new ar.edu.uade.analytics.Entity.Category();
        c2.setId(102);
        c2.setName("CatB");
        c2.setActive(false);
        java.util.Set<ar.edu.uade.analytics.Entity.Category> cats = new java.util.HashSet<>();
        cats.add(c1);
        cats.add(c2);
        product.setCategories(cats);
        // Ejecutar el mapper estático
        ar.edu.uade.analytics.DTO.ProductDTO dto = ar.edu.uade.analytics.DTO.ProductDTO.fromEntity(product);
        // Aserciones simples para garantizar que el mapping se realizó (cubre lambdas de mapeo)
        org.junit.jupiter.api.Assertions.assertNotNull(dto);
        org.junit.jupiter.api.Assertions.assertNotNull(dto.getBrand());
        org.junit.jupiter.api.Assertions.assertEquals("MarcaDTO", dto.getBrand().getName());
        org.junit.jupiter.api.Assertions.assertNotNull(dto.getCategories());
        org.junit.jupiter.api.Assertions.assertEquals(2, dto.getCategories().size());
        java.util.List<String> names = dto.getCategories().stream().map(ar.edu.uade.analytics.DTO.CategoryDTO::getName).toList();
        org.junit.jupiter.api.Assertions.assertTrue(names.contains("CatA"));
        org.junit.jupiter.api.Assertions.assertTrue(names.contains("CatB"));
    }

    @Test
    void testEditProduct_withMultipleCategories() throws Exception {
        // Preparar payload con categorías: [Cat7, Cat8]
        java.util.List<ar.edu.uade.analytics.DTO.CategoryDTO> cats = new java.util.ArrayList<>();
        cats.add(new ar.edu.uade.analytics.DTO.CategoryDTO(7L, "Cat7", true));
        cats.add(new ar.edu.uade.analytics.DTO.CategoryDTO(8L, "Cat8", true));
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload payload =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullPayload(
                        6100,
                        null, null, null, null, null, null, cats,
                        null, null, null, null, null, null, null, null, null
                );
        ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage msg =
                new ar.edu.uade.analytics.Communication.KafkaMockService.EditProductFullMessage("type", payload, "timestamp");
        when(kafkaMockService.getEditProductMockFull()).thenReturn(msg);
        // Producto existente
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setId(6100);
        product.setTitle("Prod6100");
        when(productRepository.findById(6100)).thenReturn(java.util.Optional.of(product));
        // Categorías existentes
        ar.edu.uade.analytics.Entity.Category cat7 = new ar.edu.uade.analytics.Entity.Category();
        cat7.setId(7);
        cat7.setName("Cat7");
        cat7.setActive(true);
        ar.edu.uade.analytics.Entity.Category cat8 = new ar.edu.uade.analytics.Entity.Category();
        cat8.setId(8);
        cat8.setName("Cat8");
        cat8.setActive(true);
        when(categoryRepository.findById(7)).thenReturn(java.util.Optional.of(cat7));
        when(categoryRepository.findById(8)).thenReturn(java.util.Optional.of(cat8));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cat7")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cat8")));
    }
}
