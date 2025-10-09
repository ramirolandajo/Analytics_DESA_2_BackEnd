package ar.edu.uade.analytics.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Verifica conectividad a Kafka al iniciar la app. Si no hay broker disponible,
 * intenta 3 veces y luego aborta el arranque lanzando una excepción.
 * Si conecta, arranca los listeners manualmente (auto-startup está en false).
 */
@ConditionalOnProperty(value = "analytics.kafka.enabled", havingValue = "true")
@ConditionalOnClass(KafkaListenerEndpointRegistry.class)
@Component
public class KafkaStartupVerifier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KafkaStartupVerifier.class);

    private final ObjectProvider<KafkaListenerEndpointRegistry> listenerRegistryProvider;
    private final String bootstrapServers;

    public KafkaStartupVerifier(
            ObjectProvider<KafkaListenerEndpointRegistry> listenerRegistryProvider,
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers
    ) {
        this.listenerRegistryProvider = listenerRegistryProvider;
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Intentar 3 veces con timeout corto para fail-fast
        int maxAttempts = 3;
        Duration opTimeout = Duration.ofSeconds(3);
        long backoffMs = 1000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (pingKafka(opTimeout)) {
                log.info("Kafka accesible en {}. Arrancando listeners.", bootstrapServers);
                // Iniciar todos los listeners registrados
                KafkaListenerEndpointRegistry listenerRegistry = listenerRegistryProvider.getIfAvailable();
                if (listenerRegistry != null) {
                    listenerRegistry.start();
                } else {
                    log.warn("KafkaListenerEndpointRegistry no disponible; los listeners pueden no iniciarse automáticamente.");
                }
                return;
            }
            log.warn("Intento {}/{}: Kafka no accesible en {}. Reintentando en {} ms...", attempt, maxAttempts, bootstrapServers, backoffMs);
            Thread.sleep(backoffMs);
        }
        String msg = "No se pudo conectar a Kafka en %s después de %d intentos. Abortando arranque.".formatted(bootstrapServers, maxAttempts);
        log.error(msg);
        throw new IllegalStateException(msg);
    }

    private boolean pingKafka(Duration timeout) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (AdminClient admin = AdminClient.create(props)) {
            ListTopicsOptions opts = new ListTopicsOptions().timeoutMs((int) timeout.toMillis());
            admin.listTopics(opts).names().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            log.debug("Fallo ping a Kafka: {}", e.toString());
            return false;
        }
    }
}
