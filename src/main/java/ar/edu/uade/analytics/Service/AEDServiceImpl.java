package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Purchase;
import ar.edu.uade.analytics.Entity.CartItem;
import org.springframework.stereotype.Service;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AEDServiceImpl implements AEDService {
    @Override
    public Table getPurchasesTable(List<Purchase> purchases) {
        IntColumn purchaseId = IntColumn.create("purchaseId");
        DoubleColumn finalPrice = DoubleColumn.create("finalPrice");
        IntColumn productId = IntColumn.create("productId");
        IntColumn quantity = IntColumn.create("quantity");
        StringColumn status = StringColumn.create("status");
        for (Purchase purchase : purchases) {
            if (purchase.getCart() != null && purchase.getCart().getItems() != null) {
                for (CartItem item : purchase.getCart().getItems()) {
                    purchaseId.append(purchase.getId());
                    finalPrice.append(purchase.getCart().getFinalPrice() != null ? purchase.getCart().getFinalPrice() : 0.0);
                    productId.append(item.getProduct().getId());
                    quantity.append(item.getQuantity() != null ? item.getQuantity() : 0);
                    status.append(purchase.getStatus().name());
                }
            }
        }
        return Table.create("Purchases", purchaseId, finalPrice, productId, quantity, status);
    }

    @Override
    public Map<String, Object> getPurchaseStatistics(List<Purchase> purchases) {
        Table table = getPurchasesTable(purchases);
        Map<String, Object> stats = new HashMap<>();
        if (table.rowCount() > 0) {
            DoubleColumn finalPrice = table.doubleColumn("finalPrice");
            IntColumn quantity = table.intColumn("quantity");
            stats.put("mediaPrecioFinal", finalPrice.mean());
            stats.put("medianaPrecioFinal", finalPrice.median());
            stats.put("desviacionPrecioFinal", finalPrice.standardDeviation());
            stats.put("mediaCantidad", quantity.mean());
            stats.put("medianaCantidad", quantity.median());
            stats.put("desviacionCantidad", quantity.standardDeviation());
        }
        return stats;
    }

    @Override
    public Map<String, Object> getHistogramData(List<Purchase> purchases) {
        Table table = getPurchasesTable(purchases);
        Map<String, Object> result = new HashMap<>();
        if (table.rowCount() > 0) {
            DoubleColumn finalPrice = table.doubleColumn("finalPrice");
            // Histograma de precios finales (bines de 10)
            double min = finalPrice.min();
            double max = finalPrice.max();
            int bins = 10;
            double binSize = (max - min) / bins;
            int[] counts = new int[bins];
            for (double v : finalPrice.asDoubleArray()) {
                int bin = (int) ((v - min) / binSize);
                if (bin == bins) bin--;
                counts[bin]++;
            }
            result.put("bins", bins);
            result.put("min", min);
            result.put("max", max);
            result.put("binSize", binSize);
            result.put("counts", counts);
        }
        return result;
    }

    @Override
    public Map<String, Object> getCorrelationData(List<Purchase> purchases) {
        Table table = getPurchasesTable(purchases);
        Map<String, Object> result = new HashMap<>();
        if (table.rowCount() > 0) {
            DoubleColumn finalPrice = table.doubleColumn("finalPrice");
            IntColumn quantity = table.intColumn("quantity");
            double[] x = finalPrice.asDoubleArray();
            int[] y = quantity.asIntArray();
            double meanX = 0, meanY = 0;
            for (int i = 0; i < x.length; i++) {
                meanX += x[i];
                meanY += y[i];
            }
            meanX /= x.length;
            meanY /= y.length;
            double num = 0, denX = 0, denY = 0;
            for (int i = 0; i < x.length; i++) {
                num += (x[i] - meanX) * (y[i] - meanY);
                denX += Math.pow(x[i] - meanX, 2);
                denY += Math.pow(y[i] - meanY, 2);
            }
            double correlation = num / (Math.sqrt(denX) * Math.sqrt(denY));
            result.put("correlation_finalPrice_quantity", correlation);
        }
        return result;
    }

    @Override
    public Map<String, Object> getOutliers(List<Purchase> purchases) {
        Table table = getPurchasesTable(purchases);
        Map<String, Object> result = new HashMap<>();
        if (table.rowCount() > 0) {
            DoubleColumn finalPrice = table.doubleColumn("finalPrice");
            double q1 = finalPrice.quartile1();
            double q3 = finalPrice.quartile3();
            double iqr = q3 - q1;
            double lower = q1 - 1.5 * iqr;
            double upper = q3 + 1.5 * iqr;
            List<Double> outliers = new java.util.ArrayList<>();
            for (double v : finalPrice.asDoubleArray()) {
                if (v < lower || v > upper) outliers.add(v);
            }
            result.put("outliers_finalPrice", outliers);
            result.put("lowerBound", lower);
            result.put("upperBound", upper);
        }
        return result;
    }

    @Override
    public Map<String, Object> getNullCounts(List<Purchase> purchases) {
        Table table = getPurchasesTable(purchases);
        Map<String, Object> result = new HashMap<>();
        for (String col : table.columnNames()) {
            result.put(col, table.column(col).countMissing());
        }
        return result;
    }
}
