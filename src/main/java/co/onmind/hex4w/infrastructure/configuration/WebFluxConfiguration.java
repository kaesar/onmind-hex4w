package co.onmind.hex4w.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuration class for WebFlux-specific components.
 * 
 * This configuration class sets up WebFlux-specific beans and configurations
 * required for reactive web processing, including JSON serialization/deserialization,
 * codec configuration, and other WebFlux-related components.
 * 
 * <p>The configuration ensures proper handling of reactive streams, JSON processing,
 * and integration with the Spring WebFlux framework while maintaining consistency
 * with the hexagonal architecture principles.</p>
 * 
 * <p>Uses Jackson 3 ({@code tools.jackson}): Java 8 time types are supported
 * out of the box by {@code jackson-databind}, no extra module needed.</p>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class WebFluxConfiguration implements WebFluxConfigurer {
    
    /**
     * Creates a JsonMapper bean for JSON serialization/deserialization.
     * 
     * This method configures a Jackson 3 JsonMapper with defaults
     * for handling JSON processing in the reactive application,
     * including Java 8 time types (LocalDateTime, LocalDate, etc.).
     * 
     * @return a configured JsonMapper instance
     */
    @Bean
    public JsonMapper jsonMapper() {
        return new JsonMapper();
    }
    
    /**
     * Configures HTTP message codecs for reactive processing.
     * 
     * This method customizes the codec configuration to use the configured
     * JsonMapper for JSON processing, ensuring consistent serialization
     * across the application.
     * 
     * @param configurer the server codec configurer
     */
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        JsonMapper mapper = jsonMapper();
        
        configurer.defaultCodecs().jacksonJsonEncoder(new JacksonJsonEncoder(mapper));
        configurer.defaultCodecs().jacksonJsonDecoder(new JacksonJsonDecoder(mapper));
    }
}
