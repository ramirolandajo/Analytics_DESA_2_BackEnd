package ar.edu.uade.analytics.Entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Brand {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String name;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "brand")
    @JsonManagedReference("brand-product")
    private List<Product> products;

    public Brand() {
        this.active = true;
    }
}
