//package ar.edu.uade.analytics.Service;
//
//import ar.edu.uade.analytics.Entity.*;
//import ar.edu.uade.analytics.Repository.*;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//public class EventDispatcherServiceSalesTest {
//
//    private ObjectMapper mapper;
//    private ProductRepository productRepository;
//    private BrandRepository brandRepository;
//    private CategoryRepository categoryRepository;
//    private EventRepository eventRepository;
//    private ReviewRepository reviewRepository;
//    private FavouriteProductsRepository favouriteProductsRepository;
//    private ViewRepository viewRepository;
//    private PurchaseRepository purchaseRepository;
//    private UserRepository userRepository;
//    private StockChangeLogRepository stockChangeLogRepository;
//
//    private EventDispatcherService service;
//
//    @BeforeEach
//    void setup() {
//        mapper = new ObjectMapper();
//        productRepository = mock(ProductRepository.class);
//        brandRepository = mock(BrandRepository.class);
//        categoryRepository = mock(CategoryRepository.class);
//        eventRepository = mock(EventRepository.class);
//        reviewRepository = mock(ReviewRepository.class);
//        favouriteProductsRepository = mock(FavouriteProductsRepository.class);
//        viewRepository = mock(ViewRepository.class);
//        purchaseRepository = mock(PurchaseRepository.class);
//        userRepository = mock(UserRepository.class);
//        stockChangeLogRepository = mock(StockChangeLogRepository.class);
//
//        service = new EventDispatcherService(
//                mapper,
//                productRepository,
//                brandRepository,
//                categoryRepository,
//                eventRepository,
//                reviewRepository,
//                favouriteProductsRepository,
//                viewRepository,
//                purchaseRepository,
//                userRepository,
//                new SimpleMeterRegistry(),
//                stockChangeLogRepository
//        );
//    }
//
//    @Test
//    void compraConfirmada_creaUsuarioCarritoItemsYCompra() throws Exception {
//        String json = """
//        {
//        	"eventId": "d6d93e55-a1fd-4fdf-b565-1af91a4cdf45",
//        	"eventType": "POST: Compra confirmada",
//        	"timestamp": 1759923314.084080900,
//        	"originModule": "Ventas",
//        	"payload": {
//        		"purchaseId": 15,
//        		"user": {
//        			"name": "Enzo",
//        			"id": 7,
//        			"email": "asplanattienzo@gmail.com"
//        		},
//        		"cart": {
//        			"cartId": 15,
//        			"finalPrice": 3896.07,
//        			"items": [
//        				{
//        					"productCode": 10016,
//        					"quantity": 3,
//        					"price": 1215.0,
//        					"id": 18,
//        					"title": "Notebook Profesional X1"
//        				},
//        				{
//        					"productCode": 10017,
//        					"quantity": 3,
//        					"price": 83.69,
//        					"id": 19,
//        					"title": "Auriculares Gamer Pro"
//        				}
//        			]
//        		},
//        		"status": "CONFIRMED"
//        	}
//        }
//        """;
//        JsonNode node = mapper.readTree(json).get("payload");
//
//        // Stubs: no existe el usuario, existen 2 productos por productCode
//        when(userRepository.findByEmail("asplanattienzo@gmail.com")).thenReturn(null);
//        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
//            User u = inv.getArgument(0);
//            // simular id generado
//            u.setId(1);
//            return u;
//        });
//        Product p1 = new Product(); p1.setId(11); p1.setProductCode(10016);
//        Product p2 = new Product(); p2.setId(12); p2.setProductCode(10017);
//        when(productRepository.findByProductCode(10016)).thenReturn(p1);
//        when(productRepository.findByProductCode(10017)).thenReturn(p2);
//
//        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
//        when(purchaseRepository.save(purchaseCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
//
//        service.handleSales("post: compra confirmada", node);
//
//        Purchase saved = purchaseCaptor.getValue();
//        assertThat(saved).isNotNull();
//        assertThat(saved.getUser()).isNotNull();
//        assertThat(saved.getUser().getEmail()).isEqualTo("asplanattienzo@gmail.com");
//        assertThat(saved.getCart()).isNotNull();
//        assertThat(saved.getCart().getExternalCartId()).isEqualTo(15);
//        assertThat(saved.getCart().getFinalPrice()).isEqualTo(3896.07f);
//        assertThat(saved.getCart().getItems()).hasSize(2);
//        assertThat(saved.getCart().getItems().get(0).getProduct().getProductCode()).isEqualTo(10016);
//        assertThat(saved.getCart().getItems().get(1).getProduct().getProductCode()).isEqualTo(10017);
//    }
//}
//
