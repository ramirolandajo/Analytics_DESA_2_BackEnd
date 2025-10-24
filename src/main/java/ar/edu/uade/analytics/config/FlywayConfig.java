package ar.edu.uade.analytics.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

@Configuration
@ConditionalOnProperty(value = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return new FlywayMigrationStrategy() {
            @Override
            public void migrate(Flyway flyway) {
                // Reparar historial (checksums/failed) y luego migrar
                try {
                    flyway.repair();
                } catch (Exception ignore) {
                    // Si no hay nada que reparar, continuar
                }
                flyway.migrate();
            }
        };
    }
}

