package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.ConsumedEventLog;
import ar.edu.uade.analytics.Repository.ConsumedEventLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceRegisterPendingTest {
    @Mock ConsumedEventLogRepository repo;
    @Mock ObjectMapper mapper;
    @Mock MeterRegistry meterRegistry;

    IdempotencyService svc;

    @BeforeEach
    void setUp() {
        svc = new IdempotencyService(repo, mapper, meterRegistry);
    }

    @Test
    void registerPending_createsEffectiveId_and_incrementsAttempts() {
        when(repo.findByEventId("topic:-1:-1")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConsumedEventLog res = svc.registerPending(null, "t", "o", "topic", null, null, "payload");
        assertEquals("topic:-1:-1", res.getEventId());
        assertEquals(1, res.getAttempts());
    }
}
