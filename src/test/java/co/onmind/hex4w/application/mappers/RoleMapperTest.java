package co.onmind.hex4w.application.mappers;

import co.onmind.hex4w.application.dto.in.CreateRoleRequestDto;
import co.onmind.hex4w.application.dto.out.RoleResponseDto;
import co.onmind.hex4w.domain.models.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RoleMapper reactive mapping functionality.
 * 
 * This test class verifies the correct mapping between domain models and DTOs,
 * including reactive methods using StepVerifier for testing Mono and Flux streams.
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
class RoleMapperTest {
    
    private RoleMapper roleMapper;
    
    @BeforeEach
    void setUp() {
        roleMapper = Mappers.getMapper(RoleMapper.class);
    }
    
    @Test
    void shouldMapRoleToResponseDto() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now();
        Role role = new Role(1L, "ADMIN", createdAt);
        
        // When
        RoleResponseDto responseDto = roleMapper.toResponseDto(role);
        
        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.id()).isEqualTo(1L);
        assertThat(responseDto.name()).isEqualTo("ADMIN");
        assertThat(responseDto.createdAt()).isEqualTo(createdAt);
    }
    
    @Test
    void shouldMapCreateRequestDtoToRole() {
        // Given
        CreateRoleRequestDto requestDto = new CreateRoleRequestDto("USER");
        
        // When
        Role role = roleMapper.toEntity(requestDto);
        
        // Then
        assertThat(role).isNotNull();
        assertThat(role.getName()).isEqualTo("USER");
        assertThat(role.getId()).isNull(); // Should be ignored in mapping
        assertThat(role.getCreatedAt()).isNull(); // Should be ignored in mapping
    }
    
    @Test
    void shouldMapNullRoleToNullResponseDto() {
        // Given
        Role role = null;
        
        // When
        RoleResponseDto responseDto = roleMapper.toResponseDto(role);
        
        // Then
        assertThat(responseDto).isNull();
    }
    
    @Test
    void shouldMapNullCreateRequestDtoToNullRole() {
        // Given
        CreateRoleRequestDto requestDto = null;
        
        // When
        Role role = roleMapper.toEntity(requestDto);
        
        // Then
        assertThat(role).isNull();
    }
    
    @Test
    void shouldMapMonoRoleToMonoResponseDto() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now();
        Role role = new Role(1L, "ADMIN", createdAt);
        Mono<Role> roleMono = Mono.just(role);
        
        // When
        Mono<RoleResponseDto> responseDtoMono = roleMapper.toResponseDtoMono(roleMono);
        
        // Then
        StepVerifier.create(responseDtoMono)
            .assertNext(responseDto -> {
                assertThat(responseDto.id()).isEqualTo(1L);
                assertThat(responseDto.name()).isEqualTo("ADMIN");
                assertThat(responseDto.createdAt()).isEqualTo(createdAt);
            })
            .verifyComplete();
    }
    
    @Test
    void shouldMapEmptyMonoRoleToEmptyMonoResponseDto() {
        // Given
        Mono<Role> emptyRoleMono = Mono.empty();
        
        // When
        Mono<RoleResponseDto> responseDtoMono = roleMapper.toResponseDtoMono(emptyRoleMono);
        
        // Then
        StepVerifier.create(responseDtoMono)
            .verifyComplete();
    }
    
    @Test
    void shouldMapFluxRoleToFluxResponseDto() {
        // Given
        LocalDateTime createdAt1 = LocalDateTime.now();
        LocalDateTime createdAt2 = LocalDateTime.now().plusMinutes(1);
        Role role1 = new Role(1L, "ADMIN", createdAt1);
        Role role2 = new Role(2L, "USER", createdAt2);
        Flux<Role> roleFlux = Flux.just(role1, role2);
        
        // When
        Flux<RoleResponseDto> responseDtoFlux = roleMapper.toResponseDtoFlux(roleFlux);
        
        // Then
        StepVerifier.create(responseDtoFlux)
            .assertNext(responseDto -> {
                assertThat(responseDto.id()).isEqualTo(1L);
                assertThat(responseDto.name()).isEqualTo("ADMIN");
                assertThat(responseDto.createdAt()).isEqualTo(createdAt1);
            })
            .assertNext(responseDto -> {
                assertThat(responseDto.id()).isEqualTo(2L);
                assertThat(responseDto.name()).isEqualTo("USER");
                assertThat(responseDto.createdAt()).isEqualTo(createdAt2);
            })
            .verifyComplete();
    }
    
    @Test
    void shouldMapEmptyFluxRoleToEmptyFluxResponseDto() {
        // Given
        Flux<Role> emptyRoleFlux = Flux.empty();
        
        // When
        Flux<RoleResponseDto> responseDtoFlux = roleMapper.toResponseDtoFlux(emptyRoleFlux);
        
        // Then
        StepVerifier.create(responseDtoFlux)
            .verifyComplete();
    }
    
    @Test
    void shouldMapMonoCreateRequestDtoToMonoRole() {
        // Given
        CreateRoleRequestDto requestDto = new CreateRoleRequestDto("MANAGER");
        Mono<CreateRoleRequestDto> requestMono = Mono.just(requestDto);
        
        // When
        Mono<Role> roleMono = roleMapper.toEntityMono(requestMono);
        
        // Then
        StepVerifier.create(roleMono)
            .assertNext(role -> {
                assertThat(role.getName()).isEqualTo("MANAGER");
                assertThat(role.getId()).isNull();
                assertThat(role.getCreatedAt()).isNull();
            })
            .verifyComplete();
    }
    
    @Test
    void shouldMapEmptyMonoCreateRequestDtoToEmptyMonoRole() {
        // Given
        Mono<CreateRoleRequestDto> emptyRequestMono = Mono.empty();
        
        // When
        Mono<Role> roleMono = roleMapper.toEntityMono(emptyRequestMono);
        
        // Then
        StepVerifier.create(roleMono)
            .verifyComplete();
    }
    
    @Test
    void shouldHandleErrorInMonoMapping() {
        // Given
        Mono<Role> errorMono = Mono.error(new RuntimeException("Test error"));
        
        // When
        Mono<RoleResponseDto> responseDtoMono = roleMapper.toResponseDtoMono(errorMono);
        
        // Then
        StepVerifier.create(responseDtoMono)
            .expectError(RuntimeException.class)
            .verify();
    }
    
    @Test
    void shouldHandleErrorInFluxMapping() {
        // Given
        Flux<Role> errorFlux = Flux.error(new RuntimeException("Test error"));
        
        // When
        Flux<RoleResponseDto> responseDtoFlux = roleMapper.toResponseDtoFlux(errorFlux);
        
        // Then
        StepVerifier.create(responseDtoFlux)
            .expectError(RuntimeException.class)
            .verify();
    }
}