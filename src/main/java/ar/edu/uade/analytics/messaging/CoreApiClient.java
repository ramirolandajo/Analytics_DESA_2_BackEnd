package ar.edu.uade.analytics.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Collections;

@Component
public class CoreApiClient {
    private static final Logger log = LoggerFactory.getLogger(CoreApiClient.class);

    public record AckResponse(boolean ok, String error) {}

    private final RestTemplate restTemplate;

    @Value("${communication.intermediary.url:http://localhost:8090}")
    private String baseUrl;

    public CoreApiClient(BearerTokenInterceptor interceptor,
                         @Value("${http.client.connect-timeout-ms:5000}") int connectTimeoutMs,
                         @Value("${http.client.read-timeout-ms:10000}") int readTimeoutMs,
                         @Value("${analytics.ack.auth.enabled:false}") boolean authEnabled) {
        SimpleClientHttpRequestFactory reqFactory = new SimpleClientHttpRequestFactory();
        reqFactory.setConnectTimeout(connectTimeoutMs);
        reqFactory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(reqFactory));
        if (authEnabled) {
            this.restTemplate.setInterceptors(Collections.singletonList(interceptor));
        }
    }

    public AckResponse ackEvent(String eventId) {
        return ackEvent(eventId, null);
    }

    public AckResponse ackEvent(String eventId, OffsetDateTime consumedAt) {
        String url = String.format("%s/events/%s/ack", baseUrl, eventId);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = consumedAt == null
                    ? "{}"
                    : String.format("{\"consumedAt\":\"%s\"}", consumedAt.toString());
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            var response = restTemplate.postForEntity(url, entity, String.class);
            boolean ok = response.getStatusCode().is2xxSuccessful();
            if (ok) {
                log.info("ACK HTTP 2xx enviado a {}", url);
                return new AckResponse(true, null);
            } else {
                String msg = "HTTP " + response.getStatusCode();
                log.warn("ACK HTTP no-2xx {} para {}", response.getStatusCode(), url);
                return new AckResponse(false, msg);
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            log.warn("Fallo ACK HTTP a {}: {}", url, msg);
            return new AckResponse(false, msg);
        }
    }
}
