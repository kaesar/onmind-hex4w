package co.onmind.hex4w.infrastructure.configuration;

import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Configuration class for validation components.
 * 
 * This configuration class sets up the validation infrastructure required
 * for reactive request validation in the WebFlux application. It provides
 * the necessary beans for Bean Validation (JSR-303) support.
 * 
 * <p>The validation configuration ensures that input DTOs are properly
 * validated before being processed by the application layer, maintaining
 * data integrity and providing clear error messages for invalid requests.</p>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class ValidationConfiguration {
    
    /**
     * Creates a Validator bean for reactive validation.
     * 
     * This method configures a LocalValidatorFactoryBean that provides
     * JSR-303 Bean Validation support for the application. The validator
     * is used in handlers to validate incoming request DTOs before
     * processing them through use cases.
     * 
     * <p>The validator supports all standard JSR-303 annotations such as:</p>
     * <ul>
     *   <li>@NotNull, @NotBlank, @NotEmpty</li>
     *   <li>@Size, @Min, @Max</li>
     *   <li>@Pattern, @Email</li>
     *   <li>Custom validation annotations</li>
     * </ul>
     * 
     * @return a configured Validator instance
     */
    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }
}