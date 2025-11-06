package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class EventDispatcherServiceTest {

    @Test
    void handleUpsertProducto_createsOrUpdatesProduct_and_incrementsCounter() {
        ObjectMapper mapper = new ObjectMapper();
        ProductRepository productRepo = mock(ProductRepository.class);
        BrandRepository brandRepo = mock(BrandRepository.class);
        CategoryRepository categoryRepo = mock(CategoryRepository.class);
        EventRepository eventRepo = mock(EventRepository.class);
        ReviewRepository reviewRepo = mock(ReviewRepository.class);
        FavouriteProductsRepository favRepo = mock(FavouriteProductsRepository.class);
        ViewRepository viewRepo = mock(ViewRepository.class);
        PurchaseRepository purchaseRepo = mock(PurchaseRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        Counter counter = mock(Counter.class);
        doReturn(counter).when(meterRegistry).counter(anyString(), any(String[].class));
        StockChangeLogRepository sclRepo = mock(StockChangeLogRepository.class);
        CartRepository cartRepo = mock(CartRepository.class);

        EventDispatcherService svc = new EventDispatcherService(mapper, productRepo, brandRepo, categoryRepo,
                eventRepo, reviewRepo, favRepo, viewRepo, purchaseRepo, userRepo, meterRegistry, sclRepo, cartRepo);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("productCode", 1234);
        payload.put("name", "Mock Product");
        payload.put("price", 19.9);
        payload.put("stock", 50);
        // no brand/categories

        when(productRepo.findByProductCode(1234)).thenReturn(null);
        when(productRepo.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(99);
            return p;
        });

        svc.handleInventory("post: agregar un producto", payload);

        verify(productRepo, times(1)).save(any(Product.class));
        verify(meterRegistry, atLeastOnce()).counter(eq("analytics.inventory.product.upsert"), any(String[].class));
    }

//    @Test
//    void handleCompraConfirmada_createsUser_product_logs_and_savesPurchase() {
//        ObjectMapper mapper = new ObjectMapper();
//        ProductRepository productRepo = mock(ProductRepository.class);
//        BrandRepository brandRepo = mock(BrandRepository.class);
//        CategoryRepository categoryRepo = mock(CategoryRepository.class);
//        EventRepository eventRepo = mock(EventRepository.class);
//        ReviewRepository reviewRepo = mock(ReviewRepository.class);
//        FavouriteProductsRepository favRepo = mock(FavouriteProductsRepository.class);
//        ViewRepository viewRepo = mock(ViewRepository.class);
//        PurchaseRepository purchaseRepo = mock(PurchaseRepository.class);
//        UserRepository userRepo = mock(UserRepository.class);
//        MeterRegistry meterRegistry = mock(MeterRegistry.class);
//        Counter counter = mock(Counter.class);
//        doReturn(counter).when(meterRegistry).counter(anyString(), any(String[].class));
//        StockChangeLogRepository sclRepo = mock(StockChangeLogRepository.class);
//        CartRepository cartRepo = mock(CartRepository.class);
//
//        EventDispatcherService svc = new EventDispatcherService(mapper, productRepo, brandRepo, categoryRepo,
//                eventRepo, reviewRepo, favRepo, viewRepo, purchaseRepo, userRepo, meterRegistry, sclRepo, cartRepo);
//
//        ObjectNode payload = mapper.createObjectNode();
//        payload.put("type", "post: compra confirmada");
//        ObjectNode user = mapper.createObjectNode();
//        user.put("email", "a@b.com");
//        user.put("name", "Foo");
//        payload.set("user", user);
//        ObjectNode cart = mapper.createObjectNode();
//        cart.put("cartId", 55);
//        cart.put("finalPrice", 100.0);
//        var items = mapper.createArrayNode();
//        ObjectNode it = mapper.createObjectNode();
//        it.put("productCode", 200);
//        it.put("quantity", 3);
//        items.add(it);
//        cart.set("items", items);
//        payload.set("cart", cart);
//
//        when(userRepo.findByEmail("a@b.com")).thenReturn(null);
//        when(userRepo.save(any())).thenAnswer(inv -> {
//            User u = inv.getArgument(0);
//            u.setId(77);
//            return u;
//        });
//        when(productRepo.findByProductCode(200)).thenReturn(null);
//        when(productRepo.save(any())).thenAnswer(inv -> {
//            Product p = inv.getArgument(0);
//            p.setId(33);
//            p.setProductCode(200);
//            return p;
//        });
//        when(productRepo.findByProductCodeIn(any())).thenReturn(Collections.emptyList());
//        when(cartRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
//        when(purchaseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
//
//        svc.handleCompraConfirmada(payload);
//
//        verify(userRepo, times(1)).save(any(User.class));
//        verify(productRepo, atLeastOnce()).save(any(Product.class));
//        verify(sclRepo, times(0)).save(any());
//        verify(purchaseRepo, times(1)).save(any(Purchase.class));
//    }

    @Test
    void handleActualizarStock_createsProduct_and_logsStockChange() {
        ObjectMapper mapper = new ObjectMapper();
        ProductRepository productRepo = mock(ProductRepository.class);
        BrandRepository brandRepo = mock(BrandRepository.class);
        CategoryRepository categoryRepo = mock(CategoryRepository.class);
        EventRepository eventRepo = mock(EventRepository.class);
        ReviewRepository reviewRepo = mock(ReviewRepository.class);
        FavouriteProductsRepository favRepo = mock(FavouriteProductsRepository.class);
        ViewRepository viewRepo = mock(ViewRepository.class);
        PurchaseRepository purchaseRepo = mock(PurchaseRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        Counter counter = mock(Counter.class);
        doReturn(counter).when(meterRegistry).counter(anyString(), any(String[].class));
        StockChangeLogRepository sclRepo = mock(StockChangeLogRepository.class);
        CartRepository cartRepo = mock(CartRepository.class);

        EventDispatcherService svc = new EventDispatcherService(mapper, productRepo, brandRepo, categoryRepo,
                eventRepo, reviewRepo, favRepo, viewRepo, purchaseRepo, userRepo, meterRegistry, sclRepo, cartRepo);

        var payload = mapper.createObjectNode();
        payload.put("productCode", 777);
        payload.put("stock", 10);
        payload.put("reason", "restock");
        when(productRepo.findStockByProductCode(777)).thenReturn(null);
        when(productRepo.findByProductCode(777)).thenReturn(null);
        when(productRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("put: actualizar stock", payload);

        verify(productRepo, times(1)).save(any(Product.class));
        verify(sclRepo, times(1)).save(any());
        verify(meterRegistry, atLeast(1)).counter(eq("analytics.inventory.stock.updated"), any(String[].class));
    }

    @Test
    void handleProductoActivo_setsActiveWhenProductExists() {
        ObjectMapper mapper = new ObjectMapper();
        ProductRepository productRepo = mock(ProductRepository.class);
        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        Counter counter = mock(Counter.class);
        doReturn(counter).when(meterRegistry).counter(anyString(), any(String[].class));
        StockChangeLogRepository sclRepo = mock(StockChangeLogRepository.class);
        // other repos not used
        EventDispatcherService svc = new EventDispatcherService(mapper, productRepo, mock(BrandRepository.class), mock(CategoryRepository.class),
                mock(EventRepository.class), mock(ReviewRepository.class), mock(FavouriteProductsRepository.class), mock(ViewRepository.class),
                mock(PurchaseRepository.class), mock(UserRepository.class), meterRegistry, sclRepo, mock(CartRepository.class));

        Product prod = new Product(); prod.setId(10); prod.setActive(false); prod.setProductCode(321);
        when(productRepo.findByProductCode(321)).thenReturn(prod);
        when(productRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var payload = mapper.createObjectNode();
        payload.put("productCode", 321);

        svc.handleInventory("patch: producto activado", payload);

        verify(productRepo, times(1)).save(any(Product.class));
        // capture saved product to assert active true
        verify(meterRegistry, atLeastOnce()).counter(eq("analytics.inventory.product.active"), any(String[].class));
    }

    @Test
    void handleMarca_createsOrUpdatesBrand_and_incrementsCounter() {
        ObjectMapper mapper = new ObjectMapper();
        BrandRepository brandRepo = mock(BrandRepository.class);
        ProductRepository productRepo = mock(ProductRepository.class);
        CategoryRepository categoryRepo = mock(CategoryRepository.class);
        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        Counter counter = mock(Counter.class);
        doReturn(counter).when(meterRegistry).counter(anyString(), any(String[].class));
        EventDispatcherService svc = new EventDispatcherService(mapper, productRepo, brandRepo, categoryRepo,
                mock(EventRepository.class), mock(ReviewRepository.class), mock(FavouriteProductsRepository.class), mock(ViewRepository.class),
                mock(PurchaseRepository.class), mock(UserRepository.class), meterRegistry, mock(StockChangeLogRepository.class), mock(CartRepository.class));

        var payload = mapper.createObjectNode();
        payload.put("brandCode", 50);
        payload.put("name", "MyBrand");

        when(brandRepo.findByBrandCode(50)).thenReturn(null);
        when(brandRepo.findByNameIgnoreCase("MyBrand")).thenReturn(null);
        when(brandRepo.findById(50)).thenReturn(Optional.empty());
        when(brandRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: marca creada", payload);

        verify(brandRepo, times(1)).save(any(Brand.class));
        verify(meterRegistry, atLeastOnce()).counter(eq("analytics.inventory.brand.upsert"), any(String[].class));
    }

    @Test
    void handleCategoria_createsOrUpdatesCategory_and_incrementsCounter() {
        ObjectMapper mapper = new ObjectMapper();
        CategoryRepository categoryRepo = mock(CategoryRepository.class);
        ProductRepository productRepo = mock(ProductRepository.class);
        BrandRepository brandRepo = mock(BrandRepository.class);
        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        Counter counter = mock(Counter.class);
        doReturn(counter).when(meterRegistry).counter(anyString(), any(String[].class));
        EventDispatcherService svc = new EventDispatcherService(mapper, productRepo, brandRepo, categoryRepo,
                mock(EventRepository.class), mock(ReviewRepository.class), mock(FavouriteProductsRepository.class), mock(ViewRepository.class),
                mock(PurchaseRepository.class), mock(UserRepository.class), meterRegistry, mock(StockChangeLogRepository.class), mock(CartRepository.class));

        var payload = mapper.createObjectNode();
        payload.put("categoryCode", 77);
        payload.put("name", "MyCat");

        when(categoryRepo.findByCategoryCode(77)).thenReturn(null);
        when(categoryRepo.findByNameIgnoreCase("MyCat")).thenReturn(null);
        when(categoryRepo.findById(77)).thenReturn(Optional.empty());
        when(categoryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.handleInventory("post: categoría creada", payload);

        verify(categoryRepo, times(1)).save(any(Category.class));
        verify(meterRegistry, atLeastOnce()).counter(eq("analytics.inventory.category.upsert"), any(String[].class));
    }

    @Test
    void handleBatchProductos_countsOkAndFail() {
        ObjectMapper mapper = new ObjectMapper();
        ProductRepository productRepo = mock(ProductRepository.class);
        doReturn(mock(Counter.class)).when(mock(MeterRegistry.class)).counter(anyString(), any(String[].class));
        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        Counter counter = mock(Counter.class);
        doReturn(counter).when(meterRegistry).counter(anyString(), any(String[].class));

        // make findByProductCode throw for productCode 2 to force a failure
        when(productRepo.findByProductCode(1)).thenReturn(null);
        doThrow(new RuntimeException("boom")).when(productRepo).findByProductCode(2);

        EventDispatcherService svc = new EventDispatcherService(mapper, productRepo, mock(BrandRepository.class), mock(CategoryRepository.class),
                mock(EventRepository.class), mock(ReviewRepository.class), mock(FavouriteProductsRepository.class), mock(ViewRepository.class),
                mock(PurchaseRepository.class), mock(UserRepository.class), meterRegistry, mock(StockChangeLogRepository.class), mock(CartRepository.class));

        var payload = mapper.createObjectNode();
        var items = mapper.createArrayNode();
        var i1 = mapper.createObjectNode(); i1.put("productCode", 1); i1.put("name", "p1");
        var i2 = mapper.createObjectNode(); i2.put("productCode", 2); i2.put("name", "p2");
        items.add(i1); items.add(i2);
        payload.set("items", items);

        svc.handleInventory("post: agregar productos (batch)", payload);

        verify(meterRegistry, atLeastOnce()).counter(eq("analytics.inventory.product.batch.ok"), any(String[].class));
        verify(meterRegistry, atLeastOnce()).counter(eq("analytics.inventory.product.batch.fail"), any(String[].class));
    }

    @Test
    void handleReview_createsReview_and_incrementsCounter() {
        ObjectMapper mapper = new ObjectMapper();
        ProductRepository productRepo = mock(ProductRepository.class);
        ReviewRepository reviewRepo = mock(ReviewRepository.class);
        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        Counter counter = mock(Counter.class);
        doReturn(counter).when(meterRegistry).counter(anyString(), any(String[].class));

        EventDispatcherService svc = new EventDispatcherService(mapper, productRepo, mock(BrandRepository.class), mock(CategoryRepository.class),
                mock(EventRepository.class), reviewRepo, mock(FavouriteProductsRepository.class), mock(ViewRepository.class),
                mock(PurchaseRepository.class), mock(UserRepository.class), meterRegistry, mock(StockChangeLogRepository.class), mock(CartRepository.class));

        var payload = mapper.createObjectNode();
        payload.put("rating", 4);
        payload.put("productCode", 5);
        when(productRepo.findByProductCode(5)).thenReturn(new Product());
        svc.handleSales("post: review creada", payload);

        verify(reviewRepo, times(1)).save(any(Review.class));
        verify(meterRegistry, atLeastOnce()).counter(eq("analytics.sales.review"), any(String[].class));
    }

    @Test
    void handleFavAdd_andRemove_behaviour() {
        ObjectMapper mapper = new ObjectMapper();
        ProductRepository productRepo = mock(ProductRepository.class);
        FavouriteProductsRepository favRepo = mock(FavouriteProductsRepository.class);
        ViewRepository viewRepo = mock(ViewRepository.class);
        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        Counter counter = mock(Counter.class);
        doReturn(counter).when(meterRegistry).counter(anyString(), any(String[].class));
        EventDispatcherService svc = new EventDispatcherService(mapper, productRepo, mock(BrandRepository.class), mock(CategoryRepository.class),
                mock(EventRepository.class), mock(ReviewRepository.class), favRepo, viewRepo,
                mock(PurchaseRepository.class), mock(UserRepository.class), meterRegistry, mock(StockChangeLogRepository.class), mock(CartRepository.class));

        var add = mapper.createObjectNode(); add.put("productCode", 10);
        when(productRepo.findByProductCode(10)).thenReturn(new Product());
        svc.handleSales("post: producto agregado a favoritos", add);
        verify(favRepo, times(1)).save(any(FavouriteProducts.class));

        var remove = mapper.createObjectNode(); remove.put("productCode", 10);
        FavouriteProducts f = new FavouriteProducts(); f.setProductCode(10);
        when(favRepo.findAll()).thenReturn(List.of(f));
        svc.handleSales("delete: producto quitado de favoritos", remove);
        verify(favRepo, times(1)).delete(any(FavouriteProducts.class));
    }

    @Test
    void handleVistaDiaria_savesViews_forArrayPayload() {
        ObjectMapper mapper = new ObjectMapper();
        ViewRepository viewRepo = mock(ViewRepository.class);
        ProductRepository productRepo = mock(ProductRepository.class);
        MeterRegistry meterRegistry = mock(MeterRegistry.class);
        Counter counter = mock(Counter.class);
        doReturn(counter).when(meterRegistry).counter(anyString(), any(String[].class));

        EventDispatcherService svc = new EventDispatcherService(mapper, productRepo, mock(BrandRepository.class), mock(CategoryRepository.class),
                mock(EventRepository.class), mock(ReviewRepository.class), mock(FavouriteProductsRepository.class), viewRepo,
                mock(PurchaseRepository.class), mock(UserRepository.class), meterRegistry, mock(StockChangeLogRepository.class), mock(CartRepository.class));

        var arr = mapper.createArrayNode();
        var v1 = mapper.createObjectNode(); v1.put("productCode", 5);
        var v2 = mapper.createObjectNode(); v2.put("productCode", 6);
        arr.add(v1); arr.add(v2);

        when(productRepo.findByProductCode(5)).thenReturn(new Product());
        when(productRepo.findByProductCode(6)).thenReturn(null);

        svc.handleSales("get: vista diaria de productos", arr, OffsetDateTime.now());

        verify(viewRepo, times(2)).save(any(View.class));
        verify(meterRegistry, atLeastOnce()).counter(eq("analytics.sales.view.daily"), any(String[].class));
    }

}
