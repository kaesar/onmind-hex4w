package co.onmind.hex4w.application.usecases;

import co.onmind.hex4w.application.dto.in.CreateRoleRequestDto;
import co.onmind.hex4w.application.dto.out.RoleResponseDto;
import co.onmind.hex4w.application.mappers.RoleMapper;
import co.onmind.hex4w.application.ports.out.RoleRepositoryPort;
import co.onmind.hex4w.domain.exceptions.DuplicateRoleException;
import co.onmind.hex4w.domain.models.Role;
import co.onmind.hex4w.domain.services.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RoleUseCase using reactive testing with StepVerifier.
 * 
 * This test class verifies the reactive behavior of the role use case implementation,
 * including success scenarios, error handling, and edge cases. It uses Mockito for
 * mocking dependencies and StepVerifier for testing reactive streams.
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleUseCase Tests")
class RoleUseCaseImplTest {
    
    @Mock
    private RoleService roleService;
    
    @Mock
    private RoleRepositoryPort roleRepository;
    
    @Mock
    private RoleMapper roleMapper;
    
    private RoleUseCase roleUseCase;
    
    private Role testRole;
    private CreateRoleRequestDto testRequest;
    private RoleResponseDto testResponse;
    
    @BeforeEach
    void setUp() {
        roleUseCase = new RoleUseCase(roleService, roleRepository, roleMapper);
        
        // Setup test data
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("ADMIN");
        testRole.setCreatedAt(LocalDateTime.now());
        
        testRequest = new CreateRoleRequestDto("ADMIN");
        testResponse = new RoleResponseDto(1L, "ADMIN", testRole.getCreatedAt());
    }
    
    @Nested
    @DisplayName("Create Role Tests")
    class CreateRoleTests {
        
        @Test
        @DisplayName("Should create role successfully when all validations pass")
        void shouldCreateRoleSuccessfully() {
            // Given
            when(roleRepository.existsByName("ADMIN")).thenReturn(Mono.just(false));
            when(roleService.createRole("ADMIN")).thenReturn(Mono.just(testRole));
            when(roleRepository.save(testRole)).thenReturn(Mono.just(testRole));
            when(roleMapper.toResponseDto(testRole)).thenReturn(testResponse);
            
            // When & Then
            StepVerifier.create(roleUseCase.createRole(testRequest))
                .expectNext(testResponse)
                .verifyComplete();
            
            // Verify interactions
            verify(roleRepository).existsByName("ADMIN");
            verify(roleService).createRole("ADMIN");
            verify(roleRepository).save(testRole);
            verify(roleMapper).toResponseDto(testRole);
        }
        
        @Test
        @DisplayName("Should fail when request is null")
        void shouldFailWhenRequestIsNull() {
            // When & Then
            StepVerifier.create(roleUseCase.createRole(null))
                .expectError(IllegalArgumentException.class)
                .verify();
            
            // Verify no interactions with dependencies
            verify(roleRepository, never()).existsByName(anyString());
            verify(roleService, never()).createRole(anyString());
            verify(roleRepository, never()).save(any(Role.class));
        }
        
        @Test
        @DisplayName("Should fail when role name already exists")
        void shouldFailWhenRoleNameAlreadyExists() {
            // Given
            when(roleRepository.existsByName("ADMIN")).thenReturn(Mono.just(true));
            
            // When & Then
            StepVerifier.create(roleUseCase.createRole(testRequest))
                .expectError(DuplicateRoleException.class)
                .verify();
            
            // Verify interactions
            verify(roleRepository).existsByName("ADMIN");
            verify(roleService, never()).createRole(anyString());
            verify(roleRepository, never()).save(any(Role.class));
        }
        
        @Test
        @DisplayName("Should handle domain service errors")
        void shouldHandleDomainServiceErrors() {
            // Given
            when(roleRepository.existsByName("ADMIN")).thenReturn(Mono.just(false));
            when(roleService.createRole("ADMIN")).thenReturn(Mono.error(new IllegalArgumentException("Invalid role name")));
            
            // When & Then
            StepVerifier.create(roleUseCase.createRole(testRequest))
                .expectError(IllegalArgumentException.class)
                .verify();
            
            // Verify interactions
            verify(roleRepository).existsByName("ADMIN");
            verify(roleService).createRole("ADMIN");
            verify(roleRepository, never()).save(any(Role.class));
        }
        
