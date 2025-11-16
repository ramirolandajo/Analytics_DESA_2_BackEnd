package ar.edu.uade.analytics.DTO;

import ar.edu.uade.analytics.Entity.Product;
import tech.tablesaw.aggregate.NumericAggregateFunction;

import static tech.tablesaw.aggregate.AggregateFunctions.product;

public class ProductViewStats {
    private final String productCode;
    private final long views;

    public ProductViewStats(String productCode, long views) {
        this.productCode = productCode;
        this.views = views;
    }

    public String getProductCode() {
        return productCode;
    }

    public long getViews() {
        return views;
    }

    public NumericAggregateFunction getProduct() {
        return product;
    }
}
