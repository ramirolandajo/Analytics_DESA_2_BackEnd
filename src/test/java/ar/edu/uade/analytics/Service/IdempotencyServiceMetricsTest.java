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

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceMetricsTest {

    @Mock ConsumedEventLogRepository repo;
    @Mock ObjectMapper mapper;
    @Mock MeterRegistry meterRegistry;
    @Mock Counter counter;

    IdempotencyService svc;

    @BeforeEach
    void setUp() {
        svc = new IdempotencyService(repo, mapper, meterRegistry);
        when(meterRegistry.counter("analytics.events.processed")).thenReturn(counter);
        when(meterRegistry.counter("analytics.events.errors")).thenReturn(counter);
    }

    @Test
    void markProcessed_and_markError_incrementCounters() {
        ConsumedEventLog log = new ConsumedEventLog(); log.setEventId("e1");
        when(repo.findByEventId("e1")).thenReturn(Optional.of(log));

        svc.markProcessed("e1");
        svc.markError("e1", "boom");

        verify(repo, times(2)).findByEventId("e1");
        verify(counter, atLeast(1)).increment();
    }
}

