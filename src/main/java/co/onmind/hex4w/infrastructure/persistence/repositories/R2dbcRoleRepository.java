package co.onmind.hex4w.infrastructure.persistence.repositories;

import co.onmind.hex4w.infrastructure.persistence.entities.RoleEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * R2DBC reactive repository for role entities.
 * 
 * This interface extends ReactiveCrudRepository to provide reactive CRUD operations
 * for RoleEntity. It leverages Spring Data R2DBC's automatic implementation
 * generation and provides additional custom query methods for role-specific operations.
 * 
 * <p>All operations are reactive and return Mono/Flux types to support non-blocking
 * database operations with backpressure handling.</p>
 * 
 * <p>The repository is automatically implemented by Spring Data R2DBC at runtime,
 * providing standard CRUD operations plus the custom query methods defined here.</p>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface R2dbcRoleRepository extends ReactiveCrudRepository<RoleEntity, Long> {
    
    /**
     * Checks if a role with the specified name exists.
     * 
     * This method performs a case-sensitive existence check for role names.
     * It's commonly used for uniqueness validation before creating new roles.
     * 
     * @param name The role name to check for existence. Must not be null or empty.
     * @return A Mono that emits true if a role with the specified name exists, false otherwise.
     *         The Mono will emit an error if a database error occurs during the check.
     */
    Mono<Boolean> existsByName(String name);
    
    /**
     * Finds roles by name pattern matching (case-insensitive).
     * 
     * This method searches for roles whose names contain the specified search term.
     * The search is case-insensitive and supports partial matching using SQL LIKE semantics.
     * 
     * <p>The search pattern is automatically wrapped with wildcards, so searching for "admin"
     * will match roles like "ADMIN", "Administrator", "admin_user", etc.</p>
     * 
     * @param name The search pattern to match against role names. Must not be null or empty.
     * @return A Flux that emits matching roles. If no roles match, the Flux will complete
     *         without emitting any items. The Flux will emit an error if a database error occurs.
     */
    Flux<RoleEntity> findByNameContainingIgnoreCase(String name);
    
    /**
     * Finds a role by its exact name (case-sensitive).
     * 
     * This method retrieves a single role that matches the specified name exactly.
     * It's useful for authentication and authorization scenarios where exact name
     * matching is required.
     * 
     * @param name The exact name of the role to find. Must not be null or empty.
     * @return A Mono that emits the role if found, or completes empty if not found.
     *         The Mono will emit an error if a database error occurs during retrieval.
     */
    Mono<RoleEntity> findByName(String name);
}