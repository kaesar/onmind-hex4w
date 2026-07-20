package co.onmind.hex4w.infrastructure.configuration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class for WebClient and external service communication.
 * 
 * This configuration class sets up WebClient instances for making reactive
 * HTTP calls to external services. It provides proper timeout configuration,
 * connection pooling, logging, and error handling for external communications.
 * 
 * <p>Features provided:</p>
 * <ul>
 *   <li>Connection timeout and read/write timeout configuration</li>
 *   <li>Connection pooling for better performance</li>
 *   <li>Request/response logging for debugging</li>
 *   <li>Error handling and retry mechanisms</li>
 *   <li>Base URL configuration for different environments</li>
 * </ul>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class WebClientConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(WebClientConfiguration.class);
    
    @Value("${app.webclient.connect-timeout:5000}")
    private int connectTimeout;
    
    @Value("${app.webclient.read-timeout:10000}")
    private int readTimeout;
    
    @Value("${app.webclient.write-timeout:10000}")
    private int writeTimeout;
    
    @Value("${app.webclient.max-memory-size:1048576}")
    private int maxMemorySize;
    
    /**
     * Creates a default WebClient bean for general HTTP communications.
     * 
     * This WebClient is configured with appropriate timeouts, connection pooling,
     * and logging for making HTTP calls to external services. It includes
     * request/response logging and error handling.
     * 
     * @return a configured WebClient instance
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .clientConnector(createClientConnector())
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxMemorySize))
            .filter(logRequest())
            .filter(logResponse())
            .filter(handleErrors())
            .build();
    }
    
    /**
     * Creates a WebClient specifically configured for notification services.
     * 
     * This WebClient can be used for sending notifications to external services
     * and includes specific configuration optimized for notification scenarios.
     * 
     * @param baseUrl the base URL for the notification service
     * @return a configured WebClient for notifications
     */
    @Bean
    public WebClient notificationWebClient(@Value("${app.notification.base-url:http://localhost:8081}") String baseUrl) {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(createClientConnector())
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxMemorySize))
            .filter(logRequest())
            .filter(logResponse())
            .filter(handleErrors())
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .build();
    }
    
    /**
     * Creates a ReactorClientHttpConnector with custom HTTP client configuration.
     * 
     * This method configures the underlying HTTP client with appropriate
     * timeouts, connection pooling, and other performance optimizations.
     * 
     * @return a configured ReactorClientHttpConnector
     */
    private ReactorClientHttpConnector createClientConnector() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
            .responseTimeout(Duration.ofMillis(readTimeout))
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
            );
        
        return new ReactorClientHttpConnector(httpClient);
    }
    
    /**
     * Creates a filter for logging outgoing requests.
     * 
     * This filter logs details about outgoing HTTP requests including
     * method, URL, headers, and body (when appropriate).
     * 
     * @return an ExchangeFilterFunction for request logging
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (logger.isDebugEnabled()) {
                logger.debug("Outgoing request: {} {} - Headers: {}", 
                    clientRequest.method(), 
                    clientRequest.url(),
                    clientRequest.headers()
                );
            } else {
                logger.info("Outgoing request: {} {}", 
                    clientRequest.method(), 
                    clientRequest.url()
                );
            }
            return Mono.just(clientRequest);
        });
    }
    
    /**
     * Creates a filter for logging incoming responses.
     * 
     * This filter logs details about incoming HTTP responses including
     * status code, headers, and timing information.
     * 
     * @return an ExchangeFilterFunction for response logging
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (logger.isDebugEnabled()) {
                logger.debug("Incoming response: {} - Headers: {}", 
                    clientResponse.statusCode(),
                    clientResponse.headers().asHttpHeaders()
                );
            } else {
                logger.info("Incoming response: {}", clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }
    
    /**
     * Creates a filter for handling HTTP errors.
     * 
     * This filter provides centralized error handling for HTTP responses,
     * converting HTTP error status codes into appropriate exceptions.
     * 
     * @return an ExchangeFilterFunction for error handling
     */
    private ExchangeFilterFunction handleErrors() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                return clientResponse.bodyToMono(String.class)
                    .defaultIfEmpty("Unknown error")
                    .flatMap(errorBody -> {
                        String errorMessage = String.format(
                            "HTTP %d error: %s", 
                            clientResponse.statusCode().value(), 
                            errorBody
                        );
                        
                        logger.error("External service error: {}", errorMessage);
                        
                        return Mono.error(new ExternalServiceException(
                            errorMessage, 
                            clientResponse.statusCode().value()
                        ));
                    });
            }
            return Mono.just(clientResponse);
        });
    }
    
    /**
     * Creates a NotificationWebClient bean for notification service integration.
     * 
     * @param notificationWebClient the configured WebClient for notifications
     * @param webClientGeneric the generic WebClient utility
     * @return a configured NotificationWebClient instance
     */
    @Bean
    public co.onmind.hex4w.infrastructure.webclients.NotificationWebClient notificationWebClientBean(
            @org.springframework.beans.factory.annotation.Qualifier("notificationWebClient") WebClient notificationWebClient,
            co.onmind.hex4w.transverse.WebClientGeneric webClientGeneric) {
        return new co.onmind.hex4w.infrastructure.webclients.NotificationWebClient(notificationWebClient, webClientGeneric);
    }
    
    @Bean
    public S3AsyncClient s3AsyncClient(
            @Value("${app.s3.region:us-east-1}") String region,
            @Value("${app.s3.endpoint:#{null}}") String endpoint,
            @Value("${app.s3.force-path-style:false}") boolean forcePathStyle) {

        var builder = S3AsyncClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create());

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        if (forcePathStyle) {
            builder.serviceConfiguration(config -> config.pathStyleAccessEnabled(true));
        }

        return builder.build();
    }

    /**
     * Exception thrown when external service calls fail.
     * 
     * This exception encapsulates HTTP errors from external services
     * and provides structured error information for proper handling.
     */
    public static class ExternalServiceException extends RuntimeException {
        private final int statusCode;
        
        /**
         * Constructs a new ExternalServiceException.
         * 
         * @param message the error message
         * @param statusCode the HTTP status code
         */
        public ExternalServiceException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
        
        /**
         * Gets the HTTP status code associated with this exception.
         * 
         * @return the HTTP status code
         */
        public int getStatusCode() {
            return statusCode;
        }
    }
}