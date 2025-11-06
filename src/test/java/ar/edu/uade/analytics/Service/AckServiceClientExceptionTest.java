package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.messaging.CoreApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AckServiceClientExceptionTest {

    @Mock CoreApiClient coreApiClient;
    @Mock IdempotencyService idempotencyService;

    AckService svc;

    @BeforeEach
    void setUp() throws Exception {
        svc = new AckService(coreApiClient, idempotencyService);
        var f = AckService.class.getDeclaredField("ackEnabled"); f.setAccessible(true); f.setBoolean(svc, true);
        var r = AckService.class.getDeclaredField("immediateRetries"); r.setAccessible(true); r.setInt(svc, 2);
        var b = AckService.class.getDeclaredField("backoffBaseMs"); b.setAccessible(true); b.setLong(svc, 1L);
    }

    @Test
    void sendAck_whenClientThrows_retries_and_marksFalse() {
        String eid = "evt-throw";
        when(idempotencyService.findByEventId(eid)).thenReturn(Optional.empty());
        when(coreApiClient.ackEvent(eq(eid), any())).thenThrow(new RuntimeException("boom"));

        svc.sendAck(eid);

        verify(coreApiClient, atLeastOnce()).ackEvent(eq(eid), any());
        verify(idempotencyService).markAck(eq(eid), eq(false), anyString());
    }
}

