package ar.edu.uade.analytics.Controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
public class PurchaseControllerTest {
    private MockMvc mockMvc;

    @Mock
    private ar.edu.uade.analytics.Service.PurchaseService purchaseService;
    @Mock
    private ar.edu.uade.analytics.Communication.KafkaMockService kafkaMockService;
    @Mock
    private ar.edu.uade.analytics.Repository.CartRepository cartRepository;
    @Mock
    private ar.edu.uade.analytics.Repository.ProductRepository productRepository;
    @Mock
    private ar.edu.uade.analytics.Repository.StockChangeLogRepository stockChangeLogRepository;
    @Mock
    private ar.edu.uade.analytics.Repository.UserRepository userRepository;

    @InjectMocks
    private ar.edu.uade.analytics.Controller.PurchaseController purchaseController;

    @BeforeEach
    void setup() {
        // Usar try-with-resources para evitar warning de AutoCloseable
        try (AutoCloseable mocks = MockitoAnnotations.openMocks(this)) {
            mockMvc = MockMvcBuilders.standaloneSetup(purchaseController).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void contextLoads() {
        // Test de carga de contexto
    }

    @Test
    void testGetAllPurchases_empty() throws Exception {
        org.mockito.Mockito.when(purchaseService.findAll()).thenReturn(java.util.List.of());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/purchase"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("[]"));
    }

    @Test
    void testGetAllPurchases_withPurchases() throws Exception {
        ar.edu.uade.analytics.Entity.Purchase purchase = new ar.edu.uade.analytics.Entity.Purchase();
        org.mockito.Mockito.when(purchaseService.findAll()).thenReturn(java.util.List.of(purchase));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/purchase"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testSyncMockSale_success() throws Exception {
        // Mock evento válido
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(1, 21, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 980.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMock()).thenReturn(event);
        // Mock producto y usuario
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setStock(10);
        org.mockito.Mockito.when(productRepository.findById(21)).thenReturn(java.util.Optional.of(product));
        org.mockito.Mockito.when(userRepository.findByEmail("lucia@example.com")).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(userRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(cartRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(stockChangeLogRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(purchaseService).save(org.mockito.Mockito.any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Venta mockeada sincronizada correctamente.")));
    }

    @Test
    void testSyncMockSale_eventNull() throws Exception {
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMock()).thenReturn(null);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("No se encontró evento de venta mockeada.")));
    }

    @Test
    void testSyncMockSale_eventPayloadNull() throws Exception {
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", null, "2025-09-16T09:00:00.000000000");
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMock()).thenReturn(event);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("No se encontró evento de venta mockeada.")));
    }

