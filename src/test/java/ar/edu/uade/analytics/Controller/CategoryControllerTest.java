package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Communication.KafkaMockService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import ar.edu.uade.analytics.DTO.CategoryDTO;
import ar.edu.uade.analytics.Entity.Category;
import ar.edu.uade.analytics.Communication.KafkaMockService.CategorySyncMessage;
import ar.edu.uade.analytics.Communication.KafkaMockService.CategorySyncPayload;

@WebMvcTest(CategoryController.class)
public class CategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ar.edu.uade.analytics.Communication.KafkaMockService kafkaMockService;
    @MockBean
    private ar.edu.uade.analytics.Service.CategoryService categoryService;

    @Test
    void contextLoads() {}

    @Test
    void testGetAllCategories_empty() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void testGetAllCategories_withCategories() throws Exception {
        Category cat = new Category();
        cat.setId(1); // Cambiado a Integer
        cat.setName("Electrónica");
        cat.setActive(true);
        when(categoryService.getAllCategories()).thenReturn(List.of(cat));
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Electrónica")));
    }

    @Test
    void testSyncCategoriesFromMock_newCategory() throws Exception {
        CategoryDTO dto = new CategoryDTO(1L, "Electrodomésticos", true);
        CategorySyncPayload payload = new CategorySyncPayload(List.of(dto));
        CategorySyncMessage msg = new CategorySyncMessage("CATEGORY_SYNC", payload, "2025-09-07T00:00:00");
        when(kafkaMockService.getCategoriesMock()).thenReturn(msg);
        // Antes de guardar, no hay categorías
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>())
            .thenReturn(List.of(new Category() {{ setId(1); setName("Electrodomésticos"); setActive(true); }}));
        when(categoryService.saveCategory(any())).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1);
            return c;
        });
        mockMvc.perform(get("/categories/sync"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Electrodomésticos")));
    }

    @Test
    void testSyncCategoriesFromMock_existingCategory() throws Exception {
        CategoryDTO dto = new CategoryDTO(1L, "Electrodomésticos", true);
        CategorySyncPayload payload = new CategorySyncPayload(List.of(dto));
        CategorySyncMessage msg = new CategorySyncMessage("CATEGORY_SYNC", payload, "2025-09-07T00:00:00");
        when(kafkaMockService.getCategoriesMock()).thenReturn(msg);
        Category cat = new Category();
        cat.setId(1);
        cat.setName("Electrodomésticos");
        cat.setActive(true);
        when(categoryService.getAllCategories()).thenReturn(List.of(cat));
        mockMvc.perform(get("/categories/sync"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Electrodomésticos")));
    }

    @Test
    void testAddBulkCategories_emptyList() throws Exception {
        mockMvc.perform(post("/categories/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void testAddBulkCategories_nullList() throws Exception {
        // Enviar body vacío para simular lista nula
        mockMvc.perform(post("/categories/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void testAddBulkCategories_newCategory() throws Exception {
        CategoryDTO dto = new CategoryDTO(null, "Nueva", true);
        Category saved = new Category();
        saved.setId(2);
        saved.setName("Nueva");
        saved.setActive(true);
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        when(categoryService.saveCategory(any())).thenReturn(saved);
        String json = "[{\"name\":\"Nueva\",\"active\":true}]";
        mockMvc.perform(post("/categories/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nueva")));
    }

    @Test
    void testAddCategoryFromMock() throws Exception {
        CategoryDTO dto = new CategoryDTO(null, "MockCat", true);
        CategorySyncPayload payload = new CategorySyncPayload(List.of(dto));
        CategorySyncMessage msg = new CategorySyncMessage(null, payload, null);
        when(kafkaMockService.getCategoriesMock()).thenReturn(msg);
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        Category saved = new Category();
        saved.setId(10);
        saved.setName("MockCat");
        saved.setActive(true);
        when(categoryService.saveCategory(any())).thenReturn(saved);
        mockMvc.perform(post("/categories/mock/add"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MockCat")));
    }

    @Test
    void testActivateCategoryFromMock() throws Exception {
        CategoryDTO dto = new CategoryDTO(1L, "MockCat", false);
        CategorySyncPayload payload = new CategorySyncPayload(List.of(dto));
        CategorySyncMessage msg = new CategorySyncMessage(null, payload, null);
        when(kafkaMockService.getCategoriesMock()).thenReturn(msg);
        Category cat = new Category();
        cat.setId(1);
        cat.setName("MockCat");
        cat.setActive(false);
        when(categoryService.getAllCategories()).thenReturn(List.of(cat));
        Category updated = new Category();
        updated.setId(1);
        updated.setName("MockCat");
        updated.setActive(true);
        when(categoryService.saveCategory(any())).thenReturn(updated);
        mockMvc.perform(patch("/categories/mock/activate"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MockCat")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("true")));
    }

    @Test
    void testDeactivateCategoryFromMock() throws Exception {
        CategoryDTO dto = new CategoryDTO(1L, "MockCat", true);
        CategorySyncPayload payload = new CategorySyncPayload(List.of(dto));
        CategorySyncMessage msg = new CategorySyncMessage(null, payload, null);
        when(kafkaMockService.getCategoriesMock()).thenReturn(msg);
        Category cat = new Category();
        cat.setId(1);
        cat.setName("MockCat");
        cat.setActive(true);
        when(categoryService.getAllCategories()).thenReturn(List.of(cat));
        Category updated = new Category();
        updated.setId(1);
        updated.setName("MockCat");
        updated.setActive(false);
        when(categoryService.saveCategory(any())).thenReturn(updated);
        mockMvc.perform(patch("/categories/mock/deactivate"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MockCat")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("false")));
    }

    @Test
    void testUpdateCategoryFromMock() throws Exception {
        CategoryDTO dto = new CategoryDTO(1L, "MockCatUpdated", false);
        CategorySyncPayload payload = new CategorySyncPayload(List.of(dto));
        CategorySyncMessage msg = new CategorySyncMessage(null, payload, null);
        when(kafkaMockService.getCategoriesMock()).thenReturn(msg);
        // Antes de actualizar, debe existir la categoría con el nombre original
        Category cat = new Category();
        cat.setId(1);
        cat.setName("MockCatUpdated");
        cat.setActive(true);
        when(categoryService.getAllCategories()).thenReturn(List.of(cat));
        Category updated = new Category();
        updated.setId(1);
        updated.setName("MockCatUpdated");
        updated.setActive(false);
        when(categoryService.saveCategory(any())).thenReturn(updated);
        mockMvc.perform(patch("/categories/mock/update"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MockCatUpdated")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("false")));
    }

    @Test
    void testAddBulkCategories_nullName_activeFalse() throws Exception {
        // No debe agregarse ninguna categoría con nombre null y active false
        String json = "[{\"name\":null,\"active\":false}]";
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        mockMvc.perform(post("/categories/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void testAddBulkCategories_nullName_activeTrue() throws Exception {
        // Debe agregarse una categoría con nombre null y active true
        String json = "[{\"name\":null,\"active\":true}]";
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        Category saved = new Category();
        saved.setId(100);
        saved.setName(null);
        saved.setActive(true);
        when(categoryService.saveCategory(any())).thenReturn(saved);
        mockMvc.perform(post("/categories/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("null")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("true")));
    }

    @Test
    void testAddBulkCategories_nullName_duplicateInRequest() throws Exception {
        // Solo debe agregarse una categoría con nombre null por petición
        String json = "[{\"name\":null,\"active\":true},{\"name\":null,\"active\":true}]";
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        Category saved = new Category();
        saved.setId(101);
        saved.setName(null);
        saved.setActive(true);
        when(categoryService.saveCategory(any())).thenReturn(saved);
        mockMvc.perform(post("/categories/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("null")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("true")));
    }

    @Test
    void testAddBulkCategories_nullName_alreadyExists() throws Exception {
        // No debe agregarse si ya existe una categoría con nombre null
        Category existing = new Category();
        existing.setId(102);
        existing.setName(null);
        existing.setActive(true);
        when(categoryService.getAllCategories()).thenReturn(List.of(existing));
        String json = "[{\"name\":null,\"active\":true}]";
        mockMvc.perform(post("/categories/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("null")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("true")));
    }

    @Test
    void testAddBulkCategories_duplicateNameInRequest() throws Exception {
        // Solo debe agregarse una categoría con el mismo nombre por petición
        String json = "[{\"name\":\"Repetida\",\"active\":true},{\"name\":\"Repetida\",\"active\":true}]";
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        Category saved = new Category();
        saved.setId(103);
        saved.setName("Repetida");
        saved.setActive(true);
        when(categoryService.saveCategory(any())).thenReturn(saved);
        mockMvc.perform(post("/categories/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Repetida")));
    }

    @Test
    void testAddBulkCategories_alreadyExists() throws Exception {
        // Si ya existe, debe devolver la existente
        Category existing = new Category();
        existing.setId(104);
        existing.setName("Existente");
        existing.setActive(true);
        when(categoryService.getAllCategories()).thenReturn(List.of(existing));
        String json = "[{\"name\":\"Existente\",\"active\":true}]";
        mockMvc.perform(post("/categories/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Existente")));
    }

    @Test
    void testAddBulkCategories_newCategory_activeNull() throws Exception {
        // Si active es null, debe agregarse como true
        String json = "[{\"name\":\"NuevaNullActive\"}]";
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        Category saved = new Category();
        saved.setId(105);
        saved.setName("NuevaNullActive");
        saved.setActive(true);
        when(categoryService.saveCategory(any())).thenReturn(saved);
        mockMvc.perform(post("/categories/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("NuevaNullActive")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("true")));
    }

    @Test
    void testAddBulkCategories_withNullElement() throws Exception {
        // Si la lista contiene un elemento nulo, debe ser ignorado
        String json = "[null, {\"name\":\"SoloValida\",\"active\":true}]";
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        Category saved = new Category();
        saved.setId(200);
        saved.setName("SoloValida");
        saved.setActive(true);
        when(categoryService.saveCategory(any())).thenReturn(saved);
        mockMvc.perform(post("/categories/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SoloValida")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("true")));
    }

    @Test
    void testAddBulkCategories_saveReturnsNull() throws Exception {
        // Si saveCategory retorna null, no debe agregarse nada
        String json = "[{\"name\":\"NoSeGuarda\",\"active\":true}]";
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        when(categoryService.saveCategory(any())).thenReturn(null);
        mockMvc.perform(post("/categories/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void testActivateCategoryFromMock_notFound() throws Exception {
        CategoryDTO dto = new CategoryDTO(1L, "Inexistente", true);
        CategorySyncPayload payload = new CategorySyncPayload(List.of(dto));
        CategorySyncMessage msg = new CategorySyncMessage(null, payload, null);
        when(kafkaMockService.getCategoriesMock()).thenReturn(msg);
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        mockMvc.perform(patch("/categories/mock/activate"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testDeactivateCategoryFromMock_notFound() throws Exception {
        CategoryDTO dto = new CategoryDTO(1L, "Inexistente", true);
        CategorySyncPayload payload = new CategorySyncPayload(List.of(dto));
        CategorySyncMessage msg = new CategorySyncMessage(null, payload, null);
        when(kafkaMockService.getCategoriesMock()).thenReturn(msg);
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        mockMvc.perform(patch("/categories/mock/deactivate"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testUpdateCategoryFromMock_notFound() throws Exception {
        CategoryDTO dto = new CategoryDTO(1L, "Inexistente", true);
        CategorySyncPayload payload = new CategorySyncPayload(List.of(dto));
        CategorySyncMessage msg = new CategorySyncMessage(null, payload, null);
        when(kafkaMockService.getCategoriesMock()).thenReturn(msg);
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        mockMvc.perform(patch("/categories/mock/update"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testAddCategoryFromMock_nullName_alreadyExists() throws Exception {
        // El mock trae una categoría con nombre null y ya existe en la base
        CategoryDTO dto = new CategoryDTO(null, null, true);
        CategorySyncPayload payload = new CategorySyncPayload(List.of(dto));
        CategorySyncMessage msg = new CategorySyncMessage(null, payload, null);
        when(kafkaMockService.getCategoriesMock()).thenReturn(msg);
        Category existing = new Category();
        existing.setId(300);
        existing.setName(null);
        existing.setActive(true);
        when(categoryService.getAllCategories()).thenReturn(List.of(existing));
        mockMvc.perform(post("/categories/mock/add"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("null")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("true")));
    }

    @Test
    void testAddCategoryFromMock_nullName_new() throws Exception {
        // El mock trae una categoría con nombre null y no existe en la base
        CategoryDTO dto = new CategoryDTO(null, null, true);
        CategorySyncPayload payload = new CategorySyncPayload(List.of(dto));
        CategorySyncMessage msg = new CategorySyncMessage(null, payload, null);
        when(kafkaMockService.getCategoriesMock()).thenReturn(msg);
        when(categoryService.getAllCategories()).thenReturn(new ArrayList<>());
        Category saved = new Category();
        saved.setId(301);
        saved.setName(null);
        saved.setActive(true);
        when(categoryService.saveCategory(any())).thenReturn(saved);
        mockMvc.perform(post("/categories/mock/add"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("null")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("true")));
    }
}
