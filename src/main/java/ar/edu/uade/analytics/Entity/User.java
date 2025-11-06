package ar.edu.uade.analytics.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Setter;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;
    @Column(name = "lastname")
    private String lastname;
    @Column(name = "email", unique = true)
    private String email;

    @JsonIgnore
    @Column(name = "password")
    private String password;
    @Column(name = "role")
    private String role;
    @Column(name = "account_active")
    private boolean accountActive = false;

    @Column
    private Boolean sessionActive = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("cart-user")
    private List<Cart> carts;

    @Setter
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("purchase-user")
    private List<Purchase> purchases;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("favourite-user")
    private List<FavouriteProducts> favouriteProducts;

    public boolean isEmpty() {
        return this.id == null && this.name == null && this.lastname == null && this.email == null && this.password == null && this.role == null;
    }

    public Boolean getSessionActive() {
        return sessionActive;
    }

    public void setSessionActive(Boolean sessionActive) {
        this.sessionActive = sessionActive;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isAccountActive() {
        return accountActive;
    }

    public void setAccountActive(boolean accountActive) {
        this.accountActive = accountActive;
    }

    public void setCarts(List<Cart> c) {
        this.carts = c;
    }

    public void setFavouriteProducts(List<FavouriteProducts> favouriteProducts) {
        this.favouriteProducts = favouriteProducts;

    }

    public List<Cart> getCarts() {
        return carts;
    }

    public List<Purchase> getPurchases() {
        return purchases;
    }

    public List<FavouriteProducts> getFavouriteProducts() {
        return favouriteProducts;
    }
}
