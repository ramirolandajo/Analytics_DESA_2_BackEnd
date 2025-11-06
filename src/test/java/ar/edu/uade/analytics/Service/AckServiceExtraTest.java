package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.messaging.CoreApiClient;
import ar.edu.uade.analytics.messaging.CoreApiClient.AckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AckServiceExtraTest {
    @Mock CoreApiClient coreApiClient;
    @Mock IdempotencyService idempotencyService;
    AckService ackService;

    @BeforeEach
    void setUp() throws Exception {
        // create service first
        ackService = new AckService(coreApiClient, idempotencyService);
        // When created by new AckService, @Value fields not injected; set them on the instance
        Field fAck = AckService.class.getDeclaredField("ackEnabled"); fAck.setAccessible(true); fAck.setBoolean(ackService, true);
        Field fRetries = AckService.class.getDeclaredField("immediateRetries"); fRetries.setAccessible(true); fRetries.setInt(ackService, 1);
        Field fBackoff = AckService.class.getDeclaredField("backoffBaseMs"); fBackoff.setAccessible(true); fBackoff.setLong(ackService, 0L);
        // ensure findByEventId returns Optional.empty()
        when(idempotencyservice().findByEventId(anyString())).thenReturn(Optional.empty());
    }

    // helper to return the mock idempotencyService (keeps earlier code style)
    private IdempotencyService idempotencyservice() { return idempotencyService; }

    @Test
    void sendAck_whenCoreReturnsOk_marksAckTrue() {
        AckResponse resp = mock(AckResponse.class);
        when(resp.ok()).thenReturn(true);
        when(coreApiClient.ackEvent(eq("evt-ok"), any(OffsetDateTime.class))).thenReturn(resp);

        ackService.sendAck("evt-ok");

        verify(coreApiClient).ackEvent(eq("evt-ok"), any(OffsetDateTime.class));
        verify(idempotencyService, atMostOnce()).markAck(eq("evt-ok"), eq(true), isNull());
    }

    @Test
    void sendAck_whenCoreReturnsFalse_marksAckFalse_withErrorFromResp() {
        AckResponse resp = mock(AckResponse.class);
        when(resp.ok()).thenReturn(false);
        when(resp.error()).thenReturn("boom");
        when(coreApiClient.ackEvent(eq("evt-retry"), any(OffsetDateTime.class))).thenReturn(resp);

        ackService.sendAck("evt-retry");

        verify(coreApiClient).ackEvent(eq("evt-retry"), any(OffsetDateTime.class));
        verify(idempotencyService, atMostOnce()).markAck(eq("evt-retry"), eq(false), eq("boom"));
    }

    @Test
    void sendAck_whenCoreReturnsNull_marksAckFalse_withUnknown() {
        when(coreApiClient.ackEvent(eq("evt-null"), any(OffsetDateTime.class))).thenReturn(null);

        ackService.sendAck("evt-null");

        verify(coreApiClient).ackEvent(eq("evt-null"), any(OffsetDateTime.class));
        verify(idempotencyService, atMostOnce()).markAck(eq("evt-null"), eq(false), eq("unknown"));
    }
}
