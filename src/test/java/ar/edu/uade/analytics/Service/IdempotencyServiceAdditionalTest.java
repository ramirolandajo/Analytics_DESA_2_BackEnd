package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.ConsumedEventLog;
import ar.edu.uade.analytics.Repository.ConsumedEventLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IdempotencyServiceAdditionalTest {
    @Mock ConsumedEventLogRepository repo;
    @Mock MeterRegistry meterRegistry;
    @Mock Counter counter;
    IdempotencyService service;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        service = new IdempotencyService(repo, new ObjectMapper(), meterRegistry);
    }

    @Test
    void alreadyProcessed_checksCacheAndRepo() {
        ConsumedEventLog log = new ConsumedEventLog();
        log.setEventId("evt-1");
        log.setStatus(ConsumedEventLog.Status.PROCESSED);
        when(repo.findByEventId("evt-1")).thenReturn(Optional.of(log));
        assertTrue(service.alreadyProcessed("evt-1"));
    }

    @Test
    void markProcessed_marksAndIncrements() {
        ConsumedEventLog log = new ConsumedEventLog(); log.setEventId("evt-2");
        when(repo.findByEventId("evt-2")).thenReturn(Optional.of(log));
        service.markProcessed("evt-2");
        verify(repo).save(any(ConsumedEventLog.class));
        verify(counter, atLeastOnce()).increment();
    }

    @Test
    void markError_marksAndIncrementsError() {
        ConsumedEventLog log = new ConsumedEventLog(); log.setEventId("evt-3");
        when(repo.findByEventId("evt-3")).thenReturn(Optional.of(log));
        service.markError("evt-3", "boom");
        verify(repo).save(any(ConsumedEventLog.class));
        verify(counter, atLeastOnce()).increment();
    }
}
