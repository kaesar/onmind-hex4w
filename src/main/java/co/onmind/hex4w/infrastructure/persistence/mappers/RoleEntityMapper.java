package co.onmind.hex4w.infrastructure.persistence.mappers;

import co.onmind.hex4w.domain.models.Role;
import co.onmind.hex4w.infrastructure.persistence.entities.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * MapStruct mapper for converting between Role domain models and RoleEntity persistence entities.
 * 
 * This mapper provides bidirectional conversion between the domain layer (Role) and
 * the persistence layer (RoleEntity). It handles the mapping of all fields and
 * provides additional utility methods for working with reactive streams.
 * 
 * <p>The mapper is automatically implemented by MapStruct at compile time,
 * generating efficient mapping code without reflection.</p>
 * 
 * <p>Special handling is provided for reactive types (Mono/Flux) to support
 * the reactive programming model used throughout the application.</p>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface RoleEntityMapper {
    
    /**
     * Converts a Role domain model to a RoleEntity persistence entity.
     * 
     * This method maps all fields from the domain model to the corresponding
     * entity fields. The mapping is straightforward as both objects have
     * the same field structure.
     * 
     * @param role The domain model to convert. Can be null.
     * @return The corresponding RoleEntity, or null if the input was null.
     */
    RoleEntity toEntity(Role role);
    
    /**
     * Converts a RoleEntity persistence entity to a Role domain model.
     * 
     * This method maps all fields from the entity to the corresponding
     * domain model fields. The mapping preserves all data including
     * the ID and timestamps.
     * 
     * @param entity The persistence entity to convert. Can be null.
     * @return The corresponding Role domain model, or null if the input was null.
     */
    Role toDomain(RoleEntity entity);
    
    /**
     * Updates an existing RoleEntity with data from a Role domain model.
     * 
     * This method is useful for update operations where you want to preserve
     * the existing entity instance but update its fields with new data.
     * The ID field is typically preserved to maintain entity identity.
     * 
     * @param role The source domain model with updated data
     * @param entity The target entity to update
     */
    @Mapping(target = "id", ignore = true) // Preserve existing ID
    void updateEntityFromDomain(Role role, @MappingTarget RoleEntity entity);
    
    /**
     * Converts a Flux of RoleEntity to a Flux of Role domain models.
     * 
     * This utility method provides a convenient way to convert reactive streams
     * of entities to domain models. It applies the toDomain mapping to each
     * element in the stream.
     * 
     * @param entities The Flux of entities to convert
     * @return A Flux of corresponding domain models
     */
    default Flux<Role> toDomainFlux(Flux<RoleEntity> entities) {
        return entities.map(this::toDomain);
    }
    
    /**
     * Converts a Mono of RoleEntity to a Mono of Role domain model.
     * 
     * This utility method provides a convenient way to convert reactive
     * single-value streams of entities to domain models.
     * 
     * @param entity The Mono of entity to convert
     * @return A Mono of the corresponding domain model
     */
    default Mono<Role> toDomainMono(Mono<RoleEntity> entity) {
        return entity.map(this::toDomain);
    }
    
    /**
     * Converts a Flux of Role domain models to a Flux of RoleEntity.
     * 
     * This utility method provides a convenient way to convert reactive streams
     * of domain models to entities. It applies the toEntity mapping to each
     * element in the stream.
     * 
     * @param roles The Flux of domain models to convert
     * @return A Flux of corresponding entities
     */
    default Flux<RoleEntity> toEntityFlux(Flux<Role> roles) {
        return roles.map(this::toEntity);
    }
    
    /**
     * Converts a Mono of Role domain model to a Mono of RoleEntity.
     * 
     * This utility method provides a convenient way to convert reactive
     * single-value streams of domain models to entities.
     * 
     * @param role The Mono of domain model to convert
     * @return A Mono of the corresponding entity
     */
    default Mono<RoleEntity> toEntityMono(Mono<Role> role) {
        return role.map(this::toEntity);
    }
    
    /**
     * Creates a new RoleEntity with the current timestamp.
     * 
     * This utility method is useful when creating new entities that need
     * to have their createdAt field set to the current time.
     * 
     * @param name The name for the new role
     * @return A new RoleEntity with the specified name and current timestamp
     */
    default RoleEntity createNewEntity(String name) {
        RoleEntity entity = new RoleEntity();
        entity.setName(name);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}