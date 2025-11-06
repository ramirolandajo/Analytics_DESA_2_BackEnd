package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.messaging.CoreApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AckService {
    private static final Logger log = LoggerFactory.getLogger(AckService.class);

    @Value("${analytics.ack.enabled:true}")
    private boolean ackEnabled;

    @Value("${analytics.ack.immediate-retries:3}")
    private int immediateRetries;

    @Value("${analytics.ack.backoff-ms:1000}")
    private long backoffBaseMs;

    private final CoreApiClient coreApiClient;
    private final IdempotencyService idempotencyService;

    public AckService(CoreApiClient coreApiClient, IdempotencyService idempotencyService) {
        this.coreApiClient = coreApiClient;
        this.idempotencyService = idempotencyService;
    }

    public void sendAck(String eventId) {
        if (!ackEnabled || eventId == null || eventId.isBlank()) return;
        // Usar processedAt si lo tenemos; si no, ahora
        OffsetDateTime consumedAt = idempotencyService.findByEventId(eventId)
                .map(cel -> cel.getProcessedAt())
                .orElse(OffsetDateTime.now());

        CoreApiClient.AckResponse resp = null;
        boolean success = false;
        for (int attempt = 1; attempt <= Math.max(1, immediateRetries); attempt++) {
            try {
                resp = coreApiClient.ackEvent(eventId, consumedAt);
            } catch (Exception e) {
                // defensa: si el cliente lanza excepción, lo tratamos como fallo y seguimos reintentando
                log.debug("coreApiClient.ackEvent threw an exception on attempt {}: {}", attempt, e.getMessage());
                resp = null;
            }
            if (resp != null && resp.ok()) {
                success = true;
                break;
            }
            // calcular backoff 1x, 3x, 5x base
            long delay = switch (attempt) {
                case 1 -> backoffBaseMs;     // 1s
                case 2 -> backoffBaseMs * 3; // 3s
                default -> backoffBaseMs * 5; // 5s
            };
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        idempotencyService.markAck(eventId, success, success ? null : (resp != null ? resp.error() : "unknown"));
        if (!success) {
            log.warn("ACK pendiente de reintento para eventId={}", eventId);
        }
    }
}
