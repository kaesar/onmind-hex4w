package co.onmind.hex4w.transverse.exceptions;

import co.onmind.hex4w.domain.exceptions.DuplicateRoleException;
import co.onmind.hex4w.domain.exceptions.RoleNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Global error handler for reactive web applications.
 * 
 * This component provides centralized error handling for all unhandled exceptions
 * that occur during reactive request processing. It implements the ErrorWebExceptionHandler
 * interface to intercept exceptions and convert them into appropriate HTTP responses.
 * 
 * <p>The handler follows these principles:</p>
 * <ul>
 *   <li>Maps domain exceptions to appropriate HTTP status codes</li>
 *   <li>Provides consistent error response format</li>
 *   <li>Logs errors for monitoring and debugging</li>
 *   <li>Handles both expected business exceptions and unexpected system errors</li>
 *   <li>Maintains reactive processing throughout error handling</li>
 * </ul>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
@Order(-2) // Higher precedence than default error handler
public class GlobalErrorHandler implements ErrorWebExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalErrorHandler.class);
    
    private final ObjectMapper objectMapper;
    
    /**
     * Constructs a new GlobalErrorHandler with the required dependencies.
     * 
     * @param objectMapper the JSON object mapper for serializing error responses
     */
    public GlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handles exceptions that occur during reactive request processing.
     * 
     * This method intercepts unhandled exceptions and converts them into
     * appropriate HTTP error responses. It maps different exception types
     * to their corresponding HTTP status codes and provides consistent
     * error response formatting.
     * 
     * <p>Exception mapping:</p>
     * <ul>
     *   <li>DuplicateRoleException → 409 CONFLICT</li>
     *   <li>RoleNotFoundException → 404 NOT_FOUND</li>
     *   <li>IllegalArgumentException → 400 BAD_REQUEST</li>
     *   <li>JsonProcessingException → 400 BAD_REQUEST</li>
     *   <li>All other exceptions → 500 INTERNAL_SERVER_ERROR</li>
     * </ul>
     * 
     * @param exchange the server web exchange containing request and response
     * @param ex the exception that occurred
     * @return a Mono that completes when the error response has been written
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        // Log the error for monitoring and debugging
        logError(exchange, ex);
        
        // Determine the appropriate HTTP status and error details
        ErrorDetails errorDetails = mapExceptionToErrorDetails(ex);
        
        // Set response headers
        response.setStatusCode(errorDetails.status());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // Create error response
        ErrorResponse errorResponse = new ErrorResponse(
            errorDetails.code(),
            errorDetails.message(),
            errorDetails.status().value(),
            LocalDateTime.now(),
            exchange.getRequest().getPath().value()
        );
        
        // Write error response
        return writeErrorResponse(response, errorResponse);
    }
    
    /**
     * Maps exceptions to error details including status code, error code, and message.
     * 
     * @param ex the exception to map
     * @return error details for the exception
     */
    private ErrorDetails mapExceptionToErrorDetails(Throwable ex) {
        return switch (ex) {
            case DuplicateRoleException duplicateEx -> new ErrorDetails(
                HttpStatus.CONFLICT,
                "DUPLICATE_ROLE",
                duplicateEx.getMessage()
            );
            case RoleNotFoundException notFoundEx -> new ErrorDetails(
                HttpStatus.NOT_FOUND,
                "ROLE_NOT_FOUND",
                notFoundEx.getMessage()
            );
            case IllegalArgumentException illegalArgEx -> new ErrorDetails(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                illegalArgEx.getMessage()
            );
            case JsonProcessingException jsonEx -> new ErrorDetails(
                HttpStatus.BAD_REQUEST,
                "INVALID_JSON",
                "Invalid JSON format in request body"
            );
            default -> new ErrorDetails(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred"
            );
        };
    }
    
    /**
     * Writes the error response to the HTTP response body.
     * 
     * @param response the server HTTP response
     * @param errorResponse the error response to write
     * @return a Mono that completes when the response has been written
     */
    private Mono<Void> writeErrorResponse(ServerHttpResponse response, ErrorResponse errorResponse) {
        try {
            byte[] responseBytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(responseBytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize error response", e);
            // Fallback to simple error message
            String fallbackMessage = "{\"code\":\"SERIALIZATION_ERROR\",\"message\":\"Failed to serialize error response\"}";
            DataBuffer buffer = response.bufferFactory().wrap(fallbackMessage.getBytes());
            return response.writeWith(Mono.just(buffer));
        }
    }
    
    /**
     * Logs error information for monitoring and debugging purposes.
     * 
     * @param exchange the server web exchange
     * @param ex the exception that occurred
     */
    private void logError(ServerWebExchange exchange, Throwable ex) {
        String requestPath = exchange.getRequest().getPath().value();
        String requestMethod = exchange.getRequest().getMethod().name();
        
        if (ex instanceof DuplicateRoleException || ex instanceof RoleNotFoundException) {
            // Business exceptions - log as info level
            logger.info("Business exception occurred: {} {} - {}", 
                requestMethod, requestPath, ex.getMessage());
        } else if (ex instanceof IllegalArgumentException) {
            // Validation exceptions - log as warn level
            logger.warn("Validation exception occurred: {} {} - {}", 
                requestMethod, requestPath, ex.getMessage());
        } else {
            // System exceptions - log as error level with stack trace
            logger.error("Unexpected exception occurred: {} {} - {}", 
                requestMethod, requestPath, ex.getMessage(), ex);
        }
    }
    
    /**
     * Record representing error details for exception mapping.
     * 
     * @param status the HTTP status code
     * @param code the application-specific error code
     * @param message the error message
     */
    private record ErrorDetails(
        HttpStatus status,
        String code,
        String message
    ) {}
    
    /**
     * Record representing the standardized error response structure.
     * 
     * This record defines the consistent format for all error responses
     * returned by the application, providing clients with structured
     * error information for proper error handling.
     * 
     * @param code the application-specific error code
     * @param message the human-readable error message
     * @param status the HTTP status code
     * @param timestamp the timestamp when the error occurred
     * @param path the request path where the error occurred
     */
    public record ErrorResponse(
        String code,
        String message,
        int status,
        LocalDateTime timestamp,
        String path
    ) {}
}