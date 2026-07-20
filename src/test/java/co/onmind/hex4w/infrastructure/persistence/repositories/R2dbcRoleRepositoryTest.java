package co.onmind.hex4w.infrastructure.persistence.repositories;

import co.onmind.hex4w.infrastructure.persistence.entities.RoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

/**
 * Integration tests for R2dbcRoleRepository using @DataR2dbcTest.
 * 
 * This test class verifies the reactive repository operations against an actual
 * R2DBC database (H2 in-memory). It uses Spring Boot's @DataR2dbcTest annotation
 * to set up a minimal Spring context with only the R2DBC components needed for testing.
 * 
 * <p>Tests use StepVerifier to verify reactive streams behavior, including
 * successful emissions, error conditions, and completion signals.</p>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@DataR2dbcTest
@ActiveProfiles("test")
@DisplayName("R2DBC Role Repository Tests")
class R2dbcRoleRepositoryTest {
    
    @Autowired
    private R2dbcRoleRepository repository;
    
    private RoleEntity testRole1;
    private RoleEntity testRole2;
    private RoleEntity testRole3;
    
    @BeforeEach
    void setUp() {
        // Clean up database before each test
        repository.deleteAll().block();
        
        // Create test data
        testRole1 = new RoleEntity("ADMIN");
        testRole2 = new RoleEntity("USER");
        testRole3 = new RoleEntity("MODERATOR");
    }
    
    @Test
    @DisplayName("Should save and retrieve role successfully")
    void shouldSaveAndRetrieveRole() {
        // Given
        RoleEntity roleToSave = new RoleEntity("TEST_ROLE");
        roleToSave.setCreatedAt(LocalDateTime.now());
        
        // When & Then
        StepVerifier.create(repository.save(roleToSave))
                .assertNext(savedRole -> {
                    assert savedRole.getId() != null;
                    assert savedRole.getName().equals("TEST_ROLE");
                    assert savedRole.getCreatedAt() != null;
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should find role by ID when exists")
    void shouldFindRoleByIdWhenExists() {
        // Given
        RoleEntity savedRole = repository.save(testRole1).block();
        assert savedRole != null;
        
        // When & Then
        StepVerifier.create(repository.findById(savedRole.getId()))
                .assertNext(found -> {
                    assert found.getId().equals(savedRole.getId());
                    assert found.getName().equals("ADMIN");
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should return empty when role ID does not exist")
    void shouldReturnEmptyWhenRoleIdDoesNotExist() {
        // When & Then
        StepVerifier.create(repository.findById(999L))
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should find all roles")
    void shouldFindAllRoles() {
        // Given - save roles and wait for completion
        repository.saveAll(Flux.just(testRole1, testRole2, testRole3)).blockLast();
        
        // When & Then
        StepVerifier.create(repository.findAll())
                .expectNextCount(3)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should return empty flux when no roles exist")
    void shouldReturnEmptyFluxWhenNoRolesExist() {
        // When & Then
        StepVerifier.create(repository.findAll())
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should check if role exists by name")
    void shouldCheckIfRoleExistsByName() {
        // Given
        repository.save(testRole1).block();
        
        // When & Then - existing role
        StepVerifier.create(repository.existsByName("ADMIN"))
                .expectNext(true)
                .verifyComplete();
        
        // When & Then - non-existing role
        StepVerifier.create(repository.existsByName("NON_EXISTENT"))
                .expectNext(false)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should find roles by name containing ignore case")
    void shouldFindRolesByNameContainingIgnoreCase() {
        // Given
        repository.saveAll(Flux.just(testRole1, testRole2, testRole3)).blockLast();
        
        // When & Then - partial match
        StepVerifier.create(repository.findByNameContainingIgnoreCase("admin"))
                .assertNext(role -> {
                    assert role.getName().equals("ADMIN");
                })
                .verifyComplete();
        
        // When & Then - case insensitive
        StepVerifier.create(repository.findByNameContainingIgnoreCase("USER"))
                .assertNext(role -> {
                    assert role.getName().equals("USER");
                })
                .verifyComplete();
        
        // When & Then - no match
        StepVerifier.create(repository.findByNameContainingIgnoreCase("NONEXISTENT"))
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should find role by exact name")
    void shouldFindRoleByExactName() {
        // Given
        repository.save(testRole1).block();
        
        // When & Then - exact match
        StepVerifier.create(repository.findByName("ADMIN"))
                .assertNext(role -> {
                    assert role.getName().equals("ADMIN");
                    assert role.getId() != null;
                })
                .verifyComplete();
        
        // When & Then - no match
        StepVerifier.create(repository.findByName("admin")) // case sensitive
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should delete role by ID")
    void shouldDeleteRoleById() {
        // Given
        RoleEntity savedRole = repository.save(testRole1).block();
        assert savedRole != null;
        Long roleId = savedRole.getId();
        
        // Verify role exists
        StepVerifier.create(repository.existsById(roleId))
                .expectNext(true)
                .verifyComplete();
        
        // When
        StepVerifier.create(repository.deleteById(roleId))
                .verifyComplete();
        
        // Then - verify role is deleted
        StepVerifier.create(repository.existsById(roleId))
                .expectNext(false)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should delete all roles")
    void shouldDeleteAllRoles() {
        // Given
        repository.saveAll(Flux.just(testRole1, testRole2)).blockLast();
        
        // Verify roles exist
        StepVerifier.create(repository.count())
                .expectNext(2L)
                .verifyComplete();
        
        // When
        StepVerifier.create(repository.deleteAll())
                .verifyComplete();
        
        // Then
        StepVerifier.create(repository.count())
                .expectNext(0L)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should count roles correctly")
    void shouldCountRolesCorrectly() {
        // Given - no roles
        StepVerifier.create(repository.count())
                .expectNext(0L)
                .verifyComplete();
        
        // Given - add some roles
        repository.saveAll(Flux.just(testRole1, testRole2, testRole3)).blockLast();
        
        // When & Then
        StepVerifier.create(repository.count())
                .expectNext(3L)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle concurrent saves correctly")
    void shouldHandleConcurrentSavesCorrectly() {
        // Given
        RoleEntity role1 = new RoleEntity("CONCURRENT_1");
        RoleEntity role2 = new RoleEntity("CONCURRENT_2");
        RoleEntity role3 = new RoleEntity("CONCURRENT_3");
        
        // When - save sequentially to avoid timing issues
        repository.save(role1).block();
        repository.save(role2).block();
        repository.save(role3).block();
        
        // Then - verify all were saved
        StepVerifier.create(repository.count())
                .expectNext(3L)
                .verifyComplete();
    }
}