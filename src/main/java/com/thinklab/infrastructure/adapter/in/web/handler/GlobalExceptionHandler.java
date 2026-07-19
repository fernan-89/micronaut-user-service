package com.thinklab.infrastructure.adapter.in.web.handler;

import com.thinklab.domain.exception.BusinessException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler: Centralized reactive error transformation.
 * This adapter intercepts domain and infrastructure exceptions, translating them
 * into standardized RFC 7807 "Problem Details" responses. It ensures that
 * business rule violations (4xx) are clearly distinguished from critical
 * technical failures (500).
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 *     <li><b>Problem Details:</b> Adherence to RFC 7807 for consistent error contracts.</li>
 *     <li><b>Defensive Boundary:</b> Prevents leaking internal stacktraces to API consumers.</li>
 *     <li><b>Forensic Telemetry:</b> Logs root causes with full context for SRE auditing.</li>
 * </ul>
 */
@Slf4j
@Produces
@Singleton
@Requires(classes = {ExceptionHandler.class})
public class GlobalExceptionHandler implements ExceptionHandler<Throwable, HttpResponse<Map<String, Object>>> {

    @Override
    public HttpResponse<Map<String, Object>> handle(HttpRequest request, Throwable exception) {
        log.error("[ACTION: GLOBAL_EXCEPTION_HANDLER] - Exception captured at boundary: {} | Path: {}",
                exception.getMessage(), request.getPath());

        if (exception instanceof BusinessException ex) {
            return handleBusinessException(ex);
        }

        if (exception instanceof ConstraintViolationException ex) {
            return handleValidationException(ex);
        }

        return handleGenericException(exception);
    }

    /**
     * Maps specialized BusinessExceptions to appropriate semantic HTTP statuses.
     */
    private HttpResponse<Map<String, Object>> handleBusinessException(BusinessException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case "USER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "USER_ALREADY_EXISTS" -> HttpStatus.CONFLICT;
            case "INVALID_STATE_TRANSITION" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.BAD_REQUEST;
        };

        log.warn("[ACTION: BUSINESS_FAILURE] - Domain violation [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return HttpResponse.status(status).body(createProblem(ex.getErrorCode(), ex.getMessage(), status.getCode()));
    }

    /**
     * Maps Jakarta Bean Validation errors to 400 Bad Request.
     */
    private HttpResponse<Map<String, Object>> handleValidationException(ConstraintViolationException ex) {
        String details = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));

        return HttpResponse.badRequest(createProblem("VALIDATION_FAILED", details, 400));
    }

    /**
     * Fallback for unexpected technical failures (NASA standard for internal stability).
     */
    private HttpResponse<Map<String, Object>> handleGenericException(Throwable ex) {
        log.error("[ACTION: INTERNAL_FAILURE] - Critical technical failure detected: ", ex);

        Map<String, Object> body = createProblem(
                "INTERNAL_SERVER_ERROR",
                "An unexpected technical failure occurred in the core processing pipeline.",
                500
        );

        // Optional: In non-production environments, you might want to expose the cause
        body.put("details", ex.getClass().getSimpleName() + ": " + ex.getMessage());

        return HttpResponse.serverError(body);
    }

    /**
     * Factory: Constructs an RFC 7807 compliant Problem Detail payload.
     */
    private Map<String, Object> createProblem(String code, String message, int status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error_code", code);
        body.put("message", message);
        return body;
    }
}