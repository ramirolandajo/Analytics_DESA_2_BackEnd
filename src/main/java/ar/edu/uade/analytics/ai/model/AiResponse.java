package ar.edu.uade.analytics.ai.model;

import lombok.Data;

@Data
public class AiResponse {
    private String sql;
    private String visualization;
    private String description;
}
