package co.onmind.hex4w.transverse;

import co.onmind.hex4w.infrastructure.configuration.WebClientConfiguration.ExternalServiceException;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * Generic utility class for WebClient operations.
 *
 * <p>All methods have two flavors:
 * <ul>
 *   <li>Plain — no auth header (existing usage).</li>
 *   <li>With {@code XdbToken} — injects Bearer/Basic/NoAuth header.</li>
 * </ul></p>
 */
public class WebClientGeneric {
    
    private static final Logger logger = LoggerFactory.getLogger(WebClientGeneric.class);
    
    private final WebClient webClient;
    
    /**
     * Constructs a new WebClientGeneric with the provided WebClient.
     * 
     * @param webClient the WebClient instance to use for HTTP operations
     */
    public WebClientGeneric(WebClient webClient) {
        this.webClient = webClient;
    }
    
    /**
     * Performs a reactive GET request and returns a single object.
     * 
     * This method makes a GET request to the specified URI and deserializes
     * the response to the specified type. It includes automatic retry logic
     * and error handling.
     * 
     * @param <T> the type of the response object
     * @param uri the request URI
     * @param responseType the class of the response type
     * @return a Mono containing the response object
     */
    public <T> Mono<T> get(String uri, Class<T> responseType) {
        logger.debug("Making GET request to: {}", uri);
        
        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(responseType)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(createRetrySpec())
            .doOnSuccess(result -> logger.debug("GET request successful: {}", uri))
            .doOnError(error -> logger.error("GET request failed: {} - Error: {}", uri, error.getMessage()));
    }

    public <T> Mono<T> get(String uri, Class<T> responseType, AbcToken auth) {
        var spec = webClient.get().uri(uri);
        String hdr = auth.toHeaderValue();
        if (!hdr.isEmpty()) spec.header(HttpHeaders.AUTHORIZATION, hdr);
        return spec.retrieve()
            .bodyToMono(responseType)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(createRetrySpec())
            .doOnSuccess(result -> logger.debug("GET request successful: {}", uri))
            .doOnError(error -> logger.error("GET request failed: {} - Error: {}", uri, error.getMessage()));
    }
    
    /**
     * Performs a reactive GET request and returns a collection of objects.
     * 
     * This method makes a GET request to the specified URI and deserializes
     * the response to a Flux of the specified type.
     * 
     * @param <T> the type of the response objects
     * @param uri the request URI
     * @param responseType the class of the response type
     * @return a Flux containing the response objects
     */
    public <T> Flux<T> getMany(String uri, Class<T> responseType) {
        logger.debug("Making GET request for collection to: {}", uri);
        
        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToFlux(responseType)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(createRetrySpec())
            .doOnComplete(() -> logger.debug("GET collection request successful: {}", uri))
            .doOnError(error -> logger.error("GET collection request failed: {} - Error: {}", uri, error.getMessage()));
    }

    public <T> Flux<T> getMany(String uri, Class<T> responseType, AbcToken auth) {
        var spec = webClient.get().uri(uri);
        String hdr = auth.toHeaderValue();
        if (!hdr.isEmpty()) spec.header(HttpHeaders.AUTHORIZATION, hdr);
        return spec.retrieve()
            .bodyToFlux(responseType)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(createRetrySpec())
            .doOnComplete(() -> logger.debug("GET collection request successful: {}", uri))
            .doOnError(error -> logger.error("GET collection request failed: {} - Error: {}", uri, error.getMessage()));
    }
    
    /**
     * Performs a reactive POST request with a request body.
     * 
     * This method makes a POST request to the specified URI with the provided
     * request body and deserializes the response to the specified type.
     * 
     * @param <T> the type of the request body
     * @param <R> the type of the response object
     * @param uri the request URI
     * @param requestBody the request body object
     * @param responseType the class of the response type
     * @return a Mono containing the response object
     */
    public <T, R> Mono<R> post(String uri, T requestBody, Class<R> responseType) {
        logger.debug("Making POST request to: {}", uri);
        
        return webClient.post()
            .uri(uri)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(responseType)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(createRetrySpec())
            .doOnSuccess(result -> logger.debug("POST request successful: {}", uri))
            .doOnError(error -> logger.error("POST request failed: {} - Error: {}", uri, error.getMessage()));
    }

    public <T, R> Mono<R> post(String uri, T requestBody, Class<R> responseType, AbcToken auth) {
        var spec = webClient.post().uri(uri);
        String hdr = auth.toHeaderValue();
        if (!hdr.isEmpty()) {
            spec.header(HttpHeaders.AUTHORIZATION, hdr);
        }
        return spec.bodyValue(requestBody)
            .retrieve()
            .bodyToMono(responseType)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(createRetrySpec())
            .doOnSuccess(result -> logger.debug("POST request successful: {}", uri))
            .doOnError(error -> logger.error("POST request failed: {} - Error: {}", uri, error.getMessage()));
    }
    
