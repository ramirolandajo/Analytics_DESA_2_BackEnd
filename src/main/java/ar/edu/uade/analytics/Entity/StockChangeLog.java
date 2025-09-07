package ar.edu.uade.analytics.Entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_change_log")
public class StockChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer oldStock;

    @Column(nullable = false)
    private Integer newStock;

    @Column(nullable = false)
    private Integer quantityChanged;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @Column
    private String reason; // Ej: "Venta", "Actualización manual", etc.
}

