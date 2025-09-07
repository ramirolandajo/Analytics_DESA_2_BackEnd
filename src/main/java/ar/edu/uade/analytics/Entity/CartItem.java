package ar.edu.uade.analytics.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonBackReference("cart-item")
    private Cart cart;

    // Suponiendo que la entidad Product existe
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference("product-item")
    private Product product;

    @Column(name = "quantity")
    private Integer quantity;
}
