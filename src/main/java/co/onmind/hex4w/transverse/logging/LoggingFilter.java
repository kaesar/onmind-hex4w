package co.onmind.hex4w.transverse.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Reactive web filter for request/response logging and tracing.
 * 
 * This filter provides comprehensive logging capabilities for reactive web requests,
 * including request tracing, performance monitoring, and structured logging.
 * It operates in a non-blocking manner, maintaining the reactive nature of the application.
 * 
 * <p>Features provided:</p>
 * <ul>
 *   <li>Request/response logging with timing information</li>
 *   <li>Unique request ID generation for tracing</li>
 *   <li>MDC (Mapped Diagnostic Context) support for structured logging</li>
 *   <li>Performance metrics collection</li>
 *   <li>Error logging integration</li>
 * </ul>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
@Order(1) // Execute early in the filter chain
public class LoggingFilter implements WebFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String START_TIME_KEY = "startTime";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Filters incoming requests to add logging and tracing capabilities.
     * 
     * This method intercepts all incoming requests and adds comprehensive
     * logging, including request details, timing information, and unique
     * request identifiers for tracing purposes.
     * 
     * @param exchange the server web exchange
     * @param chain the filter chain
     * @return a Mono that completes when the request processing is finished
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        // Generate unique request ID
        String requestId = generateRequestId();
        long startTime = System.currentTimeMillis();
        
        // Log incoming request
        logIncomingRequest(request, requestId);
        
        // Add request ID to exchange attributes for use in handlers
        exchange.getAttributes().put(REQUEST_ID_KEY, requestId);
        exchange.getAttributes().put(START_TIME_KEY, startTime);
        
        // Continue with the filter chain and log the response
        return chain.filter(exchange)
            .contextWrite(Context.of(REQUEST_ID_KEY, requestId))
            .doOnSuccess(unused -> logOutgoingResponse(request, response, requestId, startTime))
            .doOnError(throwable -> logErrorResponse(request, response, requestId, startTime, throwable))
            .doFinally(signalType -> clearMDC());
    }
    
    /**
     * Logs incoming request details.
     * 
     * @param request the HTTP request
     * @param requestId the unique request identifier
     */
    private void logIncomingRequest(ServerHttpRequest request, String requestId) {
        try {
            // Set MDC for structured logging
            MDC.put(REQUEST_ID_KEY, requestId);
            MDC.put("method", request.getMethod().name());
            MDC.put("path", request.getPath().value());
            MDC.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            
            logger.info("Incoming request: {} {} - RequestId: {} - RemoteAddress: {} - UserAgent: {}",
                request.getMethod().name(),
                request.getURI(),
                requestId,
                getRemoteAddress(request),
                getUserAgent(request)
            );
            
            // Log query parameters if present
            if (!request.getQueryParams().isEmpty()) {
                logger.debug("Query parameters: {}", request.getQueryParams());
            }
            
            // Log headers in debug mode
            if (logger.isDebugEnabled()) {
                request.getHeaders().forEach((name, values) -> 
                    logger.debug("Header: {} = {}", name, values)
                );
            }
            
        } catch (Exception e) {
            logger.warn("Error logging incoming request", e);
        }
    }
    
    /**
     * Logs successful response details.
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @param requestId the unique request identifier
     * @param startTime the request start time
     */
    private void logOutgoingResponse(ServerHttpRequest request, ServerHttpResponse response, 
                                   String requestId, long startTime) {
        try {
            long duration = System.currentTimeMillis() - startTime;
            
            // Update MDC with response information
            MDC.put("status", String.valueOf(response.getStatusCode().value()));
            MDC.put("duration", String.valueOf(duration));
            
            logger.info("Outgoing response: {} {} - Status: {} - Duration: {}ms - RequestId: {}",
                request.getMethod().name(),
                request.getPath().value(),
                response.getStatusCode().value(),
                duration,
                requestId
            );
            
            // Log performance warning for slow requests
            if (duration > 1000) {
                logger.warn("Slow request detected: {} {} took {}ms - RequestId: {}",
                    request.getMethod().name(),
                    request.getPath().value(),
                    duration,
                    requestId
                );
            }
            
        } catch (Exception e) {
            logger.warn("Error logging outgoing response", e);
        }
    }
    
    /**
     * Logs error response details.
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @param requestId the unique request identifier
     * @param startTime the request start time
     * @param throwable the error that occurred
     */
    private void logErrorResponse(ServerHttpRequest request, ServerHttpResponse response,
                                String requestId, long startTime, Throwable throwable) {
        try {
            long duration = System.currentTimeMillis() - startTime;
            
            // Update MDC with error information
            MDC.put("error", throwable.getClass().getSimpleName());
            MDC.put("duration", String.valueOf(duration));
            
            logger.error("Error response: {} {} - Error: {} - Duration: {}ms - RequestId: {} - Message: {}",
                request.getMethod().name(),
                request.getPath().value(),
                throwable.getClass().getSimpleName(),
                duration,
                requestId,
                throwable.getMessage()
            );
            
        } catch (Exception e) {
            logger.warn("Error logging error response", e);
        }
    }
    
    /**
     * Generates a unique request identifier.
     * 
     * @return a unique request ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Extracts the remote address from the request.
     * 
     * @param request the HTTP request
     * @return the remote address or "unknown"
     */
    private String getRemoteAddress(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
            request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
    
    /**
     * Extracts the user agent from the request.
     * 
     * @param request the HTTP request
     * @return the user agent or "unknown"
     */
    private String getUserAgent(ServerHttpRequest request) {
        String userAgent = request.getHeaders().getFirst("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }
    
    /**
     * Clears the MDC to prevent memory leaks.
     */
    private void clearMDC() {
        MDC.clear();
    }
}