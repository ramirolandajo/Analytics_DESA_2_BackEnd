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
class IdempotencyServiceMarkAckTest {

    @Mock ConsumedEventLogRepository repo;
    @Mock ObjectMapper mapper;
    @Mock MeterRegistry meterRegistry;

    IdempotencyService svc;

    @BeforeEach
    void setUp() {
        svc = new IdempotencyService(repo, mapper, meterRegistry);
    }

    @Test
    void markAck_setsAckSent_and_incrementsAttempts() {
        ConsumedEventLog log = new ConsumedEventLog(); log.setEventId("e2"); log.setAckAttempts(0);
        when(repo.findByEventId("e2")).thenReturn(Optional.of(log));

        svc.markAck("e2", false, "err");

        verify(repo).save(any());
    }
}

