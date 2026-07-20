package co.onmind.hex4w.application.ports.in;

import co.onmind.hex4w.application.dto.in.CreateRoleRequestDto;
import co.onmind.hex4w.application.dto.out.RoleResponseDto;
import reactor.core.publisher.Mono;

/**
 * Input port for creating roles in the system.
 * 
 * This interface defines the contract for creating new roles in a reactive manner.
 * It follows the hexagonal architecture pattern by defining the input boundary
 * for role creation use cases. The implementation should handle business logic
 * validation, orchestration with domain services, and persistence operations.
 * 
 * <p>All operations are reactive and return Mono types to support non-blocking
 * operations and backpressure handling in the reactive streams specification.</p>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
public interface CreateRoleTrait {
    
    /**
     * Creates a new role in the system.
     * 
     * This method processes the role creation request by validating the input data,
     * applying business rules through domain services, and persisting the new role.
     * The operation is performed reactively to support high-concurrency scenarios.
     * 
     * <p>Business rules applied during creation:</p>
     * <ul>
     *   <li>Role name must be unique in the system</li>
     *   <li>Role name must pass domain validation rules</li>
     *   <li>Creation timestamp is automatically assigned</li>
     * </ul>
     * 
     * @param request The role creation request containing the role name and other required data.
     *                Must not be null and should contain valid data according to DTO validation rules.
     * @return A Mono that emits the created role as a RoleResponseDto when the operation completes successfully.
     *         The Mono will emit an error if:
     *         <ul>
     *           <li>The role name already exists (DuplicateRoleException)</li>
     *           <li>The input data is invalid (ValidationException)</li>
     *           <li>A persistence error occurs (PersistenceException)</li>
     *         </ul>
     * 
     * @throws IllegalArgumentException if the request parameter is null
     * 
     * @see CreateRoleRequestDto
     * @see RoleResponseDto
     * @see co.onmind.hex4w.domain.exceptions.DuplicateRoleException
     */
    Mono<RoleResponseDto> createRole(CreateRoleRequestDto request);
}