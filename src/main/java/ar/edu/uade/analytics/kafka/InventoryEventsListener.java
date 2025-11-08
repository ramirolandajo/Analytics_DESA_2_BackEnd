package ar.edu.uade.analytics.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ar.edu.uade.analytics.Service.AckService;
import ar.edu.uade.analytics.Service.EventDispatcherService;
import ar.edu.uade.analytics.Service.IdempotencyService;

@ConditionalOnProperty(value = "analytics.kafka.enabled", havingValue = "true")
@Component
public class InventoryEventsListener {
    private static final Logger log = LoggerFactory.getLogger(InventoryEventsListener.class);

    private final ObjectMapper mapper;
    private final EventDispatcherService dispatcher;
    private final IdempotencyService idempotency;
    private final AckService ackService;

    @Value("${analitica.kafka.inventario.topic:inventario}")
    private String topicName;

    public InventoryEventsListener(ObjectMapper mapper,
                                   EventDispatcherService dispatcher,
                                   IdempotencyService idempotency,
                                   AckService ackService) {
        this.mapper = mapper;
        this.dispatcher = dispatcher;
        this.idempotency = idempotency;
        this.ackService = ackService;
    }

    @KafkaListener(topics = "${analitica.kafka.inventario.topic}", concurrency = "${analitica.kafka.inventario.concurrency:1}")
    public void onMessage(ConsumerRecord<String, EventMessage> record) {
        EventMessage msg = record.value();
        if (msg == null) {
            log.warn("[INV] Mensaje no deserializable; se salta. topic={} partition={} offset={}", record.topic(), record.partition(), record.offset());
            return;
        }
        String eventId = msg.eventId;
        String type = msg.getNormalizedEventType();
        try {
            idempotency.registerPending(eventId, type, msg.originModule, record.topic(), record.partition(), record.offset(), msg.payload);
            if (idempotency.alreadyProcessed(eventId)) {
                log.info("[INV] Evento ya procesado id={} type={}", eventId, type);
                ackService.sendAck(eventId);
                return;
            }
            JsonNode payload = msg.unwrapPayload(mapper);
            log.info("[INV] Recibido id={} type={} origin={} topic={} partition={} offset={}", eventId, type, msg.originModule, record.topic(), record.partition(), record.offset());
            dispatcher.handleInventory(type, payload);
            idempotency.markProcessed(eventId);
            ackService.sendAck(eventId);
        } catch (Exception e) {
            log.error("[INV] Error procesando id={} type={}: {}", eventId, type, e.getMessage(), e);
            idempotency.markError(eventId, e.getMessage());
            // No relanzar excepción para evitar reintentos infinitos; el contenedor confirmará el offset del batch
        }
    }
}
