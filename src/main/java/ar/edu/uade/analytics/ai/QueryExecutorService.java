package ar.edu.uade.analytics.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueryExecutorService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> safeQuery(String sql) {
        if (!sql.trim().toLowerCase().startsWith("select")) {
            throw new IllegalArgumentException("Solo se permiten consultas SELECT");
        }
        return jdbcTemplate.queryForList(sql);
    }
}
