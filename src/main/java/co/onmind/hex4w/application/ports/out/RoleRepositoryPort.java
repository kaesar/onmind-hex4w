package co.onmind.hex4w.application.ports.out;

import co.onmind.hex4w.domain.models.Role;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for role persistence operations.
 * 
 * This interface defines the contract for role data persistence in a reactive manner.
 * It follows the hexagonal architecture pattern by defining the output boundary
 * for role repository operations. Implementations should handle the specific
 * persistence technology details (R2DBC, MongoDB, etc.) while maintaining
 * the reactive contract defined here.
 * 
 * <p>All operations are reactive and return Mono/Flux types to support non-blocking
 * database operations and backpressure handling in the reactive streams specification.</p>
 * 
 * <p>This port abstracts the persistence layer from the business logic, allowing
 * different persistence implementations without affecting the domain or application layers.</p>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
public interface RoleRepositoryPort {
    
    /**
     * Saves a role to the persistence layer.
     * 
     * This method persists a role entity, handling both creation and update scenarios.
     * For new roles (id is null), a new record is created with an auto-generated ID.
     * For existing roles (id is not null), the existing record is updated.
     * 
     * <p>The operation is atomic and will either succeed completely or fail without
     * partial updates. The returned Role will contain the assigned ID for new entities.</p>
     * 
     * @param role The role entity to save. Must not be null and should contain valid data.
     * @return A Mono that emits the saved role with updated/assigned ID when the operation completes successfully.
     *         The Mono will emit an error if:
     *         <ul>
     *           <li>A constraint violation occurs (e.g., duplicate name)</li>
     *           <li>A persistence error occurs</li>
     *           <li>The role data is invalid</li>
     *         </ul>
     * 
     * @throws IllegalArgumentException if the role parameter is null
     * 
     * @see Role
     */
    Mono<Role> save(Role role);
    
    /**
     * Retrieves a role by its unique identifier.
     * 
     * This method fetches a single role from the persistence layer using its ID.
     * The operation is performed reactively to support non-blocking database access.
     * 
     * @param id The unique identifier of the role to retrieve. Must not be null and should be a positive number.
     * @return A Mono that emits the role if found, or completes empty if not found.
     *         The Mono will emit an error if:
     *         <ul>
     *           <li>A persistence error occurs during retrieval</li>
     *           <li>The id parameter is invalid</li>
     *         </ul>
     * 
     * @throws IllegalArgumentException if the id parameter is null or invalid
     * 
     * @see Role
     */
    Mono<Role> findById(Long id);
    
    /**
     * Retrieves all roles from the persistence layer.
     * 
     * This method fetches all available roles in the system. The operation is performed
     * reactively using Flux to handle potentially large datasets with backpressure support.
     * Results are streamed to optimize memory usage and performance.
     * 
     * @return A Flux that emits all roles. If no roles exist, the Flux will complete
     *         without emitting any items. The Flux will emit an error if:
     *         <ul>
     *           <li>A persistence error occurs during retrieval</li>
     *         </ul>
     * 
     * @see Role
     */
    Flux<Role> findAll();
    
    /**
     * Checks if a role with the specified name exists.
     * 
     * This method performs an existence check for role names to support uniqueness
     * validation. The check is case-sensitive and looks for exact matches.
     * 
     * @param name The role name to check for existence. Must not be null or empty.
     * @return A Mono that emits true if a role with the specified name exists, false otherwise.
     *         The Mono will emit an error if:
     *         <ul>
     *           <li>A persistence error occurs during the check</li>
     *           <li>The name parameter is invalid</li>
     *         </ul>
     * 
     * @throws IllegalArgumentException if the name parameter is null or empty
     */
    Mono<Boolean> existsByName(String name);
    
    /**
     * Retrieves roles by name pattern matching.
     * 
     * This method searches for roles whose names contain the specified search term.
     * The search is case-insensitive and supports partial matching using SQL LIKE
     * semantics or equivalent for the underlying persistence technology.
     * 
     * @param namePattern The search pattern to match against role names. Must not be null or empty.
     *                   Supports partial matching and is case-insensitive.
     * @return A Flux that emits matching roles. If no roles match, the Flux will complete
     *         without emitting any items. The Flux will emit an error if:
     *         <ul>
     *           <li>A persistence error occurs during search</li>
     *           <li>The namePattern parameter is invalid</li>
     *         </ul>
     * 
     * @throws IllegalArgumentException if the namePattern parameter is null or empty
     * 
     * @see Role
     */
    Flux<Role> findByNameContainingIgnoreCase(String namePattern);
    
    /**
     * Deletes a role by its unique identifier.
     * 
     * This method removes a role from the persistence layer using its ID.
     * The operation is idempotent - attempting to delete a non-existent role
     * will complete successfully without error.
     * 
     * @param id The unique identifier of the role to delete. Must not be null and should be a positive number.
     * @return A Mono that completes when the deletion operation finishes successfully.
     *         The Mono will emit an error if:
     *         <ul>
     *           <li>A persistence error occurs during deletion</li>
     *           <li>The id parameter is invalid</li>
     *           <li>A constraint violation prevents deletion (e.g., foreign key references)</li>
     *         </ul>
     * 
     * @throws IllegalArgumentException if the id parameter is null or invalid
     */
    Mono<Void> deleteById(Long id);
    
    /**
     * Counts the total number of roles in the system.
     * 
     * This method returns the total count of roles stored in the persistence layer.
     * The operation is performed reactively to support non-blocking database access.
     * 
     * @return A Mono that emits the total count of roles as a Long value.
     *         The Mono will emit an error if:
     *         <ul>
     *           <li>A persistence error occurs during counting</li>
     *         </ul>
     */
    Mono<Long> count();
}