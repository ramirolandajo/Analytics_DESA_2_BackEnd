package ar.edu.uade.analytics.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "consumed_event_log", indexes = {
        @Index(name = "idx_event_id", columnList = "event_id", unique = true),
        @Index(name = "idx_status", columnList = "status")
})
public class ConsumedEventLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "origin")
    private String origin;

    @Column(name = "topic")
    private String topic;

    @Column(name = "partition_no")
    private Integer partitionNo;

    @Column(name = "record_offset")
    private Long recordOffset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column
    private Integer attempts;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "ack_sent")
    private Boolean ackSent;

    @Column(name = "ack_attempts")
    private Integer ackAttempts;

    @Column(name = "ack_last_at")
    private OffsetDateTime ackLastAt;

    @Lob
    @Column(name = "ack_last_error")
    private String ackLastError;

    public enum Status { PENDING, PROCESSED, ERROR }
}
