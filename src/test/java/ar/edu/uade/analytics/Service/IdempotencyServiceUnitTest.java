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

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceUnitTest {
    @Mock ConsumedEventLogRepository repo;
    @Mock ObjectMapper mapper;
    @Mock MeterRegistry meterRegistry;

    IdempotencyService svc;

    @BeforeEach
    void setUp() {
        svc = new IdempotencyService(repo, mapper, meterRegistry);
    }

    @Test
    void markAck_setsAckSentAndError() {
        ConsumedEventLog log = new ConsumedEventLog(); log.setEventId("e1"); log.setAckAttempts(0);
        when(repo.findByEventId("e1")).thenReturn(Optional.of(log));

        svc.markAck("e1", false, "boom");

        verify(repo).save(any());
        assertEquals(1, log.getAckAttempts());
        assertEquals("boom", log.getAckLastError());
    }

    @Test
    void markAck_success_clearsError_and_incrementsAttempts() {
        ConsumedEventLog log = new ConsumedEventLog(); log.setEventId("e2"); log.setAckAttempts(1); log.setAckLastError("old");
        when(repo.findByEventId("e2")).thenReturn(Optional.of(log));

        svc.markAck("e2", true);

        verify(repo).save(any());
        assertNull(log.getAckLastError());
        assertEquals(2, log.getAckAttempts());
    }
}

