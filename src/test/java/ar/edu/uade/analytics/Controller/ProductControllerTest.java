package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Communication.KafkaMockService;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Repository.ProductRepository;
import ar.edu.uade.analytics.Repository.BrandRepository;
import ar.edu.uade.analytics.Repository.CategoryRepository;
import ar.edu.uade.analytics.Repository.ReviewRepository;
import ar.edu.uade.analytics.Repository.FavouriteProductsRepository;
import ar.edu.uade.analytics.Repository.StockChangeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock KafkaMockService kafkaMockService;
    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock FavouriteProductsRepository favouriteProductsRepository;
    @Mock StockChangeLogRepository stockChangeLogRepository;

    ProductController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new ProductController();
        java.lang.reflect.Field f;
        f = ProductController.class.getDeclaredField("kafkaMockService"); f.setAccessible(true); f.set(controller, kafkaMockService);
        f = ProductController.class.getDeclaredField("productRepository"); f.setAccessible(true); f.set(controller, productRepository);
        f = ProductController.class.getDeclaredField("brandRepository"); f.setAccessible(true); f.set(controller, brandRepository);
        f = ProductController.class.getDeclaredField("categoryRepository"); f.setAccessible(true); f.set(controller, categoryRepository);
        f = ProductController.class.getDeclaredField("reviewRepository"); f.setAccessible(true); f.set(controller, reviewRepository);
        f = ProductController.class.getDeclaredField("favouriteProductsRepository"); f.setAccessible(true); f.set(controller, favouriteProductsRepository);
        f = ProductController.class.getDeclaredField("stockChangeLogRepository"); f.setAccessible(true); f.set(controller, stockChangeLogRepository);
    }

    @Test
    void addProduct_createsProduct_and_returnsDTO() {
        // Using actual kafka mock from Communication package
        KafkaMockService.AddProductMessage msg = new KafkaMockService().getAddProductMock();
        when(kafkaMockService.getAddProductMock()).thenReturn(msg);
        // repository save returns product with id
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(55);
            return p;
        });

        var dto = controller.addProduct();
        assertNotNull(dto);
        assertEquals(55, dto.getId().longValue());
        verify(productRepository).save(any());
    }

    @Test
    void editProductSimple_updatesPriceAndStock() {
        KafkaMockService.EditProductSimpleMessage msg = new KafkaMockService().getEditProductMockSimple();
        when(kafkaMockService.getEditProductMockSimple()).thenReturn(msg);
        Product p = new Product(); p.setId(10); p.setProductCode(1021); p.setPrice(100f); p.setDiscount(10f);
        when(productRepository.findByProductCode(1021)).thenReturn(p);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = controller.editProductSimple();
        assertNotNull(dto);
        assertEquals(999, dto.getStock().intValue());
        assertEquals(199.99f, dto.getPrice(), 0.001);
        verify(productRepository).save(any());
    }

    @Test
    void activate_and_deactivate_product_flow() {
        KafkaMockService.ActivateProductMessage act = new KafkaMockService().getActivateProductMock();
        KafkaMockService.DeactivateProductMessage deact = new KafkaMockService().getDeactivateProductMock();
        when(kafkaMockService.getActivateProductMock()).thenReturn(act);
        when(kafkaMockService.getDeactivateProductMock()).thenReturn(deact);
        Product prod = new Product(); prod.setId(29); prod.setActive(false);
        when(productRepository.findById(29)).thenReturn(Optional.of(prod));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto1 = controller.activateProduct();
        assertTrue(dto1.getActive());
        var dto2 = controller.deactivateProduct();
        assertFalse(dto2.getActive());
    }

    @Test
    void syncMockFavourite_createsFavourite_whenProductMissing() {
        KafkaMockService.AddFavouriteProductMessage msg = new KafkaMockService().getAddFavouriteProductMock();
        when(kafkaMockService.getAddFavouriteProductMock()).thenReturn(msg);
        when(productRepository.findByProductCode(msg.payload.getProductCode())).thenReturn(null);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0); p.setId(200); return p;
        });
        when(favouriteProductsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<String> resp = controller.syncMockFavouriteProduct();
        assertEquals(200, resp.getStatusCode().value());
        verify(productRepository).save(any());
        verify(favouriteProductsRepository).save(any());
    }

    @Test
    void syncMockStockChangesSimple_processesEvents_and_returnsOk() {
        KafkaMockService.EditProductSimpleMessage ev = new KafkaMockService().getEditProductMockSimple();
        when(kafkaMockService.getEditProductMockSimpleList()).thenReturn(List.of(ev));
        Product p = new Product(); p.setProductCode(1021); p.setStock(1); p.setId(100);
        when(productRepository.findByProductCode(1021)).thenReturn(p);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockChangeLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<String> resp = controller.syncMockStockChangesSimple();
        assertEquals(200, resp.getStatusCode().value());
        verify(stockChangeLogRepository).save(any());
        verify(productRepository).save(any());
    }
}
