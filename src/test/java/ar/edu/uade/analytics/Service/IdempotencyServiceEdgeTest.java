//package ar.edu.uade.analytics.Service;
//
//import ar.edu.uade.analytics.Entity.ConsumedEventLog;
//import ar.edu.uade.analytics.Repository.ConsumedEventLogRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.micrometer.core.instrument.Counter;
//import io.micrometer.core.instrument.MeterRegistry;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Captor;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Optional;
//
//import static org.mockito.Mockito.*;
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
//class IdempotencyServiceEdgeTest {
//    @Mock ConsumedEventLogRepository repo;
//    @Mock ObjectMapper mapper;
//    @Mock MeterRegistry meterRegistry;
//    @Mock Counter counter;
//
//    IdempotencyService svc;
//
//    @Captor ArgumentCaptor<ConsumedEventLog> captor;
//
//    @BeforeEach
//    void setUp() {
//        when(meterRegistry.counter(anyString())).thenReturn(counter);
//        svc = new IdempotencyService(repo, mapper, meterRegistry);
//    }
//
//    @Test
//    void registerPending_whenEventIdNull_usesTopicPartitionOffsetAsId_and_saves() {
//        when(repo.findByEventId(anyString())).thenReturn(Optional.empty());
//        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
//
//        Object payload = new Object();
//        ConsumedEventLog saved = svc.registerPending(null, "t", "o", "topicA", 5, 100L, payload);
//
//        assertNotNull(saved.getEventId());
//        assertTrue(saved.getEventId().startsWith("topicA:"));
//        verify(repo).save(captor.capture());
//        assertEquals(ConsumedEventLog.Status.PENDING, captor.getValue().getStatus());
//        assertEquals(1, captor.getValue().getAttempts());
//    }
//
//    @Test
//    void alreadyProcessed_handlesBlankAndNullIds() {
//        assertFalse(svc.alreadyProcessed(null));
//        assertFalse(svc.alreadyProcessed(""));
//        assertFalse(svc.alreadyProcessed("   "));
//        ConsumedEventLog l = new ConsumedEventLog(); l.setEventId("e1"); l.setStatus(ConsumedEventLog.Status.PROCESSED);
//        when(repo.findByEventId("e1")).thenReturn(Optional.of(l));
//        assertTrue(svc.alreadyProcessed("e1"));
//    }
//}
//
