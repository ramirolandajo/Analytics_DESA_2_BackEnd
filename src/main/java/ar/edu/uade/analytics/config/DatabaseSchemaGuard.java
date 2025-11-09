package ar.edu.uade.analytics.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseSchemaGuard implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaGuard.class);

    private final JdbcTemplate jdbc;

    public DatabaseSchemaGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String db = jdbc.queryForObject("select database()", String.class);
            if (db == null || db.isBlank()) {
                log.warn("SchemaGuard: no se pudo determinar la base actual; se omite verificación de columnas.");
                return;
            }
            ensureLongText(db, "consumed_event_log", "payload_json");
            ensureLongText(db, "consumed_event_log", "ack_last_error");
        } catch (Exception e) {
            log.warn("SchemaGuard: fallo verificando/ajustando esquema: {}", e.toString());
        }
    }

    private void ensureLongText(String schema, String table, String column) {
        try {
            String sqlType = jdbc.queryForObject(
                    "select DATA_TYPE from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA=? and TABLE_NAME=? and COLUMN_NAME=?",
                    String.class, schema, table, column);
            if (sqlType == null) {
                log.warn("SchemaGuard: columna {}.{} inexistente en esquema {}", table, column, schema);
                return;
            }
            if (!"longtext".equalsIgnoreCase(sqlType)) {
                log.info("SchemaGuard: ajustando {}.{} en {} desde {} a LONGTEXT", table, column, schema, sqlType);
                jdbc.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " LONGTEXT NULL");
            } else {
                log.debug("SchemaGuard: {}.{} ya es LONGTEXT", table, column);
            }
        } catch (Exception e) {
            log.warn("SchemaGuard: no se pudo verificar/ajustar {}.{}: {}", table, column, e.toString());
        }
    }
}
