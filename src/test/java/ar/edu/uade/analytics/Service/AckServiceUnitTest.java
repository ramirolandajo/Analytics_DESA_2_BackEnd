package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.messaging.CoreApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AckServiceUnitTest {
    @Mock
    CoreApiClient coreApiClient;
    @Mock
    IdempotencyService idempotencyService;

    AckService svc;

    @BeforeEach
    void setUp() {
        svc = new AckService(coreApiClient, idempotencyService);
        // habilitar ack y reducir retries/backoff para tests
        setField(svc, "ackEnabled", true);
        setField(svc, "immediateRetries", 1);
        setField(svc, "backoffBaseMs", 1L);
    }

    static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendAck_whenCoreReturnsOk_marksAckTrue() {
        String eventId = "evt-1";
        CoreApiClient.AckResponse resp = new CoreApiClient.AckResponse(true, null);
        when(coreApiClient.ackEvent(eq(eventId), any(OffsetDateTime.class))).thenReturn(resp);
        when(idempotencyService.findByEventId(eventId)).thenReturn(Optional.empty());

        svc.sendAck(eventId);

        verify(coreApiClient).ackEvent(eq(eventId), any(OffsetDateTime.class));
        verify(idempotencyService).markAck(eq(eventId), eq(true), isNull());
    }

    @Test
    void sendAck_whenCoreThrows_marksAckFalseWithUnknown() {
        String eventId = "evt-2";
        when(coreApiClient.ackEvent(eq(eventId), any(OffsetDateTime.class))).thenThrow(new RuntimeException("boom"));
        when(idempotencyService.findByEventId(eventId)).thenReturn(Optional.empty());

        svc.sendAck(eventId);

        verify(coreApiClient).ackEvent(eq(eventId), any(OffsetDateTime.class));
        verify(idempotencyService).markAck(eq(eventId), eq(false), eq("unknown"));
    }
}