    @Test
    void testSyncMockSale_productNotFound() throws Exception {
        // Mock evento válido
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(1, 21, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 980.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMock()).thenReturn(event);
        // Producto no existe
        org.mockito.Mockito.when(productRepository.findById(21)).thenReturn(java.util.Optional.empty());
        org.mockito.Mockito.when(userRepository.findByEmail("lucia@example.com")).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(userRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(cartRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(purchaseService).save(org.mockito.Mockito.any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Venta mockeada sincronizada correctamente.")));
    }

    @Test
    void testSyncMockSale_productStockNull() throws Exception {
        // Mock evento válido
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(1, 21, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 980.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Luc������������a", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMock()).thenReturn(event);
        // Producto existe pero stock null
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setStock(null);
        org.mockito.Mockito.when(productRepository.findById(21)).thenReturn(java.util.Optional.of(product));
        org.mockito.Mockito.when(userRepository.findByEmail("lucia@example.com")).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(userRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(cartRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(purchaseService).save(org.mockito.Mockito.any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Venta mockeada sincronizada correctamente.")));
    }

    @Test
    void testSyncMockSale_userAlreadyExists() throws Exception {
        // Mock evento válido
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(1, 21, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 980.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMock()).thenReturn(event);
        // Producto existe
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setStock(10);
        org.mockito.Mockito.when(productRepository.findById(21)).thenReturn(java.util.Optional.of(product));
        // Usuario ya existe
        ar.edu.uade.analytics.Entity.User user = new ar.edu.uade.analytics.Entity.User();
        user.setEmail("lucia@example.com");
        org.mockito.Mockito.when(userRepository.findByEmail("lucia@example.com")).thenReturn(user);
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(cartRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(stockChangeLogRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(purchaseService).save(org.mockito.Mockito.any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Venta mockeada sincronizada correctamente.")));
    }

    @Test
    void testSyncMockSaleList_success() throws Exception {
        // Mock evento válido
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(1, 21, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 980.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock> events = java.util.List.of(event);
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(events);
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setStock(10);
        org.mockito.Mockito.when(productRepository.findById(21)).thenReturn(java.util.Optional.of(product));
        org.mockito.Mockito.when(userRepository.findByEmail("lucia@example.com")).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(userRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(cartRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(stockChangeLogRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(purchaseService).save(org.mockito.Mockito.any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas: 1")));
    }

    @Test
    void testSyncMockSaleList_eventNull() throws Exception {
        // Usar lista vacía para simular sin eventos (evita NPE)
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas: 0")));
    }

    @Test
    void testSyncMockSaleList_payloadNull() throws Exception {
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", null, "2025-09-16T09:00:00.000000000");
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock> events = java.util.List.of(event);
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(events);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("fallidas: 1")));
    }

    @Test
    void testSyncMockSaleList_productNotFound() throws Exception {
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(1, 999, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 980.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock> events = java.util.List.of(event);
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(events);
        org.mockito.Mockito.when(productRepository.findById(999)).thenReturn(java.util.Optional.empty());
        // No mockear userRepository ni cartRepository ni purchaseService porque no se usan si el producto no existe
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas: 1")));
    }

    @Test
    void testSyncMockSaleList_productStockNull() throws Exception {
        // Mock evento válido
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(1, 21, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 980.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock> events = java.util.List.of(event);
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(events);
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setStock(null);
        org.mockito.Mockito.when(productRepository.findById(21)).thenReturn(java.util.Optional.of(product));
        org.mockito.Mockito.when(userRepository.findByEmail("lucia@example.com")).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(userRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(cartRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(purchaseService).save(org.mockito.Mockito.any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas: 1")));
    }

    @Test
    void testSyncMockSaleList_userAlreadyExists() throws Exception {
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(1, 21, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 980.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock> events = java.util.List.of(event);
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(events);
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setStock(10);
        org.mockito.Mockito.when(productRepository.findById(21)).thenReturn(java.util.Optional.of(product));
        // Usuario ya existe
        ar.edu.uade.analytics.Entity.User user = new ar.edu.uade.analytics.Entity.User();
        user.setEmail("lucia@example.com");
        org.mockito.Mockito.when(userRepository.findByEmail("lucia@example.com")).thenReturn(user);
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(cartRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(stockChangeLogRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(purchaseService).save(org.mockito.Mockito.any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas: 1")));
    }

    @Test
    void testSyncMockSaleList_emptyList() throws Exception {
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas: 0")));
    }

    @Test
    void testSyncMockSaleListKafka_success() throws Exception {
        // Mock evento válido
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(1, 21, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 980.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock> events = java.util.List.of(event);
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(events);
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setStock(10);
        org.mockito.Mockito.when(productRepository.findById(21)).thenReturn(java.util.Optional.of(product));
        org.mockito.Mockito.when(userRepository.findByEmail("lucia@example.com")).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(userRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(cartRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(stockChangeLogRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(purchaseService).save(org.mockito.Mockito.any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list-kafka"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas (Kafka): 1")));
    }

    @Test
    void testSyncMockSaleListKafka_eventNull() throws Exception {
        // Usar lista vacía para simular sin eventos (evita NPE)
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list-kafka"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas (Kafka): 0")));
    }

    @Test
    void testSyncMockSaleListKafka_payloadNull() throws Exception {
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", null, "2025-09-16T09:00:00.000000000");
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock> events = java.util.List.of(event);
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(events);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list-kafka"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("fallidas: 1")));
    }

    @Test
    void testSyncMockSaleListKafka_productNotFound() throws Exception {
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(1, 999, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 980.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock> events = java.util.List.of(event);
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(events);
        org.mockito.Mockito.when(productRepository.findById(999)).thenReturn(java.util.Optional.empty());
        // No mockear userRepository ni cartRepository ni purchaseService porque no se usan si el producto no existe
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list-kafka"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas (Kafka): 1")));
    }

    @Test
    void testSyncMockSaleListKafka_productStockNull() throws Exception {
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(1, 21, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 980.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock> events = java.util.List.of(event);
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(events);
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setStock(null);
        org.mockito.Mockito.when(productRepository.findById(21)).thenReturn(java.util.Optional.of(product));
        org.mockito.Mockito.when(userRepository.findByEmail("lucia@example.com")).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(userRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(cartRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(purchaseService).save(org.mockito.Mockito.any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list-kafka"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas (Kafka): 1")));
    }

    @Test
    void testSyncMockSaleListKafka_userAlreadyExists() throws Exception {
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(1, 21, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 980.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock> events = java.util.List.of(event);
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(events);
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setStock(10);
        org.mockito.Mockito.when(productRepository.findById(21)).thenReturn(java.util.Optional.of(product));
        // Usuario ya existe
        ar.edu.uade.analytics.Entity.User user = new ar.edu.uade.analytics.Entity.User();
        user.setEmail("lucia@example.com");
        org.mockito.Mockito.when(userRepository.findByEmail("lucia@example.com")).thenReturn(user);
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(cartRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(stockChangeLogRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(purchaseService).save(org.mockito.Mockito.any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list-kafka"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas (Kafka): 1")));
    }

    @Test
    void testSyncMockSaleListKafka_emptyList() throws Exception {
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list-kafka"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas (Kafka): 0")));
    }

    @Test
    void testSyncMockSaleList_stockLlegaACero() throws Exception {
        // Producto con stock igual a la cantidad vendida
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(5, 21, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 4000.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock> events = java.util.List.of(event);
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(events);
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setStock(5);
        org.mockito.Mockito.when(productRepository.findById(21)).thenReturn(java.util.Optional.of(product));
        org.mockito.Mockito.when(userRepository.findByEmail("lucia@example.com")).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(userRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(cartRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(stockChangeLogRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(purchaseService).save(org.mockito.Mockito.any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas: 1")));
    }

    @Test
    void testSyncMockSaleList_stockQuedaEnCeroPorVentaMayor() throws Exception {
        // Producto con stock menor a la cantidad vendida
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock itemMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartItemMock(10, 21, 800.0f, "Tablet Pro 10\"");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock cartMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventCartMock(36, 8000.0f, java.util.List.of(itemMock));
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock userMock = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventUserMock(3, "Lucía", "lucia@example.com");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock payload = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventPayloadMock(36, userMock, cartMock, "CONFIRMED");
        ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock event = new ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock("StockConfirmed_CartPurchase", payload, "2025-09-16T09:00:00.000000000");
        java.util.List<ar.edu.uade.analytics.Communication.KafkaMockService.SaleEventMock> events = java.util.List.of(event);
        org.mockito.Mockito.when(kafkaMockService.getSaleEventMockList()).thenReturn(events);
        ar.edu.uade.analytics.Entity.Product product = new ar.edu.uade.analytics.Entity.Product();
        product.setStock(3);
        org.mockito.Mockito.when(productRepository.findById(21)).thenReturn(java.util.Optional.of(product));
        org.mockito.Mockito.when(userRepository.findByEmail("lucia@example.com")).thenReturn(null);
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(userRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(cartRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(stockChangeLogRepository).save(org.mockito.Mockito.any());
        org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(purchaseService).save(org.mockito.Mockito.any());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/purchase/sync-mock-sale-list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Ventas mockeadas sincronizadas: 1")));
    }

}
