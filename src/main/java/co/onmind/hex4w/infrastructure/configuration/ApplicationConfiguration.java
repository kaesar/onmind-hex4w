package co.onmind.hex4w.infrastructure.configuration;

import co.onmind.hex4w.application.ports.in.CreateRoleTrait;
import co.onmind.hex4w.application.ports.in.GetRoleTrait;
import co.onmind.hex4w.application.ports.out.RoleRepositoryPort;
import co.onmind.hex4w.application.usecases.RoleUseCase;
import co.onmind.hex4w.domain.services.RoleService;
import co.onmind.hex4w.application.mappers.RoleMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Main application configuration for dependency injection and bean wiring.
 * 
 * This configuration class defines the main application beans and their dependencies,
 * following the hexagonal architecture principles. It ensures proper dependency
 * injection between layers while maintaining the separation of concerns.
 * 
 * <p>The configuration focuses on:</p>
 * <ul>
 *   <li>Wiring use cases with their dependencies</li>
 *   <li>Configuring domain services</li>
 *   <li>Setting up reactive components</li>
 *   <li>Maintaining loose coupling between layers</li>
 * </ul>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class ApplicationConfiguration {
    
    /**
     * Creates the main use case implementation bean.
     * 
     * This method wires together the use case implementation with its required
     * dependencies, including domain services, repository ports, and mappers.
     * The use case serves as the orchestrator between the domain layer and
     * infrastructure adapters.
     * 
     * @param roleService the domain service for role business logic
     * @param roleRepository the repository port for data persistence
     * @param roleMapper the mapper for DTO conversions
     * @return a configured RoleUseCase instance
     */
    @Bean
    @ConditionalOnMissingBean
    public RoleUseCase roleUseCase(
            RoleService roleService,
            RoleRepositoryPort roleRepository,
            RoleMapper roleMapper) {
        return new RoleUseCase(roleService, roleRepository, roleMapper);
    }
    
    /**
     * Provides the CreateRoleTrait interface implementation.
     * 
     * This method exposes the use case implementation through its input port
     * interface, allowing handlers to depend on the abstraction rather than
     * the concrete implementation.
     * 
     * @param roleUseCase the use case implementation
     * @return the CreateRoleTrait interface
     */
    @Bean
    @ConditionalOnMissingBean
    public CreateRoleTrait createRoleUseCase(RoleUseCase roleUseCase) {
        return roleUseCase;
    }
    
    /**
     * Provides the GetRoleTrait interface implementation.
     * 
     * This method exposes the use case implementation through its input port
     * interface, allowing handlers to depend on the abstraction rather than
     * the concrete implementation.
     * 
     * @param roleUseCase the use case implementation
     * @return the GetRoleTrait interface
     */
    @Bean
    @ConditionalOnMissingBean
    public GetRoleTrait getRoleUseCase(RoleUseCase roleUseCase) {
        return roleUseCase;
    }
}