package ar.edu.uade.analytics.Entity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "favourite_products")
public class FavouriteProducts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_code", nullable = false)
    private Integer productCode;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference("favourite-user")
    private User user;
}