        @Test
        @DisplayName("Should handle repository save errors")
        void shouldHandleRepositorySaveErrors() {
            // Given
            when(roleRepository.existsByName("ADMIN")).thenReturn(Mono.just(false));
            when(roleService.createRole("ADMIN")).thenReturn(Mono.just(testRole));
            when(roleRepository.save(testRole)).thenReturn(Mono.error(new RuntimeException("Database error")));
            
            // When & Then
            StepVerifier.create(roleUseCase.createRole(testRequest))
                .expectError(RuntimeException.class)
                .verify();
            
            // Verify interactions
            verify(roleRepository).existsByName("ADMIN");
            verify(roleService).createRole("ADMIN");
            verify(roleRepository).save(testRole);
        }
        
        @Test
        @DisplayName("Should handle repository existence check errors")
        void shouldHandleRepositoryExistenceCheckErrors() {
            // Given
            when(roleRepository.existsByName("ADMIN")).thenReturn(Mono.error(new RuntimeException("Database connection error")));
            
            // When & Then
            StepVerifier.create(roleUseCase.createRole(testRequest))
                .expectError(RuntimeException.class)
                .verify();
            
            // Verify interactions
            verify(roleRepository).existsByName("ADMIN");
            verify(roleService, never()).createRole(anyString());
        }
    }
    
    @Nested
    @DisplayName("Get Role By ID Tests")
    class GetRoleByIdTests {
        
        @Test
        @DisplayName("Should return role when found")
        void shouldReturnRoleWhenFound() {
            // Given
            when(roleRepository.findById(1L)).thenReturn(Mono.just(testRole));
            when(roleMapper.toResponseDto(testRole)).thenReturn(testResponse);
            
            // When & Then
            StepVerifier.create(roleUseCase.getRoleById(1L))
                .expectNext(testResponse)
                .verifyComplete();
            
            // Verify interactions
            verify(roleRepository).findById(1L);
            verify(roleMapper).toResponseDto(testRole);
        }
        
        @Test
        @DisplayName("Should complete empty when role not found")
        void shouldCompleteEmptyWhenRoleNotFound() {
            // Given
            when(roleRepository.findById(1L)).thenReturn(Mono.empty());
            
            // When & Then
            StepVerifier.create(roleUseCase.getRoleById(1L))
                .verifyComplete();
            
            // Verify interactions
            verify(roleRepository).findById(1L);
            verify(roleMapper, never()).toResponseDto(any(Role.class));
        }
        
        @Test
        @DisplayName("Should fail when ID is null")
        void shouldFailWhenIdIsNull() {
            // When & Then
            StepVerifier.create(roleUseCase.getRoleById(null))
                .expectError(IllegalArgumentException.class)
                .verify();
            
            // Verify no interactions
            verify(roleRepository, never()).findById(any());
        }
        
        @Test
        @DisplayName("Should fail when ID is zero or negative")
        void shouldFailWhenIdIsZeroOrNegative() {
            // Test with zero
            StepVerifier.create(roleUseCase.getRoleById(0L))
                .expectError(IllegalArgumentException.class)
                .verify();
            
            // Test with negative
            StepVerifier.create(roleUseCase.getRoleById(-1L))
                .expectError(IllegalArgumentException.class)
                .verify();
            
            // Verify no interactions
            verify(roleRepository, never()).findById(any());
        }
        
        @Test
        @DisplayName("Should handle repository errors")
        void shouldHandleRepositoryErrors() {
            // Given
            when(roleRepository.findById(1L)).thenReturn(Mono.error(new RuntimeException("Database error")));
            
            // When & Then
            StepVerifier.create(roleUseCase.getRoleById(1L))
                .expectError(RuntimeException.class)
                .verify();
            
            // Verify interactions
            verify(roleRepository).findById(1L);
        }
    }
    
