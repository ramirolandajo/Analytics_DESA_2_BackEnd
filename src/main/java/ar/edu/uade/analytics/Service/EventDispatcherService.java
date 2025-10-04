package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.*;
import ar.edu.uade.analytics.Repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class EventDispatcherService {
    private static final Logger log = LoggerFactory.getLogger(EventDispatcherService.class);

    private final ObjectMapper mapper;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final ReviewRepository reviewRepository;
    private final FavouriteProductsRepository favouriteProductsRepository;
    private final ViewRepository viewRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final MeterRegistry meterRegistry;
    private final StockChangeLogRepository stockChangeLogRepository;

    public EventDispatcherService(ObjectMapper mapper,
                                  ProductRepository productRepository,
                                  BrandRepository brandRepository,
                                  CategoryRepository categoryRepository,
                                  EventRepository eventRepository,
                                  ReviewRepository reviewRepository,
                                  FavouriteProductsRepository favouriteProductsRepository,
                                  ViewRepository viewRepository,
                                  PurchaseRepository purchaseRepository,
                                  UserRepository userRepository,
                                  MeterRegistry meterRegistry,
                                  StockChangeLogRepository stockChangeLogRepository) {
        this.mapper = mapper;
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.reviewRepository = reviewRepository;
        this.favouriteProductsRepository = favouriteProductsRepository;
        this.viewRepository = viewRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.meterRegistry = meterRegistry;
        this.stockChangeLogRepository = stockChangeLogRepository;
    }

    // INVENTARIO
    @Transactional
    public void handleInventory(String normalizedType, JsonNode payload) {
        switch (normalizedType) {
            case "put: actualizar stock" -> handleActualizarStock(payload);
            case "post: agregar un producto" -> handleUpsertProducto(payload);
            case "patch: modificar un producto" -> handleUpsertProducto(payload);
            case "patch: producto desactivado" -> handleProductoActivo(payload, false);
            case "patch: producto activado", "patch: activar producto" -> handleProductoActivo(payload, true);
            case "put: producto actualizado" -> handleUpsertProducto(payload);
            case "post: marca creada" -> handleMarca(payload, true);
            case "patch: marca desactivada" -> handleMarca(payload, false);
            case "patch: marca activada" -> handleMarca(payload, true);
            case "post: categoría creada", "post: categoria creada" -> handleCategoria(payload, true);
            case "patch: categoria desactivada", "patch: categoría desactivada" -> handleCategoria(payload, false);
            case "patch: categoria activada", "patch: categoría activada" -> handleCategoria(payload, true);
            case "post: agregar productos (batch)", "post: agregar productos batch" -> handleBatchProductos(payload);
            default -> log.info("Evento inventario ignorado: {}", normalizedType);
        }
    }

    @Transactional
    private void handleActualizarStock(JsonNode p) {
        Integer productCode = getInt(p, "productCode");
        Integer newStock = getInt(p, "stock");
        if (productCode == null || newStock == null) return;

        // Leer el stock anterior directo desde BD para evitar 0 por entidades nuevas/no inicializadas
        Integer persistedOldStock = productRepository.findStockByProductCode(productCode);
        Integer oldStock = persistedOldStock != null ? persistedOldStock : 0;

        Product prod = Optional.ofNullable(productRepository.findByProductCode(productCode)).orElseGet(Product::new);
        boolean creating = prod.getId() == null;
        if (creating) {
            prod.setProductCode(productCode);
            prod.setActive(true);
            if (p.hasNonNull("name")) prod.setTitle(p.get("name").asText());
        }
        prod.setStock(newStock);
        productRepository.save(prod);

        // Registrar log de cambio de stock con oldStock correcto
        StockChangeLog scl = new StockChangeLog();
        scl.setProduct(prod);
        scl.setOldStock(oldStock);
        scl.setNewStock(newStock);
        scl.setQuantityChanged(newStock - oldStock);
        scl.setChangedAt(LocalDateTime.now());
        String reason = null;
        if (p.has("reason")) reason = asText(p, "reason");
        if (reason == null || reason.isBlank()) reason = "Actualización de stock";
        scl.setReason(reason);
        stockChangeLogRepository.save(scl);

        meterRegistry.counter("analytics.inventory.stock.updated").increment();
        meterRegistry.counter("analytics.inventory.stock.log").increment();
    }

    private void handleProductoActivo(JsonNode p, boolean active) {
        Integer productCode = getInt(p, "productCode");
        if (productCode == null) return;
        Product prod = productRepository.findByProductCode(productCode);
        if (prod == null) return;
        prod.setActive(active);
        productRepository.save(prod);
        meterRegistry.counter("analytics.inventory.product.active", "active", String.valueOf(active)).increment();
    }

    private void handleMarca(JsonNode p, boolean active) {
        Integer brandCode = getInt(p, "brandCode");
        Brand brand = null;
        if (brandCode != null) brand = brandRepository.findByBrandCode(brandCode);
        if (brand == null && p.hasNonNull("name")) brand = brandRepository.findByNameIgnoreCase(p.get("name").asText());
        if (brand == null && p.hasNonNull("id")) brand = brandRepository.findById(p.get("id").asInt()).orElse(null);
        if (brand == null) brand = new Brand();
        if (brandCode != null) brand.setBrandCode(brandCode);
        if (p.hasNonNull("name")) brand.setName(p.get("name").asText());
        brand.setActive(active);
        brandRepository.save(brand);
        meterRegistry.counter("analytics.inventory.brand.upsert").increment();
    }

    private void handleCategoria(JsonNode p, boolean active) {
        Integer categoryCode = getInt(p, "categoryCode");
        if (categoryCode == null) {
            // compat: categoriesCode en plural
            categoryCode = getInt(p, "categoriesCode");
        }
        Category cat = null;
        if (categoryCode != null) cat = categoryRepository.findByCategoryCode(categoryCode);
        if (cat == null && p.hasNonNull("name")) cat = categoryRepository.findByNameIgnoreCase(p.get("name").asText());
        if (cat == null && p.hasNonNull("id")) cat = categoryRepository.findById(p.get("id").asInt()).orElse(null);
        if (cat == null) cat = new Category();
        if (categoryCode != null) cat.setCategoryCode(categoryCode);
        if (p.hasNonNull("name")) cat.setName(p.get("name").asText());
        cat.setActive(active);
        categoryRepository.save(cat);
        meterRegistry.counter("analytics.inventory.category.upsert").increment();
    }

    private void handleUpsertProducto(JsonNode p) {
        Integer productCode = getInt(p, "productCode");
        if (productCode == null) return;
        Product prod = Optional.ofNullable(productRepository.findByProductCode(productCode)).orElseGet(Product::new);
        boolean creating = prod.getId() == null;
        prod.setProductCode(productCode);
        if (p.has("name")) prod.setTitle(asText(p, "name"));
        if (p.has("description")) prod.setDescription(asText(p, "description"));
        if (p.has("unitPrice")) prod.setPriceUnit(asFloat(p, "unitPrice"));
        else if (p.has("unit_price")) prod.setPriceUnit(asFloat(p, "unit_price"));
        if (p.has("price")) prod.setPrice(asFloat(p, "price"));
        if (p.has("discount")) prod.setDiscount(asFloat(p, "discount"));
        if (p.has("stock")) prod.setStock(getInt(p, "stock"));
        if (p.has("calification")) prod.setCalification(asFloat(p, "calification"));
        if (p.has("images") && p.get("images").isArray()) {
            List<String> imgs = new ArrayList<>();
            for (JsonNode n : p.get("images")) {
                imgs.add(n.asText());
            }
            prod.setMediaSrc(imgs);
        }
        if (p.has("new")) prod.setNew(p.get("new").asBoolean());
        if (p.has("bestSeller")) prod.setBestseller(p.get("bestSeller").asBoolean());
        if (p.has("featured")) prod.setFeatured(p.get("featured").asBoolean());
        if (p.has("hero")) prod.setHero(p.get("hero").asBoolean());
        if (p.has("active")) prod.setActive(p.get("active").asBoolean());

        // Resolver brand
        Brand brand = resolveBrand(p);
        if (brand != null) prod.setBrand(brand);
        // Resolver categorías
        Set<Category> categories = resolveCategories(p);
        if (!categories.isEmpty()) prod.setCategories(categories);

        productRepository.save(prod);
        meterRegistry.counter("analytics.inventory.product.upsert", "creating", String.valueOf(creating)).increment();
    }

    private Brand resolveBrand(JsonNode p) {
        Integer brandCode = getInt(p, "brandCode");
        if (brandCode == null && p.has("brand")) brandCode = getInt(p, "brand");
        Brand brand = null;
        if (brandCode != null) brand = brandRepository.findByBrandCode(brandCode);
        if (brand == null && p.hasNonNull("brandId")) brand = brandRepository.findById(p.get("brandId").asInt()).orElse(null);
        if (brand == null && p.hasNonNull("brandName")) brand = brandRepository.findByNameIgnoreCase(p.get("brandName").asText());
        return brand;
    }

    private Set<Category> resolveCategories(JsonNode p) {
        List<Integer> codes = getIntList(p, "categoryCodes");
        List<Integer> categoriesArray = Collections.emptyList();
        if (p.has("categories") && p.get("categories").isArray()) {
            List<Integer> tmp = new ArrayList<>();
            for (JsonNode n : p.get("categories")) {
                tmp.add(n.asInt());
            }
            categoriesArray = tmp;
            if (codes.isEmpty()) {
                codes = tmp; // primero intentamos tratarlas como códigos lógicos
            }
        }
        List<Category> list = new ArrayList<>();
        if (!codes.isEmpty()) list.addAll(categoryRepository.findByCategoryCodeIn(codes));
        // Fallback: si no encontró por códigos y había "categories", interpretarlas como IDs
        if (list.isEmpty() && !categoriesArray.isEmpty()) {
            list.addAll(categoryRepository.findByIdIn(categoriesArray));
        }
        if (list.isEmpty()) {
            List<Integer> ids = getIntList(p, "categoryIds");
            if (!ids.isEmpty()) list.addAll(categoryRepository.findByIdIn(ids));
        }
        return new HashSet<>(list);
    }

    private void handleBatchProductos(JsonNode p) {
        if (p == null || !p.has("items") || !p.get("items").isArray()) return;
        int ok = 0, fail = 0;
        for (JsonNode item : p.get("items")) {
            try {
                handleUpsertProducto(item);
                ok++;
            } catch (Exception e) {
                fail++;
                log.warn("Fallo item batch: {}", e.getMessage());
            }
        }
        meterRegistry.counter("analytics.inventory.product.batch.ok").increment(ok);
        meterRegistry.counter("analytics.inventory.product.batch.fail").increment(fail);
    }

    // VENTAS
    public void handleSales(String normalizedType, JsonNode payload) {
        switch (normalizedType) {
            case "post: compra confirmada" -> handleCompraConfirmada(payload);
            case "post: compra pendiente", "delete: compra cancelada" -> saveAnalyticsEvent(normalizedType, payload);
            case "post: review creada" -> handleReview(payload);
            case "post: producto agregado a favoritos" -> handleFavAdd(payload);
            case "delete: producto quitado de favoritos" -> handleFavRemove(payload);
            case "get: vista diaria de productos" -> handleVistaDiaria(payload);
            case "post: stock rollback - compra cancelada", "stockrollback_cartcancelled" -> saveAnalyticsEvent(normalizedType, payload);
            default -> log.info("Evento ventas ignorado: {}", normalizedType);
        }
    }

    private void handleCompraConfirmada(JsonNode p) {
        if (p == null) return;
        // Usuario
        User user = null;
        if (p.has("user") && p.get("user").isObject()) {
            JsonNode u = p.get("user");
            String email = asText(u, "email");
            if (email != null) {
                user = Optional.ofNullable(userRepository.findByEmail(email)).orElseGet(User::new);
                if (user.getId() == null) {
                    user.setEmail(email);
                    if (u.has("name")) user.setName(asText(u, "name"));
                    if (u.has("lastname")) user.setLastname(asText(u, "lastname"));
                    user = userRepository.save(user);
                }
            }
        }
        if (user == null) {
            log.warn("Compra confirmada sin usuario; se ignora");
            return;
        }
        // Carrito e ítems
        Cart cart = new Cart();
        cart.setUser(user);
        if (p.has("cart") && p.get("cart").isObject()) {
            JsonNode c = p.get("cart");
            Integer externalId = getInt(c, "cartId");
            if (externalId == null) externalId = getInt(c, "id");
            cart.setExternalCartId(externalId);
            Float finalPrice = asFloat(c, "finalPrice");
            if (finalPrice == null) finalPrice = asFloat(c, "final_price");
            cart.setFinalPrice(finalPrice);
            if (c.has("items") && c.get("items").isArray()) {
                List<CartItem> items = new ArrayList<>();
                for (JsonNode it : c.get("items")) {
                    Integer productCode = getInt(it, "productCode");
                    if (productCode == null && it.has("product_code")) productCode = getInt(it, "product_code");
                    Integer quantity = getInt(it, "quantity");
                    if (productCode == null || quantity == null) continue;
                    Product prod = productRepository.findByProductCode(productCode);
                    if (prod == null) {
                        log.warn("Producto no encontrado productCode={}; se salta ítem", productCode);
                        continue;
                    }
                    CartItem ci = new CartItem();
                    ci.setCart(cart);
                    ci.setProduct(prod);
                    ci.setQuantity(quantity);
                    items.add(ci);
                }
                cart.setItems(items);
            }
        }
        // Purchase
        Purchase purchase = new Purchase();
        purchase.setCart(cart);
        purchase.setUser(user);
        purchase.setStatus(Purchase.Status.CONFIRMED);
        purchase.setDate(LocalDateTime.now());
        purchaseRepository.save(purchase);
        meterRegistry.counter("analytics.sales.purchase.confirmed").increment();
    }

    private void saveAnalyticsEvent(String type, JsonNode payload) {
        try {
            Event ev = new Event(type, mapper.writeValueAsString(payload));
            ev.setTimestamp(LocalDateTime.now());
            eventRepository.save(ev);
            meterRegistry.counter("analytics.sales.event", "type", type).increment();
        } catch (Exception e) {
            log.warn("No se pudo persistir evento analytics: {}", e.getMessage());
        }
    }

    private void handleReview(JsonNode p) {
        Review r = new Review();
        // rating: aceptar varias claves
        Float rating = null;
        if (p.has("rateUpdated")) rating = asFloat(p, "rateUpdated");
        else if (p.has("rating")) rating = asFloat(p, "rating");
        else if (p.has("calification")) rating = asFloat(p, "calification");
        if (rating == null) rating = 0.0f;
        r.setCalification(rating);

        // descripción: aceptar varias claves
        String desc = null;
        if (p.has("message")) desc = asText(p, "message");
        else if (p.has("description")) desc = asText(p, "description");
        else if (p.has("comment")) desc = asText(p, "comment");
        r.setDescription(desc);

        // asociar producto: primero por productCode, luego por productId
        Integer productCode = getInt(p, "productCode");
        if (productCode == null && p.has("product_code")) productCode = getInt(p, "product_code");
        if (productCode != null) {
            Product prod = productRepository.findByProductCode(productCode);
            if (prod != null) r.setProduct(prod);
        } else if (p.has("productId")) {
            try {
                Integer pid = getInt(p, "productId");
                if (pid != null) {
                    productRepository.findById(pid).ifPresent(r::setProduct);
                }
            } catch (Exception ignore) {}
        }

        reviewRepository.save(r);
        meterRegistry.counter("analytics.sales.review").increment();
    }

    private void handleFavAdd(JsonNode p) {
        Integer productCode = getInt(p, "productCode");
        if (productCode == null) return;
        Product prod = productRepository.findByProductCode(productCode);
        if (prod == null) return; // no crear favorito si no existe el producto
        FavouriteProducts fav = new FavouriteProducts();
        fav.setProduct(prod);
        fav.setProductCode(productCode);
        favouriteProductsRepository.save(fav);
        meterRegistry.counter("analytics.sales.fav", "op", "add").increment();
    }

    private void handleFavRemove(JsonNode p) {
        Integer productCode = getInt(p, "productCode");
        if (productCode == null) return;
        // eliminación simple por productCode: obtener todos y borrar el primero para analytics
        List<FavouriteProducts> list = favouriteProductsRepository.findAll();
        for (FavouriteProducts f : list) {
            if (Objects.equals(f.getProductCode(), productCode)) {
                favouriteProductsRepository.delete(f);
                break;
            }
        }
        meterRegistry.counter("analytics.sales.fav", "op", "remove").increment();
    }

    private void handleVistaDiaria(JsonNode p) {
        if (p == null || !p.isArray()) return;
        for (JsonNode n : p) {
            Integer productCode = getInt(n, "productCode");
            View v = new View();
            v.setViewedAt(LocalDateTime.now());
            v.setProductCode(productCode);
            if (productCode != null) {
                Product prod = productRepository.findByProductCode(productCode);
                if (prod != null) v.setProduct(prod);
            }
            viewRepository.save(v);
        }
        meterRegistry.counter("analytics.sales.view.daily").increment(p.size());
    }

    // helpers
    private Integer getInt(JsonNode p, String field) {
        return (p != null && p.has(field) && !p.get(field).isNull()) ? p.get(field).asInt() : null;
    }
    private Float asFloat(JsonNode p, String field) {
        return (p != null && p.has(field) && !p.get(field).isNull()) ? (float) p.get(field).asDouble() : null;
    }
    private String asText(JsonNode p, String field) {
        return (p != null && p.has(field) && !p.get(field).isNull()) ? p.get(field).asText() : null;
    }
    private List<Integer> getIntList(JsonNode p, String field) {
        if (p == null || !p.has(field) || !p.get(field).isArray()) return Collections.emptyList();
        List<Integer> r = new ArrayList<>();
        p.get(field).forEach(n -> r.add(n.asInt()));
        return r;
    }
}
