package ar.edu.uade.analytics.kafka;

import ar.edu.uade.analytics.Entity.ConsumedEventLog;
import ar.edu.uade.analytics.Repository.ConsumedEventLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class IdempotencyService {
    private final ConsumedEventLogRepository repo;
    private final ObjectMapper mapper;
    private final MeterRegistry meterRegistry;
    // cache simple en memoria para eventos recientes
    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    public IdempotencyService(ConsumedEventLogRepository repo, ObjectMapper mapper, MeterRegistry meterRegistry) {
        this.repo = repo;
        this.mapper = mapper;
        this.meterRegistry = meterRegistry;
    }

    public Optional<ConsumedEventLog> findByEventId(String eventId) {
        return repo.findByEventId(eventId);
    }

    public boolean alreadyProcessed(String eventId) {
        if (eventId == null || eventId.isBlank()) return false; // sin id, no puede ser idempotente
        if (Boolean.TRUE.equals(cache.get(eventId))) return true;
        return repo.findByEventId(eventId)
                .map(log -> log.getStatus() == ConsumedEventLog.Status.PROCESSED)
                .orElse(false);
    }

    public ConsumedEventLog registerPending(String eventId, String eventType, String origin, String topic, Integer partition, Long offset, Object payload) {
        String effectiveId = (eventId == null || eventId.isBlank())
                ? String.format("%s:%d:%d", topic, partition == null ? -1 : partition, offset == null ? -1L : offset)
                : eventId;
        ConsumedEventLog log = repo.findByEventId(effectiveId).orElseGet(ConsumedEventLog::new);
        log.setEventId(effectiveId);
        log.setEventType(eventType);
        log.setOrigin(origin);
        log.setTopic(topic);
        log.setPartitionNo(partition);
        log.setRecordOffset(offset);
        log.setStatus(ConsumedEventLog.Status.PENDING);
        log.setAttempts(Optional.ofNullable(log.getAttempts()).orElse(0) + 1);
        try {
            log.setPayloadJson(mapper.writeValueAsString(payload));
        } catch (Exception ignore) {}
        log.setUpdatedAt(OffsetDateTime.now());
        return repo.save(log);
    }

    public void markProcessed(String eventId) {
        cache.put(eventId, true);
        repo.findByEventId(eventId).ifPresent(log -> {
            log.setStatus(ConsumedEventLog.Status.PROCESSED);
            log.setProcessedAt(OffsetDateTime.now());
            log.setUpdatedAt(OffsetDateTime.now());
            repo.save(log);
        });
        meterRegistry.counter("analytics.events.processed").increment();
    }

    public void markError(String eventId, String error) {
        repo.findByEventId(eventId).ifPresent(log -> {
            log.setStatus(ConsumedEventLog.Status.ERROR);
            log.setUpdatedAt(OffsetDateTime.now());
            log.setAckLastError(error);
            repo.save(log);
        });
        meterRegistry.counter("analytics.events.errors").increment();
    }

    public void markAck(String eventId, boolean success) {
        markAck(eventId, success, null);
    }

    public void markAck(String eventId, boolean success, String error) {
        repo.findByEventId(eventId).ifPresent(log -> {
            log.setAckSent(success);
            log.setAckAttempts(Optional.ofNullable(log.getAckAttempts()).orElse(0) + 1);
            log.setAckLastAt(OffsetDateTime.now());
            if (!success && error != null) {
                log.setAckLastError(error);
            } else if (success) {
                log.setAckLastError(null);
            }
            repo.save(log);
        });
    }
}
