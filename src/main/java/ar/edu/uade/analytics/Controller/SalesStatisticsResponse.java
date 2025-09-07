package ar.edu.uade.analytics.Controller;

public class SalesStatisticsResponse {
    public Integer totalVentas;
    public Float facturacionTotal;
    public Integer productosVendidos;
    public String chartBase64;

    public SalesStatisticsResponse() {}

    public SalesStatisticsResponse(Integer totalVentas, Float facturacionTotal, Integer productosVendidos, String chartBase64) {
        this.totalVentas = totalVentas;
        this.facturacionTotal = facturacionTotal;
        this.productosVendidos = productosVendidos;
        this.chartBase64 = chartBase64;
    }

    // Getters and setters (optional, for Jackson)
    public Integer getTotalVentas() { return totalVentas; }
    public void setTotalVentas(Integer totalVentas) { this.totalVentas = totalVentas; }
    public Float getFacturacionTotal() { return facturacionTotal; }
    public void setFacturacionTotal(Float facturacionTotal) { this.facturacionTotal = facturacionTotal; }
    public Integer getProductosVendidos() { return productosVendidos; }
    public void setProductosVendidos(Integer productosVendidos) { this.productosVendidos = productosVendidos; }
    public String getChartBase64() { return chartBase64; }
    public void setChartBase64(String chartBase64) { this.chartBase64 = chartBase64; }
}