    @Nested
    @DisplayName("Get All Roles Tests")
    class GetAllRolesTests {
        
        @Test
        @DisplayName("Should return all roles when they exist")
        void shouldReturnAllRolesWhenTheyExist() {
            // Given
            Role role2 = new Role();
            role2.setId(2L);
            role2.setName("USER");
            role2.setCreatedAt(LocalDateTime.now());
            
            RoleResponseDto response2 = new RoleResponseDto(2L, "USER", role2.getCreatedAt());
            
            when(roleRepository.findAll()).thenReturn(Flux.just(testRole, role2));
            when(roleMapper.toResponseDto(testRole)).thenReturn(testResponse);
            when(roleMapper.toResponseDto(role2)).thenReturn(response2);
            
            // When & Then
            StepVerifier.create(roleUseCase.getAllRoles())
                .expectNext(testResponse)
                .expectNext(response2)
                .verifyComplete();
            
            // Verify interactions
            verify(roleRepository).findAll();
            verify(roleMapper).toResponseDto(testRole);
            verify(roleMapper).toResponseDto(role2);
        }
        
        @Test
        @DisplayName("Should complete empty when no roles exist")
        void shouldCompleteEmptyWhenNoRolesExist() {
            // Given
            when(roleRepository.findAll()).thenReturn(Flux.empty());
            
            // When & Then
            StepVerifier.create(roleUseCase.getAllRoles())
                .verifyComplete();
            
            // Verify interactions
            verify(roleRepository).findAll();
            verify(roleMapper, never()).toResponseDto(any(Role.class));
        }
        
        @Test
        @DisplayName("Should handle repository errors")
        void shouldHandleRepositoryErrors() {
            // Given
            when(roleRepository.findAll()).thenReturn(Flux.error(new RuntimeException("Database error")));
            
            // When & Then
            StepVerifier.create(roleUseCase.getAllRoles())
                .expectError(RuntimeException.class)
                .verify();
            
            // Verify interactions
            verify(roleRepository).findAll();
        }
        
        @Test
        @DisplayName("Should handle mapper errors")
        void shouldHandleMapperErrors() {
            // Given
            when(roleRepository.findAll()).thenReturn(Flux.just(testRole));
            when(roleMapper.toResponseDto(testRole)).thenThrow(new RuntimeException("Mapping error"));
            
            // When & Then
            StepVerifier.create(roleUseCase.getAllRoles())
                .expectError(RuntimeException.class)
                .verify();
            
            // Verify interactions
            verify(roleRepository).findAll();
            verify(roleMapper).toResponseDto(testRole);
        }
    }
    
    @Nested
    @DisplayName("Get Roles By Name Pattern Tests")
    class GetRolesByNamePatternTests {
        
        @Test
        @DisplayName("Should return matching roles when pattern matches")
        void shouldReturnMatchingRolesWhenPatternMatches() {
            // Given
            String pattern = "ADM";
            when(roleRepository.findByNameContainingIgnoreCase("ADM")).thenReturn(Flux.just(testRole));
            when(roleMapper.toResponseDto(testRole)).thenReturn(testResponse);
            
            // When & Then
            StepVerifier.create(roleUseCase.getRolesByNamePattern(pattern))
                .expectNext(testResponse)
                .verifyComplete();
            
            // Verify interactions
            verify(roleRepository).findByNameContainingIgnoreCase("ADM");
            verify(roleMapper).toResponseDto(testRole);
        }
        
        @Test
        @DisplayName("Should complete empty when no roles match pattern")
        void shouldCompleteEmptyWhenNoRolesMatchPattern() {
            // Given
            String pattern = "NONEXISTENT";
            when(roleRepository.findByNameContainingIgnoreCase("NONEXISTENT")).thenReturn(Flux.empty());
            
            // When & Then
            StepVerifier.create(roleUseCase.getRolesByNamePattern(pattern))
                .verifyComplete();
            
            // Verify interactions
            verify(roleRepository).findByNameContainingIgnoreCase("NONEXISTENT");
            verify(roleMapper, never()).toResponseDto(any(Role.class));
        }
        
