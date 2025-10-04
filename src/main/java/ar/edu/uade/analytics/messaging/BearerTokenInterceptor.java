package ar.edu.uade.analytics.messaging;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BearerTokenInterceptor implements ClientHttpRequestInterceptor {

    private final BackendTokenManager tokenManager;

    public BearerTokenInterceptor(BackendTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String token = tokenManager.getAccessToken();
        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return execution.execute(request, body);
    }
}
