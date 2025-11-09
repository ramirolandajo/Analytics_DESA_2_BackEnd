package ar.edu.uade.analytics.Entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ConsumedEventLogTest {

    @Test
    void enumStatusAndDefaults() {
        ConsumedEventLog l = new ConsumedEventLog();
        l.setStatus(ConsumedEventLog.Status.PENDING);
        assertEquals(ConsumedEventLog.Status.PENDING, l.getStatus());
    }

    @Test
    void settersAndTimestamps() {
        ConsumedEventLog l = new ConsumedEventLog();
        l.setEventId("evt-1");
        l.setEventType("TYPE");
        l.setOrigin("ORIG");
        l.setTopic("TOP");
        l.setPartitionNo(1);
        l.setRecordOffset(100L);
        l.setAttempts(2);
        l.setPayloadJson("{}");
        OffsetDateTime now = OffsetDateTime.now();
        l.setProcessedAt(now);
        l.setUpdatedAt(now);
        l.setAckSent(true);
        l.setAckAttempts(1);
        l.setAckLastAt(now);
        l.setAckLastError("err");

        assertEquals("evt-1", l.getEventId());
        assertEquals("TYPE", l.getEventType());
        assertEquals("ORIG", l.getOrigin());
        assertEquals("TOP", l.getTopic());
        assertEquals(1, l.getPartitionNo());
        assertEquals(100L, l.getRecordOffset());
        assertEquals(2, l.getAttempts());
        assertEquals("{}", l.getPayloadJson());
        assertEquals(now, l.getProcessedAt());
        assertEquals(now, l.getUpdatedAt());
        assertTrue(l.getAckSent());
        assertEquals(1, l.getAckAttempts());
        assertEquals(now, l.getAckLastAt());
        assertEquals("err", l.getAckLastError());
    }
}

