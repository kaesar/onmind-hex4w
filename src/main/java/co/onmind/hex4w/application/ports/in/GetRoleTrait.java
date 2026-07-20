package co.onmind.hex4w.application.ports.in;

import co.onmind.hex4w.application.dto.out.RoleResponseDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Input port for retrieving roles from the system.
 * 
 * This interface defines the contract for querying and retrieving role information
 * in a reactive manner. It follows the hexagonal architecture pattern by defining
 * the input boundary for role retrieval use cases. The implementation should handle
 * data retrieval, mapping to appropriate DTOs, and error handling for not found scenarios.
 * 
 * <p>All operations are reactive and return Mono/Flux types to support non-blocking
 * operations and backpressure handling in the reactive streams specification.</p>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
public interface GetRoleTrait {
    
    /**
     * Retrieves a specific role by its unique identifier.
     * 
     * This method fetches a single role from the system using its ID. The operation
     * is performed reactively to support high-concurrency scenarios. If the role
     * is not found, the Mono will complete empty rather than emitting an error.
     * 
     * @param id The unique identifier of the role to retrieve. Must not be null and should be a positive number.
     * @return A Mono that emits the role as a RoleResponseDto if found, or completes empty if not found.
     *         The Mono will emit an error if:
     *         <ul>
     *           <li>A persistence error occurs during retrieval</li>
     *           <li>The id parameter is invalid</li>
     *         </ul>
     * 
     * @throws IllegalArgumentException if the id parameter is null or invalid
     * 
     * @see RoleResponseDto
     */
    Mono<RoleResponseDto> getRoleById(Long id);
    
    /**
     * Retrieves all roles from the system.
     * 
     * This method fetches all available roles in the system. The operation is performed
     * reactively using Flux to handle potentially large datasets with backpressure support.
     * The roles are returned in no particular order unless specified by the implementation.
     * 
     * <p>Performance considerations:</p>
     * <ul>
     *   <li>Results are streamed to handle large datasets efficiently</li>
     *   <li>Backpressure is supported through reactive streams</li>
     *   <li>Memory usage is optimized by not loading all data at once</li>
     * </ul>
     * 
     * @return A Flux that emits all roles as RoleResponseDto objects. If no roles exist,
     *         the Flux will complete without emitting any items. The Flux will emit an error if:
     *         <ul>
     *           <li>A persistence error occurs during retrieval</li>
     *           <li>A mapping error occurs during DTO conversion</li>
     *         </ul>
     * 
     * @see RoleResponseDto
     */
    Flux<RoleResponseDto> getAllRoles();
    
    /**
     * Retrieves roles by name pattern matching.
     * 
     * This method searches for roles whose names contain the specified search term.
     * The search is case-insensitive and supports partial matching. The operation
     * is performed reactively to support efficient searching in large datasets.
     * 
     * @param namePattern The search pattern to match against role names. Must not be null or empty.
     *                   Supports partial matching and is case-insensitive.
     * @return A Flux that emits matching roles as RoleResponseDto objects. If no roles match,
     *         the Flux will complete without emitting any items. The Flux will emit an error if:
     *         <ul>
     *           <li>A persistence error occurs during search</li>
     *           <li>The namePattern parameter is invalid</li>
     *         </ul>
     * 
     * @throws IllegalArgumentException if the namePattern parameter is null or empty
     * 
     * @see RoleResponseDto
     */
    Flux<RoleResponseDto> getRolesByNamePattern(String namePattern);
}