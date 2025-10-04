package ar.edu.uade.analytics.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class BackendTokenManager {
    private static final Logger log = LoggerFactory.getLogger(BackendTokenManager.class);

    @Value("${keycloak.token.url:http://localhost:8081/realms/master/protocol/openid-connect/token}")
    private String tokenUrl;

    @Value("${keycloak.client-id:analytics}")
    private String clientId;

    @Value("${keycloak.client-secret:secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
            return cachedToken;
        }
        lock.lock();
        try {
            if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
                return cachedToken;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
            ResponseEntity<Map> resp = restTemplate.postForEntity(tokenUrl, request, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalStateException("No token response");
            }
            Map body = resp.getBody();
            this.cachedToken = String.valueOf(body.get("access_token"));
            Number expiresIn = (Number) body.getOrDefault("expires_in", 300);
            this.expiresAt = Instant.now().plusSeconds(expiresIn.longValue());
            return cachedToken;
        } catch (Exception e) {
            log.warn("Error obteniendo token OAuth2: {}", e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }
}

