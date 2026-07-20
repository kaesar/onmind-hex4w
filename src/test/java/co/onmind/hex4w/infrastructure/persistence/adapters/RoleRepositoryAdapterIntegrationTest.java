package co.onmind.hex4w.infrastructure.persistence.adapters;

import co.onmind.hex4w.domain.models.Role;
import co.onmind.hex4w.infrastructure.persistence.mappers.RoleEntityMapper;
import co.onmind.hex4w.infrastructure.persistence.repositories.R2dbcRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

/**
 * Integration tests for RoleRepositoryAdapter with real R2DBC database.
 * 
 * This test class verifies the complete persistence layer integration including
 * the adapter, mapper, and R2DBC repository working together against a real
 * H2 database. It uses @DataR2dbcTest to set up the R2DBC context and imports
 * the necessary mapper implementation.
 * 
 * <p>These tests verify the end-to-end persistence functionality including
 * proper mapping between domain models and entities, database operations,
 * and reactive stream behavior.</p>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
@DataR2dbcTest
@Import({RoleRepositoryAdapter.class, RoleEntityMapperImpl.class})
@ActiveProfiles("test")
@DisplayName("Role Repository Adapter Integration Tests")
class RoleRepositoryAdapterIntegrationTest {
    
    @Autowired
    private RoleRepositoryAdapter adapter;
    
    @Autowired
    private R2dbcRoleRepository repository;
    
    @BeforeEach
    void setUp() {
        // Clean up database before each test
        repository.deleteAll().block();
    }
    
    @Test
    @DisplayName("Should save and retrieve role with complete mapping")
    void shouldSaveAndRetrieveRoleWithCompleteMapping() {
        // Given
        Role roleToSave = new Role("INTEGRATION_TEST_ROLE");
        
        // When - save role
        StepVerifier.create(adapter.save(roleToSave))
                .assertNext(savedRole -> {
                    assert savedRole.getId() != null;
                    assert savedRole.getName().equals("INTEGRATION_TEST_ROLE");
                    assert savedRole.getCreatedAt() != null;
                })
                .verifyComplete();
        
        // Then - verify it can be retrieved
        StepVerifier.create(adapter.findAll())
                .assertNext(retrievedRole -> {
                    assert retrievedRole.getName().equals("INTEGRATION_TEST_ROLE");
                    assert retrievedRole.getId() != null;
                    assert retrievedRole.getCreatedAt() != null;
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle complete CRUD operations")
    void shouldHandleCompleteCrudOperations() {
        // Create
        Role newRole = new Role("CRUD_TEST_ROLE");
        Role savedRole = adapter.save(newRole).block();
        assert savedRole != null;
        assert savedRole.getId() != null;
        
        Long roleId = savedRole.getId();
        
        // Read
        StepVerifier.create(adapter.findById(roleId))
                .assertNext(foundRole -> {
                    assert foundRole.getId().equals(roleId);
                    assert foundRole.getName().equals("CRUD_TEST_ROLE");
                })
                .verifyComplete();
        
        // Update (save existing role with changes)
        savedRole.setName("UPDATED_CRUD_ROLE");
        StepVerifier.create(adapter.save(savedRole))
                .assertNext(updatedRole -> {
                    assert updatedRole.getId().equals(roleId);
                    assert updatedRole.getName().equals("UPDATED_CRUD_ROLE");
                })
                .verifyComplete();
        
        // Delete
        StepVerifier.create(adapter.deleteById(roleId))
                .verifyComplete();
        
        // Verify deletion
        StepVerifier.create(adapter.findById(roleId))
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle multiple roles with proper mapping")
    void shouldHandleMultipleRolesWithProperMapping() {
        // Given
        Role role1 = new Role("ROLE_1");
        Role role2 = new Role("ROLE_2");
        Role role3 = new Role("ROLE_3");
        
        // When - save multiple roles
        Flux<Role> savedRoles = Flux.just(role1, role2, role3)
                .flatMap(adapter::save);
        
        StepVerifier.create(savedRoles)
                .expectNextCount(3)
                .verifyComplete();
        
        // Then - verify all can be retrieved
        StepVerifier.create(adapter.findAll())
                .expectNextCount(3)
                .verifyComplete();
        
        // Verify count
        StepVerifier.create(adapter.count())
                .expectNext(3L)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle name-based queries correctly")
    void shouldHandleNameBasedQueriesCorrectly() {
        // Given
        Role adminRole = new Role("ADMIN");
        Role userRole = new Role("USER");
        Role moderatorRole = new Role("MODERATOR");
        
        // Save test data
        Flux.just(adminRole, userRole, moderatorRole)
                .flatMap(adapter::save)
                .blockLast();
        
        // Test existsByName
        StepVerifier.create(adapter.existsByName("ADMIN"))
                .expectNext(true)
                .verifyComplete();
        
        StepVerifier.create(adapter.existsByName("NONEXISTENT"))
                .expectNext(false)
                .verifyComplete();
        
        // Test findByNameContainingIgnoreCase
        StepVerifier.create(adapter.findByNameContainingIgnoreCase("admin"))
                .assertNext(role -> {
                    assert role.getName().equals("ADMIN");
                })
                .verifyComplete();
        
        StepVerifier.create(adapter.findByNameContainingIgnoreCase("USER"))
                .assertNext(role -> {
                    assert role.getName().equals("USER");
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should preserve timestamps correctly")
    void shouldPreserveTimestampsCorrectly() {
        // Given
        LocalDateTime beforeSave = LocalDateTime.now().minusSeconds(1);
        Role roleToSave = new Role("TIMESTAMP_TEST");
        
        // When
        Role savedRole = adapter.save(roleToSave).block();
        assert savedRole != null;
        
        LocalDateTime afterSave = LocalDateTime.now().plusSeconds(1);
        
        // Then
        assert savedRole.getCreatedAt() != null;
        assert savedRole.getCreatedAt().isAfter(beforeSave);
        assert savedRole.getCreatedAt().isBefore(afterSave);
        
        // Verify timestamp is preserved on retrieval
        StepVerifier.create(adapter.findById(savedRole.getId()))
                .assertNext(retrievedRole -> {
                    assert retrievedRole.getCreatedAt().equals(savedRole.getCreatedAt());
                })
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle concurrent operations correctly")
    void shouldHandleConcurrentOperationsCorrectly() {
        // Given
        Role role1 = new Role("CONCURRENT_1");
        Role role2 = new Role("CONCURRENT_2");
        Role role3 = new Role("CONCURRENT_3");
        
        // When - perform concurrent saves
        Flux<Role> concurrentSaves = Flux.merge(
                adapter.save(role1),
                adapter.save(role2),
                adapter.save(role3)
        );
        
        // Then
        StepVerifier.create(concurrentSaves)
                .expectNextCount(3)
                .verifyComplete();
        
        // Verify all were saved correctly
        StepVerifier.create(adapter.count())
                .expectNext(3L)
                .verifyComplete();
        
        // Verify all can be retrieved
        StepVerifier.create(adapter.findAll())
                .expectNextCount(3)
                .verifyComplete();
    }
    
    @Test
    @DisplayName("Should handle empty results correctly")
    void shouldHandleEmptyResultsCorrectly() {
        // Test findAll with no data
        StepVerifier.create(adapter.findAll())
                .verifyComplete();
        
        // Test findById with non-existent ID
        StepVerifier.create(adapter.findById(999L))
                .verifyComplete();
        
        // Test count with no data
        StepVerifier.create(adapter.count())
                .expectNext(0L)
                .verifyComplete();
        
        // Test existsByName with no data
        StepVerifier.create(adapter.existsByName("NONEXISTENT"))
                .expectNext(false)
                .verifyComplete();
        
        // Test findByNameContainingIgnoreCase with no data
        StepVerifier.create(adapter.findByNameContainingIgnoreCase("anything"))
                .verifyComplete();
    }
}

/**
 * Stub implementation of RoleEntityMapper for testing.
 * In a real application, this would be generated by MapStruct.
 */
class RoleEntityMapperImpl implements RoleEntityMapper {
    
    @Override
    public co.onmind.hex4w.infrastructure.persistence.entities.RoleEntity toEntity(Role role) {
        if (role == null) return null;
        
        co.onmind.hex4w.infrastructure.persistence.entities.RoleEntity entity =
            new co.onmind.hex4w.infrastructure.persistence.entities.RoleEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        entity.setCreatedAt(role.getCreatedAt());
        return entity;
    }
    
    @Override
    public Role toDomain(co.onmind.hex4w.infrastructure.persistence.entities.RoleEntity entity) {
        if (entity == null) return null;
        
        return new Role(entity.getId(), entity.getName(), entity.getCreatedAt());
    }
    
    @Override
    public void updateEntityFromDomain(Role role, co.onmind.hex4w.infrastructure.persistence.entities.RoleEntity entity) {
        if (role == null || entity == null) return;
        
        entity.setName(role.getName());
        entity.setCreatedAt(role.getCreatedAt());
    }
}