package co.onmind.hex4w.application.mappers;

import co.onmind.hex4w.application.dto.in.CreateRoleRequestDto;
import co.onmind.hex4w.application.dto.out.RoleResponseDto;
import co.onmind.hex4w.domain.models.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive mapper for Role entity and DTOs using MapStruct.
 * 
 * This mapper provides conversion methods between domain models and DTOs,
 * including reactive methods for working with Mono and Flux streams.
 * The mapper is configured to work with Spring's component model for
 * dependency injection.
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
@Mapper(componentModel = "spring")
public interface RoleMapper {
    
    /**
     * Converts a Role domain model to RoleResponseDto.
     * 
     * @param role The role domain model
     * @return The role response DTO
     */
    RoleResponseDto toResponseDto(Role role);
    
    /**
     * Converts a CreateRoleRequestDto to Role domain model.
     * The id and createdAt fields will be null and should be set by the domain service.
     * 
     * @param dto The create role request DTO
     * @return The role domain model
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Role toEntity(CreateRoleRequestDto dto);
    
    /**
     * Reactive method to convert a Mono<Role> to Mono<RoleResponseDto>.
     * 
     * @param roleMono The mono containing a role domain model
     * @return The mono containing a role response DTO
     */
    default Mono<RoleResponseDto> toResponseDtoMono(Mono<Role> roleMono) {
        return roleMono.map(this::toResponseDto);
    }
    
    /**
     * Reactive method to convert a Flux<Role> to Flux<RoleResponseDto>.
     * 
     * @param roleFlux The flux containing role domain models
     * @return The flux containing role response DTOs
     */
    default Flux<RoleResponseDto> toResponseDtoFlux(Flux<Role> roleFlux) {
        return roleFlux.map(this::toResponseDto);
    }
    
    /**
     * Reactive method to convert a Mono<CreateRoleRequestDto> to Mono<Role>.
     * 
     * @param requestMono The mono containing a create role request DTO
     * @return The mono containing a role domain model
     */
    default Mono<Role> toEntityMono(Mono<CreateRoleRequestDto> requestMono) {
        return requestMono.map(this::toEntity);
    }
}