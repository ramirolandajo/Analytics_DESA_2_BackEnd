package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Purchase;
import tech.tablesaw.api.Table;
import java.util.List;
import java.util.Map;

public interface AEDService {
    Table getPurchasesTable(List<Purchase> purchases);
    Map<String, Object> getPurchaseStatistics(List<Purchase> purchases);
    // Histograma de precios finales
    Map<String, Object> getHistogramData(List<Purchase> purchases);
    // Correlación entre cantidad y precio final
    Map<String, Object> getCorrelationData(List<Purchase> purchases);
    // Outliers en precios finales
    Map<String, Object> getOutliers(List<Purchase> purchases);
    // Conteo de nulos por columna
    Map<String, Object> getNullCounts(List<Purchase> purchases);
}
