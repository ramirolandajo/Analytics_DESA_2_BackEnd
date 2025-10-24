package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.ConsumedEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ConsumedEventLogRepository extends JpaRepository<ConsumedEventLog, Long> {
    Optional<ConsumedEventLog> findByEventId(String eventId);

    @Query("select c from ConsumedEventLog c where c.status = :status and (c.ackSent is null or c.ackSent = false) and (c.ackAttempts is null or c.ackAttempts < :max) order by c.updatedAt desc")
    List<ConsumedEventLog> findPendingAcks(@Param("status") ConsumedEventLog.Status status, @Param("max") Integer max);

    // Eventos por tipo (contiene) y estado, ordenados por fecha procesada asc
    List<ConsumedEventLog> findByStatusAndEventTypeContainingIgnoreCaseOrderByProcessedAtAsc(ConsumedEventLog.Status status, String eventTypePart);

    // Mismo filtro con rango temporal
    List<ConsumedEventLog> findByStatusAndEventTypeContainingIgnoreCaseAndProcessedAtBetweenOrderByProcessedAtAsc(
            ConsumedEventLog.Status status,
            String eventTypePart,
            OffsetDateTime start,
            OffsetDateTime end
    );
}