        @Test
        @DisplayName("Should fail when pattern is null")
        void shouldFailWhenPatternIsNull() {
            // When & Then
            StepVerifier.create(roleUseCase.getRolesByNamePattern(null))
                .expectError(IllegalArgumentException.class)
                .verify();
            
            // Verify no interactions
            verify(roleRepository, never()).findByNameContainingIgnoreCase(anyString());
        }
        
        @Test
        @DisplayName("Should fail when pattern is empty")
        void shouldFailWhenPatternIsEmpty() {
            // When & Then
            StepVerifier.create(roleUseCase.getRolesByNamePattern(""))
                .expectError(IllegalArgumentException.class)
                .verify();
            
            // Verify no interactions
            verify(roleRepository, never()).findByNameContainingIgnoreCase(anyString());
        }
        
        @Test
        @DisplayName("Should trim pattern before searching")
        void shouldTrimPatternBeforeSearching() {
            // Given
            String pattern = "  ADM  ";
            when(roleRepository.findByNameContainingIgnoreCase("ADM")).thenReturn(Flux.just(testRole));
            when(roleMapper.toResponseDto(testRole)).thenReturn(testResponse);
            
            // When & Then
            StepVerifier.create(roleUseCase.getRolesByNamePattern(pattern))
                .expectNext(testResponse)
                .verifyComplete();
            
            // Verify interactions - should be called with trimmed pattern
            verify(roleRepository).findByNameContainingIgnoreCase("ADM");
        }
        
        @Test
        @DisplayName("Should handle repository errors")
        void shouldHandleRepositoryErrors() {
            // Given
            String pattern = "ADM";
            when(roleRepository.findByNameContainingIgnoreCase("ADM"))
                .thenReturn(Flux.error(new RuntimeException("Database error")));
            
            // When & Then
            StepVerifier.create(roleUseCase.getRolesByNamePattern(pattern))
                .expectError(RuntimeException.class)
                .verify();
            
            // Verify interactions
            verify(roleRepository).findByNameContainingIgnoreCase("ADM");
        }
    }
    
    @Nested
    @DisplayName("Private Method Tests")
    class PrivateMethodTests {
        
        @Test
        @DisplayName("Should validate unique role name successfully when name is unique")
        void shouldValidateUniqueRoleNameSuccessfully() {
            // Given
            CreateRoleRequestDto requestWithUniqueName = new CreateRoleRequestDto("UNIQUE_ROLE");
            when(roleRepository.existsByName("UNIQUE_ROLE")).thenReturn(Mono.just(false));
            when(roleService.createRole("UNIQUE_ROLE")).thenReturn(Mono.just(testRole));
            when(roleRepository.save(testRole)).thenReturn(Mono.just(testRole));
            when(roleMapper.toResponseDto(testRole)).thenReturn(testResponse);
            
            // When & Then
            StepVerifier.create(roleUseCase.createRole(requestWithUniqueName))
                .expectNext(testResponse)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should handle domain exception mapping correctly")
        void shouldHandleDomainExceptionMappingCorrectly() {
            // Given
            when(roleRepository.existsByName("ADMIN")).thenReturn(Mono.just(false));
            when(roleService.createRole("ADMIN")).thenReturn(Mono.error(new IllegalArgumentException("Domain validation error")));
            
            // When & Then
            StepVerifier.create(roleUseCase.createRole(testRequest))
                .expectError(IllegalArgumentException.class)
                .verify();
        }
        
        @Test
        @DisplayName("Should wrap unexpected exceptions")
        void shouldWrapUnexpectedExceptions() {
            // Given
            when(roleRepository.existsByName("ADMIN")).thenReturn(Mono.just(false));
            when(roleService.createRole("ADMIN")).thenReturn(Mono.error(new NullPointerException("Unexpected error")));
            
            // When & Then
            StepVerifier.create(roleUseCase.createRole(testRequest))
                .expectError(RuntimeException.class)
                .verify();
        }
    }
}