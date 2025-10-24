package ar.edu.uade.analytics.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<Map<String, Object>> buildBody(HttpServletRequest req, HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", req != null ? req.getRequestURI() : null);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(NullPointerException ex, HttpServletRequest req) {
        log.error("Handled NPE on {} {}: {}", req != null ? req.getMethod() : "?", req != null ? req.getRequestURI() : "?", ex.getMessage(), ex);
        return buildBody(req, HttpStatus.INTERNAL_SERVER_ERROR, "NullPointerException", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Bad request on {} {}: {}", req != null ? req.getMethod() : "?", req != null ? req.getRequestURI() : "?", ex.getMessage());
        return buildBody(req, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, HttpServletRequest req) {
        log.error("Unhandled runtime on {} {}: {}", req != null ? req.getMethod() : "?", req != null ? req.getRequestURI() : "?", ex.getMessage(), ex);
        return buildBody(req, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage());
    }
}
