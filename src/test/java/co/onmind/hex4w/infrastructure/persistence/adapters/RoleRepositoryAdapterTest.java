package co.onmind.hex4w.infrastructure.persistence.adapters;

import co.onmind.hex4w.domain.models.Role;
import co.onmind.hex4w.infrastructure.persistence.entities.RoleEntity;
import co.onmind.hex4w.infrastructure.persistence.mappers.RoleEntityMapper;
import co.onmind.hex4w.infrastructure.persistence.repositories.R2dbcRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RoleRepositoryAdapter.
 * 
 * This test class verifies the adapter's behavior in isolation by mocking
 * its dependencies (R2dbcRoleRepository and RoleEntityMapper). It focuses
 * on testing the adapter's logic for converting between domain models and
 * entities, as well as proper delegation to the underlying repository.
 * 
 * <p>Tests use StepVerifier to verify reactive streams behavior and Mockito
 * for mocking dependencies and verifying interactions.</p>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Role Repository Adapter Tests")
class RoleRepositoryAdapterTest {
    
    @Mock
    private R2dbcRoleRepository r2dbcRepository;
    
    @Mock
    private RoleEntityMapper entityMapper;
    
    @InjectMocks
    private RoleRepositoryAdapter adapter;
    
    private Role testRole;
    private RoleEntity testEntity;
    
    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        testRole = new Role(1L, "ADMIN", now);
        testEntity = new RoleEntity(1L, "ADMIN", now);
    }
    
    @Test
    @DisplayName("Should save role successfully")
    void shouldSaveRoleSuccessfully() {
        // Given
        Role newRole = new Role("NEW_ROLE");
        RoleEntity newEntity = new RoleEntity("NEW_ROLE");
        RoleEntity savedEntity = new RoleEntity(1L, "NEW_ROLE", LocalDateTime.now());
        Role savedRole = new Role(1L, "NEW_ROLE", LocalDateTime.now());
        
        when(entityMapper.toEntity(newRole)).thenReturn(newEntity);
        when(r2dbcRepository.save(any(RoleEntity.class))).thenReturn(Mono.just(savedEntity));
        when(entityMapper.toDomain(savedEntity)).thenReturn(savedRole);
        
        // When & Then
        StepVerifier.create(adapter.save(newRole))
                .assertNext(result -> {
                    assert result.getId().equals(1L);
                    assert result.getName().equals("NEW_ROLE");
                    assert result.getCreatedAt() != null;
                })
                .verifyComplete();
        
        verify(entityMapper).toEntity(newRole);
        verify(r2dbcRepository).save(any(RoleEntity.class));
        verify(entityMapper).toDomain(savedEntity);
    }
    
    @Test
    @DisplayName("Should set createdAt when saving new role")
    void shouldSetCreatedAtWhenSavingNewRole() {
        // Given
        Role newRole = new Role("NEW_ROLE");
        RoleEntity newEntity = new RoleEntity("NEW_ROLE");
        // Entity has no ID and no createdAt (simulating new entity)
        newEntity.setId(null);
        newEntity.setCreatedAt(null);
        
        RoleEntity savedEntity = new RoleEntity(1L, "NEW_ROLE", LocalDateTime.now());
        Role savedRole = new Role(1L, "NEW_ROLE", LocalDateTime.now());
        
        when(entityMapper.toEntity(newRole)).thenReturn(newEntity);
        when(r2dbcRepository.save(any(RoleEntity.class))).thenReturn(Mono.just(savedEntity));
        when(entityMapper.toDomain(savedEntity)).thenReturn(savedRole);
        
        // When & Then
        StepVerifier.create(adapter.save(newRole))
                .assertNext(result -> {
                    assert result.getId().equals(1L);
                    assert result.getName().equals("NEW_ROLE");
                })
                .verifyComplete();
        
        verify(r2dbcRepository).save(any(RoleEntity.class));
    }
    
    @Test
    @DisplayName("Should find role by ID when exists")
    void shouldFindRoleByIdWhenExists() {
        // Given
        when(r2dbcRepository.findById(1L)).thenReturn(Mono.just(testEntity));
        when(entityMapper.toDomain(testEntity)).thenReturn(testRole);
        
        // When & Then
        StepVerifier.create(adapter.findById(1L))
                .assertNext(result -> {
                    assert result.getId().equals(1L);
                    assert result.getName().equals("ADMIN");
                })
                .verifyComplete();
        
        verify(r2dbcRepository).findById(1L);
        verify(entityMapper).toDomain(testEntity);
    }
    
    @Test
    @DisplayName("Should return empty when role not found by ID")
    void shouldReturnEmptyWhenRoleNotFoundById() {
        // Given
        when(r2dbcRepository.findById(999L)).thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(adapter.findById(999L))
                .verifyComplete();
        
        verify(r2dbcRepository).findById(999L);
    }
    
    @Test
    @DisplayName("Should find all roles")
    void shouldFindAllRoles() {
        // Given
        RoleEntity entity1 = new RoleEntity(1L, "ADMIN", LocalDateTime.now());
        RoleEntity entity2 = new RoleEntity(2L, "USER", LocalDateTime.now());
        Role role1 = new Role(1L, "ADMIN", LocalDateTime.now());
        Role role2 = new Role(2L, "USER", LocalDateTime.now());
        
        when(r2dbcRepository.findAll()).thenReturn(Flux.just(entity1, entity2));
        when(entityMapper.toDomain(entity1)).thenReturn(role1);
        when(entityMapper.toDomain(entity2)).thenReturn(role2);
        
        // When & Then
        StepVerifier.create(adapter.findAll())
                .assertNext(result -> {
                    assert result.getName().equals("ADMIN");
                })
                .assertNext(result -> {
                    assert result.getName().equals("USER");
                })
                .verifyComplete();
        
        verify(r2dbcRepository).findAll();
        verify(entityMapper).toDomain(entity1);
        verify(entityMapper).toDomain(entity2);
    }
    
    @Test
    @DisplayName("Should return empty flux when no roles exist")
    void shouldReturnEmptyFluxWhenNoRolesExist() {
        // Given
        when(r2dbcRepository.findAll()).thenReturn(Flux.empty());
        
        // When & Then
        StepVerifier.create(adapter.findAll())
                .verifyComplete();
        
        verify(r2dbcRepository).findAll();
    }
    
    @Test
    @DisplayName("Should check if role exists by name")
    void shouldCheckIfRoleExistsByName() {
        // Given
        when(r2dbcRepository.existsByName("ADMIN")).thenReturn(Mono.just(true));
        when(r2dbcRepository.existsByName("NONEXISTENT")).thenReturn(Mono.just(false));
        
        // When & Then - existing role
        StepVerifier.create(adapter.existsByName("ADMIN"))
                .expectNext(true)
                .verifyComplete();
        
        // When & Then - non-existing role
        StepVerifier.create(adapter.existsByName("NONEXISTENT"))
                .expectNext(false)
                .verifyComplete();
        
        verify(r2dbcRepository).existsByName("ADMIN");
        verify(r2dbcRepository).existsByName("NONEXISTENT");
    }
    
    @Test
    @DisplayName("Should find roles by name containing ignore case")
    void shouldFindRolesByNameContainingIgnoreCase() {
        // Given
        RoleEntity adminEntity = new RoleEntity(1L, "ADMIN", LocalDateTime.now());
        Role adminRole = new Role(1L, "ADMIN", LocalDateTime.now());
        
        when(r2dbcRepository.findByNameContainingIgnoreCase("admin")).thenReturn(Flux.just(adminEntity));
        when(entityMapper.toDomain(adminEntity)).thenReturn(adminRole);
        
        // When & Then
        StepVerifier.create(adapter.findByNameContainingIgnoreCase("admin"))
                .assertNext(result -> {
                    assert result.getName().equals("ADMIN");
                })
                .verifyComplete();
        
        verify(r2dbcRepository).findByNameContainingIgnoreCase("admin");
        verify(entityMapper).toDomain(adminEntity);
    }
    
    @Test
    @DisplayName("Should delete role by ID")
    void shouldDeleteRoleById() {
        // Given
        when(r2dbcRepository.deleteById(1L)).thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(adapter.deleteById(1L))
                .verifyComplete();
        
        verify(r2dbcRepository).deleteById(1L);
    }
    
    @Test
    @DisplayName("Should count roles")
    void shouldCountRoles() {
        // Given
        when(r2dbcRepository.count()).thenReturn(Mono.just(5L));
        
        // When & Then
        StepVerifier.create(adapter.count())
                .expectNext(5L)
                .verifyComplete();
        
        verify(r2dbcRepository).count();
    }
    
    @Test
    @DisplayName("Should handle repository errors gracefully")
    void shouldHandleRepositoryErrorsGracefully() {
        // Given
        RuntimeException repositoryError = new RuntimeException("Database connection failed");
        when(r2dbcRepository.findById(anyLong())).thenReturn(Mono.error(repositoryError));
        
        // When & Then
        StepVerifier.create(adapter.findById(1L))
                .expectError(RuntimeException.class)
                .verify();
        
        verify(r2dbcRepository).findById(1L);
    }
    
    @Test
    @DisplayName("Should handle mapper errors gracefully")
    void shouldHandleMapperErrorsGracefully() {
        // Given
        RuntimeException mapperError = new RuntimeException("Mapping failed");
        when(r2dbcRepository.findById(1L)).thenReturn(Mono.just(testEntity));
        when(entityMapper.toDomain(testEntity)).thenThrow(mapperError);
        
        // When & Then
        StepVerifier.create(adapter.findById(1L))
                .expectError(RuntimeException.class)
                .verify();
        
        verify(r2dbcRepository).findById(1L);
        verify(entityMapper).toDomain(testEntity);
    }
}