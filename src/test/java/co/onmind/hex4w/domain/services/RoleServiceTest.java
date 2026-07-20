package co.onmind.hex4w.domain.services;

import co.onmind.hex4w.domain.models.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reactive unit tests for the RoleService domain service.
 * 
 * These tests verify the business logic and validation rules
 * implemented in the reactive RoleService using StepVerifier.
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
@DisplayName("RoleService Reactive Domain Service Tests")
class RoleServiceTest {
    
    private RoleService roleService;
    
    @BeforeEach
    void setUp() {
        roleService = new RoleService();
    }
    
    @Nested
    @DisplayName("Create Role Tests")
    class CreateRoleTests {
        
        @Test
        @DisplayName("Should create role with valid name reactively")
        void shouldCreateRoleWithValidNameReactively() {
            // Given
            String roleName = "ADMIN";
            
            // When
            Mono<Role> result = roleService.createRole(roleName);
            
            // Then
            StepVerifier.create(result)
                .assertNext(role -> {
                    assertNotNull(role);
                    assertEquals(roleName, role.getName());
                    assertNotNull(role.getCreatedAt());
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should trim whitespace when creating role reactively")
        void shouldTrimWhitespaceWhenCreatingRoleReactively() {
            // Given
            String roleNameWithSpaces = "  ADMIN  ";
            
            // When
            Mono<Role> result = roleService.createRole(roleNameWithSpaces);
            
            // Then
            StepVerifier.create(result)
                .assertNext(role -> assertEquals("ADMIN", role.getName()))
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should reject null name when creating role reactively")
        void shouldRejectNullNameWhenCreatingRoleReactively() {
            // When
            Mono<Role> result = roleService.createRole(null);
            
            // Then
            StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    "Role name cannot be null or empty".equals(throwable.getMessage()))
                .verify();
        }
        
        @Test
        @DisplayName("Should reject empty name when creating role reactively")
        void shouldRejectEmptyNameWhenCreatingRoleReactively() {
            // When
            Mono<Role> result = roleService.createRole("");
            
            // Then
            StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    "Role name cannot be null or empty".equals(throwable.getMessage()))
                .verify();
        }
        
        @Test
        @DisplayName("Should reject reserved names when creating role reactively")
        void shouldRejectReservedNamesWhenCreatingRoleReactively() {
            // When & Then
            StepVerifier.create(roleService.createRole("SYSTEM"))
                .expectError(IllegalArgumentException.class)
                .verify();
                
            StepVerifier.create(roleService.createRole("ROOT"))
                .expectError(IllegalArgumentException.class)
                .verify();
                
            StepVerifier.create(roleService.createRole("NULL"))
                .expectError(IllegalArgumentException.class)
                .verify();
                
            StepVerifier.create(roleService.createRole("UNDEFINED"))
                .expectError(IllegalArgumentException.class)
                .verify();
        }
        
        @Test
        @DisplayName("Should reject system prefixes when creating role reactively")
        void shouldRejectSystemPrefixesWhenCreatingRoleReactively() {
            // When & Then
            StepVerifier.create(roleService.createRole("SYS_ADMIN"))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("system prefixes"))
                .verify();
                
            StepVerifier.create(roleService.createRole("INTERNAL_USER"))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("system prefixes"))
                .verify();
        }
    }
    
    @Nested
    @DisplayName("Update Role Tests")
    class UpdateRoleTests {
        
