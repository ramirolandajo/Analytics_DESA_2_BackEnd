package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Communication.KafkaMockService;
import ar.edu.uade.analytics.Entity.Product;
import ar.edu.uade.analytics.Entity.View;
import ar.edu.uade.analytics.Repository.ProductRepository;
import ar.edu.uade.analytics.Repository.ViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductViewControllerTest {
    @Mock
    KafkaMockService kafkaMockServiceSync;
    @Mock
    ProductRepository productRepository;
    @Mock
    ViewRepository viewRepository;
    @InjectMocks
    ProductViewController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ProductViewController();
        controller.kafkaMockServiceSync = kafkaMockServiceSync;
        controller.productRepository = productRepository;
        controller.viewRepository = viewRepository;
    }

    @Test
    void testConstructor() {
        assertNotNull(new ProductViewController());
    }

    @Test
    void testSyncProductViews_matchAndNoMatch() {
        // Prepare mock event
        KafkaMockService.ProductViewDTO dto1 = new KafkaMockService.ProductViewDTO(1, "Producto1", 1);
        KafkaMockService.ProductViewDTO dto2 = new KafkaMockService.ProductViewDTO(2, "Producto2", 2);
        KafkaMockService.DailyProductViewsPayload payload = new KafkaMockService.DailyProductViewsPayload(List.of(dto1, dto2));
        KafkaMockService.DailyProductViewsMessage event = new KafkaMockService.DailyProductViewsMessage(
                "DAILY_PRODUCT_VIEWS",
                payload,
                LocalDateTime.now().toString()
        );
        when(kafkaMockServiceSync.getDailyProductViewsMock()).thenReturn(event);

        // Prepare products in repo
        Product prodA = new Product();
        prodA.setProductCode(1);
        Product prodX = new Product();
        prodX.setProductCode(3);
        when(productRepository.findAll()).thenReturn(List.of(prodA, prodX));

        // Mock viewRepository.save
        when(viewRepository.save(any(View.class))).thenAnswer(inv -> inv.getArgument(0));

        List<View> result = controller.syncProductViews();
        // Only one match (A)
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getProductCode());
    }

    @Test
    void testSyncProductViews_noMatches() {
        KafkaMockService.ProductViewDTO dto = new KafkaMockService.ProductViewDTO(99, "Producto99", 99);
        KafkaMockService.DailyProductViewsPayload payload = new KafkaMockService.DailyProductViewsPayload(List.of(dto));
        KafkaMockService.DailyProductViewsMessage event = new KafkaMockService.DailyProductViewsMessage(
                "DAILY_PRODUCT_VIEWS",
                payload,
                LocalDateTime.now().toString()
        );
        when(kafkaMockServiceSync.getDailyProductViewsMock()).thenReturn(event);
        Product prodA = new Product();
        prodA.setProductCode(1);
        when(productRepository.findAll()).thenReturn(List.of(prodA));
        List<View> result = controller.syncProductViews();
        assertTrue(result.isEmpty());
    }

    @Test
    void testSyncProductViews_emptyProductsAndDTOs() {
        KafkaMockService.DailyProductViewsPayload payload = new KafkaMockService.DailyProductViewsPayload(new ArrayList<>());
        KafkaMockService.DailyProductViewsMessage event = new KafkaMockService.DailyProductViewsMessage(
                "DAILY_PRODUCT_VIEWS",
                payload,
                LocalDateTime.now().toString()
        );
        when(kafkaMockServiceSync.getDailyProductViewsMock()).thenReturn(event);
        List<View> result = controller.syncProductViews();
        assertTrue(result.isEmpty());
    }

    @Test
    void testSyncProductViews_nullProductCode() {
        KafkaMockService.ProductViewDTO dto = new KafkaMockService.ProductViewDTO(1, "ProductoNull", null);
        KafkaMockService.DailyProductViewsPayload payload = new KafkaMockService.DailyProductViewsPayload(List.of(dto));
        KafkaMockService.DailyProductViewsMessage event = new KafkaMockService.DailyProductViewsMessage(
                "DAILY_PRODUCT_VIEWS",
                payload,
                LocalDateTime.now().toString()
        );
        when(kafkaMockServiceSync.getDailyProductViewsMock()).thenReturn(event);
        Product prodA = new Product();
        prodA.setProductCode(1);
        when(productRepository.findAll()).thenReturn(List.of(prodA));
        List<View> result = controller.syncProductViews();
        assertTrue(result.isEmpty());
    }

    @Test
    void testSyncProductViews_multipleMatches() {
        KafkaMockService.ProductViewDTO dto1 = new KafkaMockService.ProductViewDTO(1, "Producto1", 1);
        KafkaMockService.ProductViewDTO dto2 = new KafkaMockService.ProductViewDTO(2, "Producto3", 3);
        KafkaMockService.DailyProductViewsPayload payload = new KafkaMockService.DailyProductViewsPayload(List.of(dto1, dto2));
        KafkaMockService.DailyProductViewsMessage event = new KafkaMockService.DailyProductViewsMessage(
                "DAILY_PRODUCT_VIEWS",
                payload,
                LocalDateTime.now().toString()
        );
        when(kafkaMockServiceSync.getDailyProductViewsMock()).thenReturn(event);
        Product prodA = new Product();
        prodA.setProductCode(1);
        Product prodX = new Product();
        prodX.setProductCode(3);
        when(productRepository.findAll()).thenReturn(List.of(prodA, prodX));
        when(viewRepository.save(any(View.class))).thenAnswer(inv -> inv.getArgument(0));
        List<View> result = controller.syncProductViews();
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(v -> Integer.valueOf(1).equals(v.getProductCode())));
        assertTrue(result.stream().anyMatch(v -> Integer.valueOf(3).equals(v.getProductCode())));
    }

    @Test
    void testSyncProductViews_productCodeNullInRepo() {
        KafkaMockService.ProductViewDTO dto = new KafkaMockService.ProductViewDTO(1, "Producto1", 1);
        KafkaMockService.DailyProductViewsPayload payload = new KafkaMockService.DailyProductViewsPayload(List.of(dto));
        KafkaMockService.DailyProductViewsMessage event = new KafkaMockService.DailyProductViewsMessage(
                "DAILY_PRODUCT_VIEWS",
                payload,
                LocalDateTime.now().toString()
        );
        when(kafkaMockServiceSync.getDailyProductViewsMock()).thenReturn(event);
        Product prodNull = new Product();
        prodNull.setProductCode(null);
        when(productRepository.findAll()).thenReturn(List.of(prodNull));
        List<View> result = controller.syncProductViews();
        assertTrue(result.isEmpty());
    }
}
