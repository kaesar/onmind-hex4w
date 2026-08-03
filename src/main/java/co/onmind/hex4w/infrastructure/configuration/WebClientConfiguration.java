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
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import co.onmind.hex4w.application.ports.out.AbcPort;
import co.onmind.hex4w.application.ports.out.CachePort;
import co.onmind.hex4w.infrastructure.webclients.CachedAbcAdapter;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcToken;
import co.onmind.hex4w.transverse.WebClientGeneric;
import com.fasterxml.jackson.databind.ObjectMapper;

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
     * Creates a WebClientGeneric bean wrapping the default WebClient.
     * 
     * @param webClient the default WebClient
     * @return a WebClientGeneric instance
     */
    @Bean
    public WebClientGeneric webClientGeneric(WebClient webClient) {
        return new WebClientGeneric(webClient);
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
     * Creates a WebClient specifically configured for XDB ABC API.
     * 
     * This WebClient is configured for communication with XDB database
     * via the ABC API endpoint (/abc).
     * 
     * @param baseUrl the base URL for the XDB service
     * @return a configured WebClient for XDB
     */
    @Bean
    public WebClient xdbWebClient(@Value("${app.xdb.base-url:http://localhost:8082}") String baseUrl) {
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
     * Creates a AbcWebClient bean for XDB ABC API integration.
     * 
     * @param xdbWebClient the configured WebClient for XDB (with base URL)
     * @return a configured AbcWebClient instance
     */
    @Bean
    public co.onmind.hex4w.infrastructure.webclients.AbcWebClient abcWebClientBean(
            @org.springframework.beans.factory.annotation.Qualifier("xdbWebClient") WebClient xdbWebClient,
            @Value("${app.xdb.auth-type:none}") String authType,
            @Value("${app.xdb.auth-token:}") String authToken) {
        co.onmind.hex4w.transverse.WebClientGeneric xdbWebClientGeneric = 
            new co.onmind.hex4w.transverse.WebClientGeneric(xdbWebClient);
        AbcToken token = switch (authType.toLowerCase()) {
            case "bearer" -> AbcToken.bearer(authToken);
            case "basic" -> {
                String[] parts = authToken.split(":", 2);
                String user = parts.length > 0 ? parts[0] : "";
                String pass = parts.length > 1 ? parts[1] : "";
                yield AbcToken.basic(user, pass);
            }
            default -> AbcToken.none();
        };
        return new co.onmind.hex4w.infrastructure.webclients.AbcWebClient(xdbWebClientGeneric, token);
    }

    @Bean
    public co.onmind.hex4w.infrastructure.webclients.AbcAdapter abcAdapter(
            co.onmind.hex4w.infrastructure.webclients.AbcWebClient abcWebClient) {
        return new co.onmind.hex4w.infrastructure.webclients.AbcAdapter(abcWebClient);
    }

    @Bean
    @org.springframework.context.annotation.Primary
    public AbcPort abcPort(
            co.onmind.hex4w.infrastructure.webclients.AbcAdapter abcAdapter,
            CachePort cachePort,
            ObjectMapper objectMapper,
            @Value("${app.xdb.cache.ttl-seconds:300}") int ttlSeconds) {
        return new CachedAbcAdapter(abcAdapter, cachePort, objectMapper,
                Duration.ofSeconds(ttlSeconds));
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

    @Bean
    public LambdaAsyncClient lambdaAsyncClient(
            @Value("${app.lambda.region:us-east-1}") String region,
            @Value("${app.lambda.endpoint:#{null}}") String endpoint) {

        var builder = LambdaAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
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