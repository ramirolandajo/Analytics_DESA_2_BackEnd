package ar.edu.uade.analytics.scheduler;

import ar.edu.uade.analytics.Entity.ConsumedEventLog;
import ar.edu.uade.analytics.Repository.ConsumedEventLogRepository;
import ar.edu.uade.analytics.messaging.CoreApiClient;
import ar.edu.uade.analytics.Service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(value = "analytics.ack.enabled", havingValue = "true", matchIfMissing = false)
public class AnalyticsAckRetryScheduler {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsAckRetryScheduler.class);

    private final ConsumedEventLogRepository repo;
    private final CoreApiClient coreApiClient;
    private final IdempotencyService idempotencyService;

    @Value("${inventario.retry.maxAttempts:5}")
    private int maxAttempts;

    public AnalyticsAckRetryScheduler(ConsumedEventLogRepository repo, CoreApiClient coreApiClient, IdempotencyService idempotencyService) {
        this.repo = repo;
        this.coreApiClient = coreApiClient;
        this.idempotencyService = idempotencyService;
    }

    @Scheduled(cron = "${analytics.ack.retry.cron:0 */1 * * * *}")
    public void retryPendingAcks() {
        List<ConsumedEventLog> pending = repo.findPendingAcks(ConsumedEventLog.Status.PROCESSED, maxAttempts);
        if (pending.isEmpty()) return;
        log.info("Reintentando ACK de {} eventos pendientes", pending.size());
        for (ConsumedEventLog cel : pending) {
            String eventId = cel.getEventId();
            CoreApiClient.AckResponse resp = coreApiClient.ackEvent(eventId);
            idempotencyService.markAck(eventId, resp.ok(), resp.error());
        }
    }
}