    /**
     * Performs a reactive POST request without expecting a response body.
     * 
     * This method makes a POST request to the specified URI with the provided
     * request body and returns a Mono<Void> indicating completion.
     * 
     * @param <T> the type of the request body
     * @param uri the request URI
     * @param requestBody the request body object
     * @return a Mono<Void> indicating completion
     */
    public <T> Mono<Void> postVoid(String uri, T requestBody) {
        logger.debug("Making POST request (void response) to: {}", uri);
        
        return webClient.post()
            .uri(uri)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Void.class)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(createRetrySpec())
            .doOnSuccess(result -> logger.debug("POST void request successful: {}", uri))
            .doOnError(error -> logger.error("POST void request failed: {} - Error: {}", uri, error.getMessage()));
    }
    
    /**
     * Performs a reactive PUT request with a request body.
     * 
     * This method makes a PUT request to the specified URI with the provided
     * request body and deserializes the response to the specified type.
     * 
     * @param <T> the type of the request body
     * @param <R> the type of the response object
     * @param uri the request URI
     * @param requestBody the request body object
     * @param responseType the class of the response type
     * @return a Mono containing the response object
     */
    public <T, R> Mono<R> put(String uri, T requestBody, Class<R> responseType) {
        logger.debug("Making PUT request to: {}", uri);
        
        return webClient.put()
            .uri(uri)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(responseType)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(createRetrySpec())
            .doOnSuccess(result -> logger.debug("PUT request successful: {}", uri))
            .doOnError(error -> logger.error("PUT request failed: {} - Error: {}", uri, error.getMessage()));
    }

    public <T, R> Mono<R> put(String uri, T requestBody, Class<R> responseType, AbcToken auth) {
        var spec = webClient.put().uri(uri);
        String hdr = auth.toHeaderValue();
        if (!hdr.isEmpty()) spec.header(HttpHeaders.AUTHORIZATION, hdr);
        return spec.bodyValue(requestBody)
            .retrieve()
            .bodyToMono(responseType)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(createRetrySpec())
            .doOnSuccess(result -> logger.debug("PUT request successful: {}", uri))
            .doOnError(error -> logger.error("PUT request failed: {} - Error: {}", uri, error.getMessage()));
    }
    
    /**
     * Performs a reactive DELETE request.
     * 
     * This method makes a DELETE request to the specified URI and returns
     * a Mono<Void> indicating completion.
     * 
     * @param uri the request URI
     * @return a Mono<Void> indicating completion
     */
    public Mono<Void> delete(String uri) {
        logger.debug("Making DELETE request to: {}", uri);
        
        return webClient.delete()
            .uri(uri)
            .retrieve()
            .bodyToMono(Void.class)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(createRetrySpec())
            .doOnSuccess(result -> logger.debug("DELETE request successful: {}", uri))
            .doOnError(error -> logger.error("DELETE request failed: {} - Error: {}", uri, error.getMessage()));
    }

    public Mono<Void> delete(String uri, AbcToken auth) {
        var spec = webClient.delete().uri(uri);
        String hdr = auth.toHeaderValue();
        if (!hdr.isEmpty()) spec.header(HttpHeaders.AUTHORIZATION, hdr);
        return spec.retrieve()
            .bodyToMono(Void.class)
            .timeout(Duration.ofSeconds(30))
            .retryWhen(createRetrySpec())
            .doOnSuccess(result -> logger.debug("DELETE request successful: {}", uri))
            .doOnError(error -> logger.error("DELETE request failed: {} - Error: {}", uri, error.getMessage()));
    }
    
    /**
     * Creates a retry specification for HTTP requests.
     * 
     * This method configures retry behavior with exponential backoff
     * for transient failures. It retries on specific HTTP status codes
     * and network errors.
     * 
     * @return a Retry specification
     */
    private Retry createRetrySpec() {
        return Retry.backoff(3, Duration.ofMillis(500))
            .maxBackoff(Duration.ofSeconds(5))
            .filter(isRetryableError())
            .doBeforeRetry(retrySignal -> 
                logger.warn("Retrying request (attempt {}): {}", 
                    retrySignal.totalRetries() + 1, 
                    retrySignal.failure().getMessage())
            );
    }
    
    /**
     * Creates a predicate to determine if an error is retryable.
     * 
     * This method defines which types of errors should trigger a retry.
     * Generally, transient network errors and server errors (5xx) are
     * considered retryable, while client errors (4xx) are not.
     * 
     * @return a Predicate that returns true for retryable errors
     */
    private Predicate<Throwable> isRetryableError() {
        return throwable -> {
            if (throwable instanceof ExternalServiceException externalEx) {
                int statusCode = externalEx.getStatusCode();
                // Retry on server errors (5xx) but not client errors (4xx)
                return statusCode >= HttpStatus.INTERNAL_SERVER_ERROR.value();
            }
            
            // Retry on network-related exceptions
            return throwable instanceof java.net.ConnectException ||
                   throwable instanceof java.util.concurrent.TimeoutException ||
                   throwable instanceof java.io.IOException;
        };
    }
}