        @Test
        @DisplayName("Should update role with valid new name reactively")
        void shouldUpdateRoleWithValidNewNameReactively() {
            // Given
            Role existingRole = new Role("OLD_NAME");
            String newName = "NEW_NAME";
            
            // When
            Mono<Role> result = roleService.updateRole(existingRole, newName);
            
            // Then
            StepVerifier.create(result)
                .assertNext(updatedRole -> {
                    assertSame(existingRole, updatedRole);
                    assertEquals(newName, updatedRole.getName());
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should trim whitespace when updating role reactively")
        void shouldTrimWhitespaceWhenUpdatingRoleReactively() {
            // Given
            Role existingRole = new Role("OLD_NAME");
            String newNameWithSpaces = "  NEW_NAME  ";
            
            // When
            Mono<Role> result = roleService.updateRole(existingRole, newNameWithSpaces);
            
            // Then
            StepVerifier.create(result)
                .assertNext(updatedRole -> assertEquals("NEW_NAME", updatedRole.getName()))
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should reject null existing role when updating reactively")
        void shouldRejectNullExistingRoleWhenUpdatingReactively() {
            // When
            Mono<Role> result = roleService.updateRole(null, "NEW_NAME");
            
            // Then
            StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    "Existing role cannot be null".equals(throwable.getMessage()))
                .verify();
        }
        
        @Test
        @DisplayName("Should reject invalid new name when updating reactively")
        void shouldRejectInvalidNewNameWhenUpdatingReactively() {
            // Given
            Role existingRole = new Role("OLD_NAME");
            
            // When & Then
            StepVerifier.create(roleService.updateRole(existingRole, ""))
                .expectError(IllegalArgumentException.class)
                .verify();
                
            StepVerifier.create(roleService.updateRole(existingRole, "SYSTEM"))
                .expectError(IllegalArgumentException.class)
                .verify();
        }
    }
    
    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {
        
        @Test
        @DisplayName("Should validate role business rules for valid role reactively")
        void shouldValidateRoleBusinessRulesForValidRoleReactively() {
            // Given
            Role validRole = new Role("VALID_ROLE");
            
            // When
            Mono<Role> result = roleService.validateRoleBusinessRules(validRole);
            
            // Then
            StepVerifier.create(result)
                .assertNext(role -> assertSame(validRole, role))
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should reject null role in business rules validation reactively")
        void shouldRejectNullRoleInBusinessRulesValidationReactively() {
            // When
            Mono<Role> result = roleService.validateRoleBusinessRules(null);
            
            // Then
            StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    "Role cannot be null".equals(throwable.getMessage()))
                .verify();
        }
        
        @Test
        @DisplayName("Should reject inactive role in business rules validation reactively")
        void shouldRejectInactiveRoleInBusinessRulesValidationReactively() {
            // Given
            Role inactiveRole = new Role();
            inactiveRole.setName("VALID_NAME");
            // createdAt is null, making it inactive
            
            // When
            Mono<Role> result = roleService.validateRoleBusinessRules(inactiveRole);
            
            // Then
            StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    "Role must be in an active state".equals(throwable.getMessage()))
                .verify();
        }
        
        @Test
        @DisplayName("Should validate role deletion for valid role reactively")
        void shouldValidateRoleDeletionForValidRoleReactively() {
            // Given
            Role validRole = new Role("VALID_ROLE");
            
            // When
            Mono<Void> result = roleService.validateRoleDeletion(validRole);
            
            // Then
            StepVerifier.create(result)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should reject null role in deletion validation reactively")
        void shouldRejectNullRoleInDeletionValidationReactively() {
            // When
            Mono<Void> result = roleService.validateRoleDeletion(null);
            
            // Then
            StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    "Role cannot be null".equals(throwable.getMessage()))
                .verify();
        }
        
        @Test
        @DisplayName("Should reject system role deletion reactively")
        void shouldRejectSystemRoleDeletionReactively() {
            // Given
            Role systemRole = new Role("SYSTEM_ADMIN");
            
            // When
            Mono<Void> result = roleService.validateRoleDeletion(systemRole);
            
            // Then
            StepVerifier.create(result)
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException &&
                    "System roles cannot be deleted".equals(throwable.getMessage()))
                .verify();
        }
    }
    
    @Nested
    @DisplayName("Name Validation Tests")
    class NameValidationTests {
        
