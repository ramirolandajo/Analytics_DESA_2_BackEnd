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
class AckServiceRetryTest {
    @Mock CoreApiClient coreApiClient;
    @Mock IdempotencyService idempotencyService;

    AckService svc;

    @BeforeEach
    void setUp() {
        svc = new AckService(coreApiClient, idempotencyService);
        // enable ack, set retries to 2
        setField(svc, "ackEnabled", true);
        setField(svc, "immediateRetries", 2);
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
    void sendAck_retriesUntilOk_and_marksAckTrue() {
        String eventId = "evt-retry";
        CoreApiClient.AckResponse first = new CoreApiClient.AckResponse(false, "err");
        CoreApiClient.AckResponse second = new CoreApiClient.AckResponse(true, null);

        when(coreApiClient.ackEvent(eq(eventId), any(OffsetDateTime.class))).thenReturn(first).thenReturn(second);
        when(idempotencyService.findByEventId(eventId)).thenReturn(Optional.empty());

        svc.sendAck(eventId);

        verify(coreApiClient, times(2)).ackEvent(eq(eventId), any(OffsetDateTime.class));
        verify(idempotencyService).markAck(eq(eventId), eq(true), isNull());
    }

    @Test
    void sendAck_allRetriesFail_marksAckFalseWithLastError() {
        String eventId = "evt-fail";
        CoreApiClient.AckResponse r1 = new CoreApiClient.AckResponse(false, "boom1");
        CoreApiClient.AckResponse r2 = new CoreApiClient.AckResponse(false, "boom2");
        when(coreApiClient.ackEvent(eq(eventId), any(OffsetDateTime.class))).thenReturn(r1).thenReturn(r2);
        when(idempotencyService.findByEventId(eventId)).thenReturn(Optional.empty());

        svc.sendAck(eventId);

        verify(coreApiClient, times(2)).ackEvent(eq(eventId), any(OffsetDateTime.class));
        verify(idempotencyService).markAck(eq(eventId), eq(false), eq("boom2"));
    }
}

