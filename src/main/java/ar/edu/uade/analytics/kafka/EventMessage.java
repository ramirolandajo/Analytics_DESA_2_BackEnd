package ar.edu.uade.analytics.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventMessage {
    public String eventId;
    public String eventType;
    public Object payload;
    public String originModule;
    public Object timestamp;

    public OffsetDateTime getTimestampAsOffsetDateTime() {
        if (timestamp == null) return OffsetDateTime.now(ZoneOffset.UTC);
        try {
            if (timestamp instanceof Number num) {
                double epochSeconds = num.doubleValue();
                long seconds = (long) epochSeconds;
                long nanos = (long) ((epochSeconds - seconds) * 1_000_000_000L);
                return OffsetDateTime.ofInstant(Instant.ofEpochSecond(seconds, nanos), ZoneOffset.UTC);
            }
            String ts = String.valueOf(timestamp);
            try {
                return OffsetDateTime.parse(ts);
            } catch (Exception ignore) {
                // Fallback: parse as Instant
                return OffsetDateTime.ofInstant(Instant.parse(ts), ZoneOffset.UTC);
            }
        } catch (Exception e) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public String getNormalizedEventType() {
        if (eventType == null) return "";
        String lower = eventType.toLowerCase();
        String noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.trim().replaceAll("\\s+", " ");
    }

    public JsonNode unwrapPayload(ObjectMapper mapper) {
        try {
            JsonNode node = mapper.valueToTree(payload);
            int guard = 0;
            while (node != null && node.has("payload") && node.get("payload").isObject() && guard < 3) {
                node = node.get("payload");
                guard++;
            }
            return node;
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }
}
