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

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyRegisterPendingSequenceTest {

    @Mock ConsumedEventLogRepository repo;
    @Mock ObjectMapper mapper;
    @Mock MeterRegistry meterRegistry;

    IdempotencyService svc;

    @BeforeEach
    void setUp() {
        svc = new IdempotencyService(repo, mapper, meterRegistry);
    }

    @Test
    void registerPending_handlesNullEventId_and_incrementsAttempts() {
        when(repo.findByEventId(anyString())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConsumedEventLog saved = svc.registerPending(null, "t", "o", "topicA", 2, 100L, Map.of("x",1));
        assertNotNull(saved.getEventId());
        assertEquals(1, saved.getAttempts());
        // second call increments attempts
        when(repo.findByEventId(saved.getEventId())).thenReturn(Optional.of(saved));
        ConsumedEventLog saved2 = svc.registerPending(saved.getEventId(), "t", "o", "topicA", 2, 100L, Map.of("x",1));
        assertEquals(2, saved2.getAttempts());
    }
}

