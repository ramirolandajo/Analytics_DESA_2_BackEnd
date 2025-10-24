package ar.edu.uade.analytics.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "purchases")
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne // se quita cascade = CascadeType.ALL para evitar persist sobre Cart ya existente
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonBackReference("purchase-cart")
    private Cart cart;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "reservation_time")
    private LocalDateTime reservationTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column
    private String direction;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("purchase-user")
    private User user;

    public enum Status {
        CONFIRMED,
        PENDING,
        CANCELLED
    }
}
