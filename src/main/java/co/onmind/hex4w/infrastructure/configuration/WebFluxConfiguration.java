package co.onmind.hex4w.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

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
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class WebFluxConfiguration implements WebFluxConfigurer {
    
    /**
     * Creates an ObjectMapper bean for JSON serialization/deserialization.
     * 
     * This method configures a Jackson ObjectMapper with appropriate modules
     * and settings for handling JSON processing in the reactive application.
     * The ObjectMapper is configured to handle Java 8 time types and other
     * common serialization scenarios.
     * 
     * <p>Configuration includes:</p>
     * <ul>
     *   <li>JavaTimeModule for LocalDateTime, LocalDate, etc.</li>
     *   <li>Proper handling of null values</li>
     *   <li>Consistent date/time formatting</li>
     * </ul>
     * 
     * @return a configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
    
    /**
     * Configures HTTP message codecs for reactive processing.
     * 
     * This method customizes the codec configuration to use the configured
     * ObjectMapper for JSON processing, ensuring consistent serialization
     * across the application.
     * 
     * @param configurer the server codec configurer
     */
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        ObjectMapper mapper = objectMapper();
        
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(mapper));
        configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(mapper));
    }
}