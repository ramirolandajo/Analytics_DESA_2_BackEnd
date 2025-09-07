package ar.edu.uade.analytics.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

// Imports necesarios para JFreeChart y pruebas
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.plot.PiePlot;
import java.awt.Color;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.boot.test.mock.mockito.MockBean;
import ar.edu.uade.analytics.Service.BrandService;
import ar.edu.uade.analytics.Communication.KafkaMockService;
import ar.edu.uade.analytics.DTO.BrandDTO;
import ar.edu.uade.analytics.Entity.Brand;
import org.mockito.Mockito;
import java.util.List;
import static org.mockito.Mockito.when;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(BrandController.class)
public class BrandControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BrandService brandService;
    @MockBean
    private KafkaMockService kafkaMockService;

    @Test
    void contextLoads() {
        // Test de carga de contexto
    }

    @Test
    void testApplyPieChartStyle() {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("A", 1);
        JFreeChart chart = ChartFactory.createPieChart("Test Pie", dataset, false, false, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        // Should not throw
        controller.applyPieChartStyle(chart, plot);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyLineChartStyle() {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(1, "A", "A");
        JFreeChart chart = ChartFactory.createLineChart("Test Line", "cat", "val", dataset);
        // Should not throw
        controller.applyLineChartStyle(chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyBarChartStyle() {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(1, "A", "A");
        JFreeChart chart = ChartFactory.createBarChart("Test Bar", "cat", "val", dataset);
        controller.applyBarChartStyle(chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyBoxPlotStyle() {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset dataset = new org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset();
        dataset.add(java.util.Arrays.asList(1, 2, 3), "Series", "Category");
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart("Test Box", "cat", "val", dataset, false);
        controller.applyBoxPlotStyle(chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyScatterChartStyle() {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        org.jfree.data.xy.XYSeries series = new org.jfree.data.xy.XYSeries("S");
        series.add(1, 2);
        org.jfree.data.xy.XYSeriesCollection dataset = new org.jfree.data.xy.XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createScatterPlot("Test Scatter", "x", "y", dataset);
        controller.applyScatterChartStyle(chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testSyncBrandsFromMock() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", true);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of());
        when(brandService.saveBrand(Mockito.any())).thenReturn(mockBrand);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockMvc.perform(get("/brand/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Samsung"));
    }

    @Test
    void testGetAllBrands() throws Exception {
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockMvc.perform(get("/brand"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Samsung"));
    }

    @Test
    void testAddBrandFromMock() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", true);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of());
        when(brandService.saveBrand(Mockito.any())).thenReturn(mockBrand);
        mockMvc.perform(post("/brand/mock/add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Samsung"));
    }

    @Test
    void testActivateBrandFromMock() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", false);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        mockBrand.setActive(false);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockBrand.setActive(true);
        when(brandService.saveBrand(Mockito.any())).thenReturn(mockBrand);
        mockMvc.perform(patch("/brand/mock/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void testDeactivateBrandFromMock() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", true);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockBrand.setActive(false);
        when(brandService.saveBrand(Mockito.any())).thenReturn(mockBrand);
        mockMvc.perform(patch("/brand/mock/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void testUpdateBrandFromMock_success() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", false);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockBrand.setActive(false);
        when(brandService.saveBrand(Mockito.any())).thenReturn(mockBrand);
        mockMvc.perform(patch("/brand/mock/update"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void testUpdateBrandFromMock_notFound() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", false);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of());
        mockMvc.perform(patch("/brand/mock/update"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Marca no encontrada"));
    }

    @Test
    void testAddBrandFromMock_alreadyExists() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", true);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockMvc.perform(post("/brand/mock/add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Samsung"));
    }

    @Test
    void testSyncBrandsFromMock_alreadyExists() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", true);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockMvc.perform(get("/brand/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Samsung"));
    }

    @Test
    void testActivateBrandFromMock_notFound() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", false);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of());
        mockMvc.perform(patch("/brand/mock/activate"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Marca no encontrada"));
    }

    @Test
    void testDeactivateBrandFromMock_notFound() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of());
        mockMvc.perform(patch("/brand/mock/deactivate"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Marca no encontrada"));
    }

    @Test
    void testSyncBrandsFromMock_nullNameAndNullActive() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, null, null);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName(null);
        mockBrand.setActive(true); // debe setearse a true por defecto
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of());
        when(brandService.saveBrand(Mockito.any())).thenReturn(mockBrand);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockMvc.perform(get("/brand/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").doesNotExist())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void testAddBrandFromMock_nullNameAndNullActive() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, null, null);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName(null);
        mockBrand.setActive(true); // debe setearse a true por defecto
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of());
        when(brandService.saveBrand(Mockito.any())).thenReturn(mockBrand);
        mockMvc.perform(post("/brand/mock/add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").doesNotExist())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void testSyncBrandsFromMock_emptyList() throws Exception {
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of());
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of());
        mockMvc.perform(get("/brand/sync"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void testSyncBrandsFromMock_multipleBrandsIncludingNullName() throws Exception {
        BrandDTO dto1 = new BrandDTO(1L, "Samsung", true);
        BrandDTO dto2 = new BrandDTO(2L, null, null);
        BrandDTO dto3 = new BrandDTO(3L, "LG", null);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(dto1, dto2, dto3));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of());
        Brand brand1 = new Brand(); brand1.setId(1); brand1.setName("Samsung"); brand1.setActive(true);
        Brand brand2 = new Brand(); brand2.setId(2); brand2.setName(null); brand2.setActive(true);
        Brand brand3 = new Brand(); brand3.setId(3); brand3.setName("LG"); brand3.setActive(true);
        when(brandService.saveBrand(Mockito.any())).thenReturn(brand1, brand2, brand3);
        when(brandService.getAllBrands()).thenReturn(List.of(brand1, brand2, brand3));
        mockMvc.perform(get("/brand/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Samsung')].active").value(true))
                .andExpect(jsonPath("$[?(@.name=='LG')].active").value(true))
                .andExpect(jsonPath("$[?(@.name==null)].active").value(true));
    }

    @Test
    void testAddBrandFromMock_nameNullInDBButNotInMock() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", true);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName(null);
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        // No debe encontrar coincidencia, así que debe intentar crear una nueva marca
        Brand newBrand = new Brand();
        newBrand.setId(2);
        newBrand.setName("Samsung");
        newBrand.setActive(true);
        when(brandService.saveBrand(Mockito.any())).thenReturn(newBrand);
        mockMvc.perform(post("/brand/mock/add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Samsung"));
    }

    @Test
    void testAddBrandFromMock_nameNullInMockButNotInDB() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, null, true);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        // No debe encontrar coincidencia, así que debe intentar crear una nueva marca
        Brand newBrand = new Brand();
        newBrand.setId(2);
        newBrand.setName(null);
        newBrand.setActive(true);
        when(brandService.saveBrand(Mockito.any())).thenReturn(newBrand);
        mockMvc.perform(post("/brand/mock/add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").doesNotExist());
    }

    @Test
    void testActivateBrandFromMock_nameNullInDB() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", true);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName(null);
        mockBrand.setActive(false);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockMvc.perform(patch("/brand/mock/activate"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Marca no encontrada"));
    }

    @Test
    void testActivateBrandFromMock_nameNullInMock() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, null, true);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        mockBrand.setActive(false);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockMvc.perform(patch("/brand/mock/activate"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Marca no encontrada"));
    }

    @Test
    void testDeactivateBrandFromMock_nameNullInDB() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", true);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName(null);
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockMvc.perform(patch("/brand/mock/deactivate"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Marca no encontrada"));
    }

    @Test
    void testDeactivateBrandFromMock_nameNullInMock() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, null, true);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockMvc.perform(patch("/brand/mock/deactivate"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Marca no encontrada"));
    }

    @Test
    void testUpdateBrandFromMock_nameNullInDB() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", false);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName(null);
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockMvc.perform(patch("/brand/mock/update"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Marca no encontrada"));
    }

    @Test
    void testUpdateBrandFromMock_nameNullInMock() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, null, false);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        mockMvc.perform(patch("/brand/mock/update"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Marca no encontrada"));
    }

    @Test
    void testUpdateBrandFromMock_activeNull() throws Exception {
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", null);
        Brand mockBrand = new Brand();
        mockBrand.setId(1);
        mockBrand.setName("Samsung");
        mockBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(mockBrand));
        // No debe cambiar el valor de active
        when(brandService.saveBrand(Mockito.any())).thenAnswer(invocation -> {
            Brand b = invocation.getArgument(0);
            assertEquals(true, b.isActive()); // sigue igual
            return b;
        });
        mockMvc.perform(patch("/brand/mock/update"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void testSyncBrandsFromMock_existingBrandNotAddedAgain() throws Exception {
        // Marca en mock y en base, mismo nombre
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", true);
        Brand existingBrand = new Brand();
        existingBrand.setId(1);
        existingBrand.setName("Samsung");
        existingBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(existingBrand));
        // No debe llamar a saveBrand
        mockMvc.perform(get("/brand/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Samsung"));
    }

    @Test
    void testAddBrandFromMock_bothNamesNotNullAndDifferent() throws Exception {
        // Marca en base: "LG", en mock: "Samsung"
        BrandDTO mockBrandDTO = new BrandDTO(1L, "Samsung", true);
        Brand existingBrand = new Brand();
        existingBrand.setId(2);
        existingBrand.setName("LG");
        existingBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(existingBrand));
        // Debe crear una nueva marca
        Brand newBrand = new Brand();
        newBrand.setId(3);
        newBrand.setName("Samsung");
        newBrand.setActive(true);
        when(brandService.saveBrand(Mockito.any())).thenReturn(newBrand);
        mockMvc.perform(post("/brand/mock/add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Samsung"));
    }

    @Test
    void testSyncBrandsFromMock_existingBrandCaseInsensitive() throws Exception {
        // Marca en mock: "samsung", en base: "SAMSUNG"
        BrandDTO mockBrandDTO = new BrandDTO(1L, "samsung", true);
        Brand existingBrand = new Brand();
        existingBrand.setId(1);
        existingBrand.setName("SAMSUNG");
        existingBrand.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(existingBrand));
        // No debe llamar a saveBrand, la marca ya existe (case-insensitive)
        mockMvc.perform(get("/brand/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("SAMSUNG"));
    }

    @Test
    void testSyncBrandsFromMock_multipleExistingBrandsOneMatches() throws Exception {
        // En la base hay dos marcas, solo una coincide (case-insensitive)
        BrandDTO mockBrandDTO = new BrandDTO(1L, "samsung", true);
        Brand existingBrand1 = new Brand();
        existingBrand1.setId(1);
        existingBrand1.setName("SAMSUNG");
        existingBrand1.setActive(true);
        Brand existingBrand2 = new Brand();
        existingBrand2.setId(2);
        existingBrand2.setName("LG");
        existingBrand2.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(existingBrand1, existingBrand2));
        // No debe agregar una nueva marca, solo debe devolver las existentes
        mockMvc.perform(get("/brand/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='SAMSUNG')]").exists())
                .andExpect(jsonPath("$[?(@.name=='LG')]").exists());
    }

    @Test
    void testAddBrandFromMock_multipleExistingBrandsOneMatches() throws Exception {
        // En la base hay dos marcas, solo una coincide (case-insensitive)
        BrandDTO mockBrandDTO = new BrandDTO(1L, "samsung", true);
        Brand existingBrand1 = new Brand();
        existingBrand1.setId(1);
        existingBrand1.setName("SAMSUNG");
        existingBrand1.setActive(true);
        Brand existingBrand2 = new Brand();
        existingBrand2.setId(2);
        existingBrand2.setName("LG");
        existingBrand2.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(existingBrand1, existingBrand2));
        // Debe devolver la marca existente que coincide (SAMSUNG)
        mockMvc.perform(post("/brand/mock/add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("SAMSUNG"));
    }

    @Test
    void testAddBrandFromMock_multipleExistingBrandsOneNullNameMatches() throws Exception {
        // En la base hay dos marcas, una con nombre null, el mock también null
        BrandDTO mockBrandDTO = new BrandDTO(1L, null, true);
        Brand existingBrand1 = new Brand();
        existingBrand1.setId(1);
        existingBrand1.setName(null);
        existingBrand1.setActive(true);
        Brand existingBrand2 = new Brand();
        existingBrand2.setId(2);
        existingBrand2.setName("LG");
        existingBrand2.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(existingBrand1, existingBrand2));
        // Debe devolver la marca existente con nombre null
        mockMvc.perform(post("/brand/mock/add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").doesNotExist());
    }

    @Test
    void testAddBrandFromMock_multipleExistingBrandsNoneMatchNullMock() throws Exception {
        // En la base hay dos marcas, ninguna con nombre null, el mock es null
        BrandDTO mockBrandDTO = new BrandDTO(1L, null, true);
        Brand existingBrand1 = new Brand();
        existingBrand1.setId(1);
        existingBrand1.setName("SAMSUNG");
        existingBrand1.setActive(true);
        Brand existingBrand2 = new Brand();
        existingBrand2.setId(2);
        existingBrand2.setName("LG");
        existingBrand2.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(existingBrand1, existingBrand2));
        // Debe crear una nueva marca con nombre null
        Brand newBrand = new Brand();
        newBrand.setId(3);
        newBrand.setName(null);
        newBrand.setActive(true);
        when(brandService.saveBrand(Mockito.any())).thenReturn(newBrand);
        mockMvc.perform(post("/brand/mock/add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").doesNotExist());
    }

    @Test
    void testSyncBrandsFromMock_existingBrandNullName() throws Exception {
        // En el mock hay una marca con nombre null, en la base también
        BrandDTO mockBrandDTO = new BrandDTO(1L, null, true);
        Brand existingBrand1 = new Brand();
        existingBrand1.setId(1);
        existingBrand1.setName(null);
        existingBrand1.setActive(true);
        Brand existingBrand2 = new Brand();
        existingBrand2.setId(2);
        existingBrand2.setName("LG");
        existingBrand2.setActive(true);
        KafkaMockService.BrandSyncPayload payload = new KafkaMockService.BrandSyncPayload(List.of(mockBrandDTO));
        KafkaMockService.BrandSyncMessage mockMsg = new KafkaMockService.BrandSyncMessage("BrandSync", payload, "2025-09-07T12:00:00");
        when(kafkaMockService.getBrandsMock()).thenReturn(mockMsg);
        when(brandService.getAllBrands()).thenReturn(List.of(existingBrand1, existingBrand2));
        // No debe agregar una nueva marca, solo debe devolver las existentes
        mockMvc.perform(get("/brand/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name==null)]").exists())
                .andExpect(jsonPath("$[?(@.name=='LG')]").exists());
    }
}
