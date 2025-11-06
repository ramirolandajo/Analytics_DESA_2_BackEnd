package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Communication.KafkaMockService;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.Review;
import ar.edu.uade.analytics.Repository.ProductRepository;
import ar.edu.uade.analytics.Repository.BrandRepository;
import ar.edu.uade.analytics.Repository.CategoryRepository;
import ar.edu.uade.analytics.Repository.ReviewRepository;
import ar.edu.uade.analytics.Repository.FavouriteProductsRepository;
import ar.edu.uade.analytics.Repository.StockChangeLogRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductControllerUnitTest {

    static void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void addProduct_usesKafkaMock_and_savesProduct() throws Exception {
        ProductController controller = new ProductController();
        KafkaMockService kafkaMockService = mock(KafkaMockService.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        BrandRepository brandRepository = mock(BrandRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        FavouriteProductsRepository favRepo = mock(FavouriteProductsRepository.class);
        StockChangeLogRepository scl = mock(StockChangeLogRepository.class);

        // mock kafka payload
        KafkaMockService.AddProductMessage msg = new KafkaMockService().getAddProductMock();
        when(kafkaMockService.getAddProductMock()).thenReturn(msg);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(55);
            return p;
        });

        setField(controller, "kafkaMockService", kafkaMockService);
        setField(controller, "productRepository", productRepository);
        setField(controller, "brandRepository", brandRepository);
        setField(controller, "categoryRepository", categoryRepository);
        setField(controller, "reviewRepository", reviewRepository);
        setField(controller, "favouriteProductsRepository", favRepo);
        setField(controller, "stockChangeLogRepository", scl);

        var dto = controller.addProduct();
        assertNotNull(dto);
        assertEquals(55, dto.getId().intValue());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void addReviewFromEvent_createsReview_and_updatesProductCalification() throws Exception {
        ProductController controller = new ProductController();
        KafkaMockService kafkaMockService = mock(KafkaMockService.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);

        // create product and existing reviews
        Product product = new Product(); product.setId(11); product.setTitle("P");
        Review r1 = new Review(); r1.setId(1L); r1.setCalification(4.0f); r1.setProduct(product);
        when(kafkaMockService.getProductReviewMock()).thenReturn(new KafkaMockService().getProductReviewMock());
        when(productRepository.findById(11)).thenReturn(Optional.of(product));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0); r.setId(2L); return r;
        });
        when(reviewRepository.findByProduct(product)).thenReturn(List.of(r1, new Review()));

        setField(controller, "kafkaMockService", kafkaMockService);
        setField(controller, "productRepository", productRepository);
        setField(controller, "reviewRepository", reviewRepository);
        setField(controller, "brandRepository", mock(BrandRepository.class));
        setField(controller, "categoryRepository", mock(CategoryRepository.class));
        setField(controller, "favouriteProductsRepository", mock(FavouriteProductsRepository.class));
        setField(controller, "stockChangeLogRepository", mock(StockChangeLogRepository.class));

        var resp = controller.addReviewFromEvent();
        assertNotNull(resp);
        assertEquals(11, resp.getProductId().intValue());
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(productRepository, times(1)).save(any(Product.class));
    }
}
