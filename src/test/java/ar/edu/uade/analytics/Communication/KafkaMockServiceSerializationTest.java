package ar.edu.uade.analytics.Communication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaMockServiceSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final KafkaMockService svc = new KafkaMockService();

    @Test
    void serialize_addProduct_and_containsTypeAndPayload() throws Exception {
        var msg = svc.getAddProductMock();
        String json = mapper.writeValueAsString(msg);
        JsonNode root = mapper.readTree(json);
        assertEquals("AddProduct", root.get("type").asText());
        assertTrue(root.has("payload"));
        assertTrue(root.get("payload").has("product"));
    }

    @Test
    void serialize_editProductSimple_and_containsFields() throws Exception {
        var msg = svc.getEditProductMockSimple();
        String json = mapper.writeValueAsString(msg);
        JsonNode root = mapper.readTree(json);
        assertEquals("EditProductSimple", root.get("type").asText());
        JsonNode payload = root.get("payload");
        assertNotNull(payload);
        assertEquals(1021, payload.get("productCode").asInt());
        assertEquals(999, payload.get("stock").asInt());
    }

    @Test
    void serialize_editProductFull_and_containsBrandAndCategories() throws Exception {
        var msg = svc.getEditProductMockFull();
        String json = mapper.writeValueAsString(msg);
        JsonNode root = mapper.readTree(json);
        assertEquals("EditProductFull", root.get("type").asText());
        JsonNode payload = root.get("payload");
        assertNotNull(payload);
        assertTrue(payload.has("brand"));
        assertTrue(payload.has("categories"));
    }

    @Test
    void serialize_activateDeactivate_and_idsPresent() throws Exception {
        var a = svc.getActivateProductMock();
        var d = svc.getDeactivateProductMock();
        JsonNode ra = mapper.readTree(mapper.writeValueAsString(a));
        JsonNode rd = mapper.readTree(mapper.writeValueAsString(d));
        assertEquals("ActivateProduct", ra.get("type").asText());
        assertEquals(29L, ra.get("payload").get("id").asLong());
        assertEquals("DeactivateProduct", rd.get("type").asText());
        assertEquals(29L, rd.get("payload").get("id").asLong());
    }

    @Test
    void serialize_productReview_and_fields() throws Exception {
        var msg = svc.getProductReviewMock();
        JsonNode root = mapper.readTree(mapper.writeValueAsString(msg));
        assertEquals("ProductReview", root.get("type").asText());
        JsonNode payload = root.get("payload");
        assertEquals(11, payload.get("productId").asInt());
        assertEquals(4.5, payload.get("calification").asDouble(), 0.0001);
    }

    @Test
    void serialize_dailyProductViews_and_listNotEmpty() throws Exception {
        var msg = svc.getDailyProductViewsMock();
        JsonNode root = mapper.readTree(mapper.writeValueAsString(msg));
        assertEquals("DAILY_PRODUCT_VIEWS", root.get("type").asText());
        JsonNode products = root.get("payload").get("products");
        assertTrue(products.isArray());
        assertTrue(products.size() >= 10);
    }

    @Test
    void serialize_saleEventMock_and_containsCartUser() throws Exception {
        var msg = svc.getSaleEventMock();
        JsonNode root = mapper.readTree(mapper.writeValueAsString(msg));
        assertEquals("StockConfirmed_CartPurchase", root.get("type").asText());
        JsonNode payload = root.get("payload");
        assertTrue(payload.has("cart"));
        assertTrue(payload.has("user"));
    }
}

