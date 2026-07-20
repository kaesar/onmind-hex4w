package co.onmind.hex4w.application.usecases;

import co.onmind.hex4w.application.dto.in.CreateRoleRequestDto;
import co.onmind.hex4w.application.dto.out.RoleResponseDto;
import co.onmind.hex4w.application.mappers.RoleMapper;
import co.onmind.hex4w.application.ports.in.CreateRoleTrait;
import co.onmind.hex4w.application.ports.in.GetRoleTrait;
import co.onmind.hex4w.application.ports.out.RoleRepositoryPort;
import co.onmind.hex4w.domain.exceptions.DuplicateRoleException;
import co.onmind.hex4w.domain.services.RoleService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive implementation of role use cases.
 * 
 * This class orchestrates the business logic for role operations by coordinating
 * between domain services, repository ports, and mappers in a reactive manner.
 *
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class RoleUseCase implements CreateRoleTrait, GetRoleTrait {
    
    private final RoleService roleService;
    private final RoleRepositoryPort roleRepository;
    private final RoleMapper roleMapper;
    
    /**
     * Constructs a new RoleUseCase with the required dependencies.
     * 
     * @param roleService The domain service for role business logic
     * @param roleRepository The repository port for role persistence operations
     * @param roleMapper The mapper for converting between domain models and DTOs
     */
    public RoleUseCase(
            RoleService roleService,
            RoleRepositoryPort roleRepository,
            RoleMapper roleMapper) {
        this.roleService = roleService;
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>This implementation performs the following steps reactively:</p>
     * <ol>
     *   <li>Validates input parameters</li>
     *   <li>Checks for duplicate role names</li>
     *   <li>Creates the role using domain service</li>
     *   <li>Persists the role through repository port</li>
     *   <li>Maps the result to response DTO</li>
     * </ol>
     * 
     * <p>The operation is atomic and will either succeed completely or fail
     * without partial updates.</p>
     */
    @Override
    public Mono<RoleResponseDto> createRole(CreateRoleRequestDto request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("CreateRoleRequestDto cannot be null"));
        }
        
        return validateUniqueRoleName(request.name())
            .then(Mono.defer(() -> roleService.createRole(request.name())))
            .flatMap(roleRepository::save)
            .map(roleMapper::toResponseDto)
            .onErrorMap(this::mapDomainExceptions);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>This implementation retrieves the role by ID and maps it to a response DTO.
     * If the role is not found, the Mono completes empty rather than emitting an error.</p>
     */
    @Override
    public Mono<RoleResponseDto> getRoleById(Long id) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("Role ID cannot be null"));
        }
        
        if (id <= 0) {
            return Mono.error(new IllegalArgumentException("Role ID must be a positive number"));
        }
        
        return roleRepository.findById(id)
            .map(roleMapper::toResponseDto);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>This implementation retrieves all roles from the repository and maps them
     * to response DTOs. The operation is performed reactively using Flux to handle
     * potentially large datasets with backpressure support.</p>
     */
    @Override
    public Flux<RoleResponseDto> getAllRoles() {
        return roleRepository.findAll()
            .map(roleMapper::toResponseDto);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>This implementation searches for roles by name pattern and maps them
     * to response DTOs. The search is case-insensitive and supports partial matching.</p>
     */
    @Override
    public Flux<RoleResponseDto> getRolesByNamePattern(String namePattern) {
        if (namePattern == null || namePattern.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("Name pattern cannot be null or empty"));
        }
        
        return roleRepository.findByNameContainingIgnoreCase(namePattern.trim())
            .map(roleMapper::toResponseDto);
    }
    
    /**
     * Validates that a role name is unique in the system.
     * 
     * This method checks if a role with the given name already exists and
     * throws a DuplicateRoleException if it does.
     * 
     * @param roleName The role name to validate
     * @return A Mono that completes if the name is unique, or emits an error if duplicate
     */
    private Mono<Void> validateUniqueRoleName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Role name cannot be null or empty"));
        }
        
        return roleRepository.existsByName(roleName.trim())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(DuplicateRoleException.forName(roleName.trim()));
                }
                return Mono.empty();
            });
    }
    
    /**
     * Maps domain exceptions to appropriate application-level exceptions.
     * 
     * This method ensures that domain exceptions are properly propagated
     * while maintaining the reactive error handling chain.
     * 
     * @param throwable The original exception
     * @return The mapped exception
     */
    private Throwable mapDomainExceptions(Throwable throwable) {
        // Domain exceptions are already properly typed, so we pass them through
        if (throwable instanceof DuplicateRoleException ||
            throwable instanceof IllegalArgumentException) {
            return throwable;
        }
        
        // For unexpected exceptions, wrap them in a generic runtime exception
        return new RuntimeException("An error occurred while processing the role operation", throwable);
    }
}