package co.onmind.hex4w.infrastructure.persistence.adapters;

import co.onmind.hex4w.application.ports.out.RoleRepositoryPort;
import co.onmind.hex4w.domain.models.Role;
import co.onmind.hex4w.infrastructure.persistence.entities.RoleEntity;
import co.onmind.hex4w.infrastructure.persistence.mappers.RoleEntityMapper;
import co.onmind.hex4w.infrastructure.persistence.repositories.R2dbcRoleRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * R2DBC implementation of the RoleRepositoryPort.
 * 
 * This adapter implements the output port for role persistence using R2DBC
 * as the underlying reactive database technology. It bridges the gap between
 * the domain layer (which works with Role domain models) and the persistence
 * layer (which works with RoleEntity).
 * 
 * <p>The adapter follows the hexagonal architecture pattern by implementing
 * the port interface defined in the application layer, while using infrastructure
 * components (R2DBC repository and entity mapper) to handle the actual persistence.</p>
 * 
 * <p>All operations are reactive and maintain the non-blocking characteristics
 * required by the WebFlux reactive stack.</p>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public class RoleRepositoryAdapter implements RoleRepositoryPort {
    
    private final R2dbcRoleRepository r2dbcRepository;
    private final RoleEntityMapper entityMapper;
    
    /**
     * Constructor for dependency injection.
     * 
     * @param r2dbcRepository The R2DBC repository for database operations
     * @param entityMapper The mapper for converting between domain models and entities
     */
    public RoleRepositoryAdapter(R2dbcRoleRepository r2dbcRepository, RoleEntityMapper entityMapper) {
        this.r2dbcRepository = r2dbcRepository;
        this.entityMapper = entityMapper;
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>This implementation converts the domain model to an entity, saves it using
     * the R2DBC repository, and converts the result back to a domain model.
     * For new roles (id is null), the createdAt timestamp is set automatically.</p>
     */
    @Override
    public Mono<Role> save(Role role) {
        return Mono.just(role)
                .map(r -> {
                    RoleEntity entity = entityMapper.toEntity(r);
                    // Set createdAt for new entities
                    if (entity.getId() == null && entity.getCreatedAt() == null) {
                        entity.setCreatedAt(LocalDateTime.now());
                    }
                    return entity;
                })
                .flatMap(r2dbcRepository::save)
                .map(entityMapper::toDomain);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>This implementation uses the R2DBC repository to find the entity by ID
     * and converts the result to a domain model if found.</p>
     */
    @Override
    public Mono<Role> findById(Long id) {
        return r2dbcRepository.findById(id)
                .map(entityMapper::toDomain);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>This implementation retrieves all entities from the R2DBC repository
     * and converts each one to a domain model using the reactive stream mapping.</p>
     */
    @Override
    public Flux<Role> findAll() {
        return r2dbcRepository.findAll()
                .map(entityMapper::toDomain);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>This implementation delegates directly to the R2DBC repository's
     * existsByName method, which performs the existence check at the database level.</p>
     */
    @Override
    public Mono<Boolean> existsByName(String name) {
        return r2dbcRepository.existsByName(name);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>This implementation uses the R2DBC repository's case-insensitive search
     * method and converts the results to domain models.</p>
     */
    @Override
    public Flux<Role> findByNameContainingIgnoreCase(String namePattern) {
        return r2dbcRepository.findByNameContainingIgnoreCase(namePattern)
                .map(entityMapper::toDomain);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>This implementation delegates directly to the R2DBC repository's
     * deleteById method. The operation is idempotent and will complete
     * successfully even if the role doesn't exist.</p>
     */
    @Override
    public Mono<Void> deleteById(Long id) {
        return r2dbcRepository.deleteById(id);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>This implementation delegates directly to the R2DBC repository's
     * count method to get the total number of roles.</p>
     */
    @Override
    public Mono<Long> count() {
        return r2dbcRepository.count();
    }
}