        @Test
        @DisplayName("Should return true for valid role names reactively")
        void shouldReturnTrueForValidRoleNamesReactively() {
            // When & Then
            StepVerifier.create(roleService.isValidRoleName("VALID_ROLE"))
                .expectNext(true)
                .verifyComplete();
                
            StepVerifier.create(roleService.isValidRoleName("User-Manager"))
                .expectNext(true)
                .verifyComplete();
                
            StepVerifier.create(roleService.isValidRoleName("Role123"))
                .expectNext(true)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should return false for invalid role names reactively")
        void shouldReturnFalseForInvalidRoleNamesReactively() {
            // When & Then
            StepVerifier.create(roleService.isValidRoleName(null))
                .expectNext(false)
                .verifyComplete();
                
            StepVerifier.create(roleService.isValidRoleName(""))
                .expectNext(false)
                .verifyComplete();
                
            StepVerifier.create(roleService.isValidRoleName("A"))
                .expectNext(false)
                .verifyComplete();
                
            StepVerifier.create(roleService.isValidRoleName("SYSTEM"))
                .expectNext(false)
                .verifyComplete();
                
            StepVerifier.create(roleService.isValidRoleName("SYS_ADMIN"))
                .expectNext(false)
                .verifyComplete();
                
            StepVerifier.create(roleService.isValidRoleName("ROLE@INVALID"))
                .expectNext(false)
                .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Name Normalization Tests")
    class NameNormalizationTests {
        
        @Test
        @DisplayName("Should normalize role name correctly reactively")
        void shouldNormalizeRoleNameCorrectlyReactively() {
            // When & Then
            StepVerifier.create(roleService.normalizeRoleName("  ADMIN  "))
                .expectNext("ADMIN")
                .verifyComplete();
                
            StepVerifier.create(roleService.normalizeRoleName("USER   MANAGER"))
                .expectNext("USER MANAGER")
                .verifyComplete();
                
            StepVerifier.create(roleService.normalizeRoleName("ROLE"))
                .expectNext("ROLE")
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should handle null in normalization reactively")
        void shouldHandleNullInNormalizationReactively() {
            // When & Then
            StepVerifier.create(roleService.normalizeRoleName(null))
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should replace multiple spaces with single space reactively")
        void shouldReplaceMultipleSpacesWithSingleSpaceReactively() {
            // When & Then
            StepVerifier.create(roleService.normalizeRoleName("USER    MANAGER     ROLE"))
                .expectNext("USER MANAGER ROLE")
                .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Active Role Tests")
    class ActiveRoleTests {
        
        @Test
        @DisplayName("Should identify active role reactively")
        void shouldIdentifyActiveRoleReactively() {
            // Given
            Role activeRole = new Role("ACTIVE_ROLE");
            
            // When & Then
            StepVerifier.create(roleService.isRoleActive(activeRole))
                .expectNext(true)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should identify inactive role reactively")
        void shouldIdentifyInactiveRoleReactively() {
            // Given
            Role inactiveRole = new Role();
            inactiveRole.setName("ROLE");
            // createdAt is null
            
            // When & Then
            StepVerifier.create(roleService.isRoleActive(inactiveRole))
                .expectNext(false)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should filter active roles from flux")
        void shouldFilterActiveRolesFromFlux() {
            // Given
            Role activeRole1 = new Role("ACTIVE_1");
            Role activeRole2 = new Role("ACTIVE_2");
            Role inactiveRole = new Role();
            inactiveRole.setName("INACTIVE");
            // inactiveRole has no createdAt
            
            Flux<Role> roles = Flux.just(activeRole1, inactiveRole, activeRole2);
            
            // When
            Flux<Role> result = roleService.findActiveRoles(roles);
            
            // Then
            StepVerifier.create(result)
                .expectNext(activeRole1)
                .expectNext(activeRole2)
                .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle role name at minimum length reactively")
        void shouldHandleRoleNameAtMinimumLengthReactively() {
            // Given
            String minLengthName = "AB";
            
            // When & Then
            StepVerifier.create(roleService.createRole(minLengthName))
                .assertNext(role -> assertEquals(minLengthName, role.getName()))
                .verifyComplete();
                
            StepVerifier.create(roleService.isValidRoleName(minLengthName))
                .expectNext(true)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should handle role name at maximum length reactively")
        void shouldHandleRoleNameAtMaximumLengthReactively() {
            // Given
            String maxLengthName = "A".repeat(50);
            
            // When & Then
            StepVerifier.create(roleService.createRole(maxLengthName))
                .assertNext(role -> assertEquals(maxLengthName, role.getName()))
                .verifyComplete();
                
            StepVerifier.create(roleService.isValidRoleName(maxLengthName))
                .expectNext(true)
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Should handle case insensitive reserved names reactively")
        void shouldHandleCaseInsensitiveReservedNamesReactively() {
            // When & Then
            StepVerifier.create(roleService.createRole("system"))
                .expectError(IllegalArgumentException.class)
                .verify();
                
            StepVerifier.create(roleService.createRole("System"))
                .expectError(IllegalArgumentException.class)
                .verify();
                
            StepVerifier.create(roleService.createRole("SYSTEM"))
                .expectError(IllegalArgumentException.class)
                .verify();
        }
        
        @Test
        @DisplayName("Should handle case insensitive system prefixes reactively")
        void shouldHandleCaseInsensitiveSystemPrefixesReactively() {
            // When & Then
            StepVerifier.create(roleService.createRole("sys_admin"))
                .expectError(IllegalArgumentException.class)
                .verify();
                
            StepVerifier.create(roleService.createRole("Sys_Admin"))
                .expectError(IllegalArgumentException.class)
                .verify();
                
            StepVerifier.create(roleService.createRole("internal_user"))
                .expectError(IllegalArgumentException.class)
                .verify();
        }
    }
}