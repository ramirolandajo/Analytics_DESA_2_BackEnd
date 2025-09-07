package ar.edu.uade.analytics.Controller;

import org.junit.jupiter.api.Test;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYSeries;
import java.awt.Color;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class AEDAnalyticsControllerStyleTest {
    // --- Pie Chart ---
    @Test
    void testApplyPieChartStyle_variasSecciones() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Facturación Total (en miles)", 100);
        dataset.setValue("Otra Sección", 50);
        JFreeChart chart = ChartFactory.createPieChart("Test Pie", dataset, true, false, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyPieChartStyle", JFreeChart.class, PiePlot.class);
        m.setAccessible(true);
        m.invoke(controller, chart, plot);
        assertEquals(Color.WHITE, plot.getSectionPaint("Facturación Total (en miles)"));
        assertEquals(Color.BLACK, plot.getSectionOutlinePaint("Otra Sección"));
    }

    @Test
    void testApplyPieChartStyle_sinLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("A", 1);
        JFreeChart chart = ChartFactory.createPieChart("Pie sin leyenda", dataset, false, false, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyPieChartStyle", JFreeChart.class, PiePlot.class);
        m.setAccessible(true);
        m.invoke(controller, chart, plot);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyPieChartStyle_sinTitulo() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("A", 1);
        JFreeChart chart = ChartFactory.createPieChart(null, dataset, true, false, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyPieChartStyle", JFreeChart.class, PiePlot.class);
        m.setAccessible(true);
        m.invoke(controller, chart, plot);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyPieChartStyle_unaSeccion() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Solo", 1);
        JFreeChart chart = ChartFactory.createPieChart("Pie uno", dataset, true, false, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyPieChartStyle", JFreeChart.class, PiePlot.class);
        m.setAccessible(true);
        m.invoke(controller, chart, plot);
        assertEquals(Color.BLACK, plot.getSectionOutlinePaint("Solo"));
    }

    @Test
    void testApplyPieChartStyle_soloSeccionEspecial() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Facturación Total (en miles)", 100);
        JFreeChart chart = ChartFactory.createPieChart("Test Pie", dataset, false, false, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyPieChartStyle", JFreeChart.class, PiePlot.class);
        m.setAccessible(true);
        m.invoke(controller, chart, plot);
        assertEquals(Color.WHITE, plot.getSectionPaint("Facturación Total (en miles)"));
    }

    @Test
    void testApplyPieChartStyle_soloSeccionNormal() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Otra", 100);
        JFreeChart chart = ChartFactory.createPieChart("Test Pie", dataset, false, false, false);
        PiePlot plot = (PiePlot) chart.getPlot();
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyPieChartStyle", JFreeChart.class, PiePlot.class);
        m.setAccessible(true);
        m.invoke(controller, chart, plot);
        assertEquals(Color.BLACK, plot.getSectionOutlinePaint("Otra"));
    }

    @Test
    void testApplyPieChartStyle_datasetVacio() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        org.jfree.data.general.DefaultPieDataset dataset = new org.jfree.data.general.DefaultPieDataset();
        JFreeChart chart = ChartFactory.createPieChart("Pie vacío", dataset, true, false, false);
        org.jfree.chart.plot.PiePlot plot = (org.jfree.chart.plot.PiePlot) chart.getPlot();
        java.lang.reflect.Method m = AEDAnalyticsController.class.getDeclaredMethod("applyPieChartStyle", JFreeChart.class, org.jfree.chart.plot.PiePlot.class);
        m.setAccessible(true);
        m.invoke(controller, chart, plot);
        assertEquals(java.awt.Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyPieChartStyle_sinTituloNiLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        org.jfree.data.general.DefaultPieDataset dataset = new org.jfree.data.general.DefaultPieDataset();
        dataset.setValue("Facturación Total (en miles)", 100);
        // Crear gráfico sin título ni leyenda
        JFreeChart chart = ChartFactory.createPieChart(null, dataset, false, false, false);
        org.jfree.chart.plot.PiePlot plot = (org.jfree.chart.plot.PiePlot) chart.getPlot();
        java.lang.reflect.Method m = AEDAnalyticsController.class.getDeclaredMethod("applyPieChartStyle", JFreeChart.class, org.jfree.chart.plot.PiePlot.class);
        m.setAccessible(true);
        m.invoke(controller, chart, plot);
        assertEquals(java.awt.Color.WHITE, plot.getSectionPaint("Facturación Total (en miles)"));
    }

    @Test
    void testApplyPieChartStyle_facturacionTotalSinMiles() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        org.jfree.data.general.DefaultPieDataset dataset = new org.jfree.data.general.DefaultPieDataset();
        dataset.setValue("Facturación Total", 123);
        JFreeChart chart = ChartFactory.createPieChart("Pie FT", dataset, false, false, false);
        org.jfree.chart.plot.PiePlot plot = (org.jfree.chart.plot.PiePlot) chart.getPlot();
        java.lang.reflect.Method m = AEDAnalyticsController.class.getDeclaredMethod("applyPieChartStyle", JFreeChart.class, org.jfree.chart.plot.PiePlot.class);
        m.setAccessible(true);
        m.invoke(controller, chart, plot);
        assertEquals(java.awt.Color.WHITE, plot.getSectionPaint("Facturación Total"));
    }

    // --- Line Chart ---
    @Test
    void testApplyLineChartStyle_conTituloYLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(1, "Serie1", "A");
        JFreeChart chart = ChartFactory.createLineChart("Line con leyenda", "X", "Y", dataset);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyLineChartStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
        assertEquals(Color.BLACK, chart.getTitle().getPaint());
    }

    @Test
    void testApplyLineChartStyle_sinLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(1, "Serie1", "A");
        JFreeChart chart = ChartFactory.createLineChart("Line sin leyenda", "X", "Y", dataset, org.jfree.chart.plot.PlotOrientation.VERTICAL, false, false, false);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyLineChartStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyLineChartStyle_sinTitulo() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(1, "Serie1", "A");
        JFreeChart chart = ChartFactory.createLineChart(null, "X", "Y", dataset);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyLineChartStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyLineChartStyle_sinTituloNiLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(1, "Serie1", "A");
        // Crear gráfico sin título ni leyenda
        JFreeChart chart = ChartFactory.createLineChart(null, "X", "Y", dataset, org.jfree.chart.plot.PlotOrientation.VERTICAL, false, false, false);
        java.lang.reflect.Method m = AEDAnalyticsController.class.getDeclaredMethod("applyLineChartStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(java.awt.Color.WHITE, chart.getBackgroundPaint());
    }

    // --- Bar Chart ---
    @Test
    void testApplyBarChartStyle_conTituloYLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(1, "Serie1", "A");
        JFreeChart chart = ChartFactory.createBarChart("Bar con leyenda", "X", "Y", dataset, org.jfree.chart.plot.PlotOrientation.VERTICAL, true, false, false);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyBarChartStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
        assertEquals(Color.BLACK, chart.getTitle().getPaint());
    }

    @Test
    void testApplyBarChartStyle_sinLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(1, "Serie1", "A");
        JFreeChart chart = ChartFactory.createBarChart("Bar sin leyenda", "X", "Y", dataset, org.jfree.chart.plot.PlotOrientation.VERTICAL, false, false, false);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyBarChartStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyBarChartStyle_sinTitulo() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(1, "Serie1", "A");
        JFreeChart chart = ChartFactory.createBarChart(null, "X", "Y", dataset, org.jfree.chart.plot.PlotOrientation.VERTICAL, false, false, false);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyBarChartStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyBarChartStyle_sinTituloNiLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(1, "Serie1", "A");
        // Crear gráfico sin título ni leyenda
        JFreeChart chart = ChartFactory.createBarChart(null, "X", "Y", dataset, org.jfree.chart.plot.PlotOrientation.VERTICAL, false, false, false);
        java.lang.reflect.Method m = AEDAnalyticsController.class.getDeclaredMethod("applyBarChartStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(java.awt.Color.WHITE, chart.getBackgroundPaint());
    }

    // --- Scatter Plot ---
    @Test
    void testApplyScatterChartStyle_conTituloYLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        org.jfree.data.xy.XYSeriesCollection dataset = new org.jfree.data.xy.XYSeriesCollection();
        XYSeries series = new XYSeries("Serie1");
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createScatterPlot("Scatter con leyenda", "X", "Y", dataset, org.jfree.chart.plot.PlotOrientation.VERTICAL, true, false, false);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyScatterChartStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
        assertEquals(Color.BLACK, chart.getTitle().getPaint());
    }

    @Test
    void testApplyScatterChartStyle_sinLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        org.jfree.data.xy.XYSeriesCollection dataset = new org.jfree.data.xy.XYSeriesCollection();
        XYSeries series = new XYSeries("Serie1");
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createScatterPlot("Scatter sin leyenda", "X", "Y", dataset, org.jfree.chart.plot.PlotOrientation.VERTICAL, false, false, false);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyScatterChartStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyScatterChartStyle_sinTitulo() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        org.jfree.data.xy.XYSeriesCollection dataset = new org.jfree.data.xy.XYSeriesCollection();
        XYSeries series = new XYSeries("Serie1");
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createScatterPlot(null, "X", "Y", dataset, org.jfree.chart.plot.PlotOrientation.VERTICAL, false, false, false);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyScatterChartStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyScatterChartStyle_sinTituloNiLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        org.jfree.data.xy.XYSeriesCollection dataset = new org.jfree.data.xy.XYSeriesCollection();
        XYSeries series = new XYSeries("Serie1");
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createScatterPlot(null, "X", "Y", dataset, org.jfree.chart.plot.PlotOrientation.VERTICAL, false, false, false);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyScatterChartStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    // --- Box Plot ---
    @Test
    void testApplyBoxPlotStyle_conTituloYLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        dataset.add(java.util.Arrays.asList(1, 2, 3), "Outliers", "Valores");
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart("Box con leyenda", "Cat", "Val", dataset, true);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyBoxPlotStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
        assertEquals(Color.BLACK, chart.getTitle().getPaint());
    }

    @Test
    void testApplyBoxPlotStyle_sinLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        dataset.add(java.util.Arrays.asList(1, 2, 3), "Outliers", "Valores");
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart("Box sin leyenda", "Cat", "Val", dataset, false);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyBoxPlotStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyBoxPlotStyle_sinTitulo() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
        dataset.add(java.util.Arrays.asList(1, 2, 3), "Outliers", "Valores");
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(null, "Cat", "Val", dataset, false);
        Method m = AEDAnalyticsController.class.getDeclaredMethod("applyBoxPlotStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(Color.WHITE, chart.getBackgroundPaint());
    }

    @Test
    void testApplyBoxPlotStyle_sinTituloNiLeyenda() throws Exception {
        AEDAnalyticsController controller = new AEDAnalyticsController();
        org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset dataset = new org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset();
        dataset.add(java.util.Arrays.asList(1, 2, 3), "Outliers", "Valores");
        // Crear gráfico sin título ni leyenda
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(null, "Cat", "Val", dataset, false);
        java.lang.reflect.Method m = AEDAnalyticsController.class.getDeclaredMethod("applyBoxPlotStyle", JFreeChart.class);
        m.setAccessible(true);
        m.invoke(controller, chart);
        assertEquals(java.awt.Color.WHITE, chart.getBackgroundPaint());
    }
}
