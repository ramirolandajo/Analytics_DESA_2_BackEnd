package ar.edu.uade.analytics.DTO;

import lombok.Data;

@Data
public class BrandDTO {
    private Long id;
    private String name;
    private Boolean active;

    public BrandDTO(Long id, String name) {
        this.id = id;
        this.name = name;
        this.active = true;
    }
    public BrandDTO(Long id, String name, Boolean active) {
        this.id = id;
        this.name = name;
        this.active = active;
    }
    public BrandDTO() {}
}
