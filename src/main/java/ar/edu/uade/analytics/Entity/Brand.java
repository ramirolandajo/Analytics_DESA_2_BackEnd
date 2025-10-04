package ar.edu.uade.analytics.Entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private String name;

    @Column(unique = true)
    private Integer brandCode;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "brand")
    @JsonManagedReference("brand-product")
    private List<Product> products;

    public Brand() {
        this.active = true;
    }
}
