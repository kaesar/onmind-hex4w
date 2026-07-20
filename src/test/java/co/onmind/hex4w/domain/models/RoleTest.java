package co.onmind.hex4w.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Role domain model.
 * 
 * These tests verify the business logic and validation rules
 * implemented in the Role domain model for the reactive WebFlux version.
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
@DisplayName("Role Domain Model Tests")
class RoleTest {
    
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should create role with name constructor")
        void shouldCreateRoleWithNameConstructor() {
            // Given
            String roleName = "ADMIN";
            
            // When
            Role role = new Role(roleName);
            
            // Then
            assertNotNull(role);
            assertEquals(roleName, role.getName());
            assertNotNull(role.getCreatedAt());
            assertNull(role.getId());
        }
        
        @Test
        @DisplayName("Should create role with full constructor")
        void shouldCreateRoleWithFullConstructor() {
            // Given
            Long id = 1L;
            String name = "USER";
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            
            // When
            Role role = new Role(id, name, createdAt);
            
            // Then
            assertNotNull(role);
            assertEquals(id, role.getId());
            assertEquals(name, role.getName());
            assertEquals(createdAt, role.getCreatedAt());
        }
        
        @Test
        @DisplayName("Should create role with default constructor")
        void shouldCreateRoleWithDefaultConstructor() {
            // When
            Role role = new Role();
            
            // Then
            assertNotNull(role);
            assertNull(role.getId());
            assertNull(role.getName());
            assertNull(role.getCreatedAt());
        }
    }
    
    @Nested
    @DisplayName("Name Validation Tests")
    class NameValidationTests {
        
        @Test
        @DisplayName("Should create role with valid names")
        void shouldCreateRoleWithValidNames() {
            // Given & When & Then
            assertDoesNotThrow(() -> new Role("ADMIN"));
            assertDoesNotThrow(() -> new Role("USER_MANAGER"));
            assertDoesNotThrow(() -> new Role("Data-Analyst"));
            assertDoesNotThrow(() -> new Role("Role123"));
            assertDoesNotThrow(() -> new Role("My Role"));
        }
        
        @Test
        @DisplayName("Should create role with null name")
        void shouldCreateRoleWithNullName() {
            // When & Then
            assertDoesNotThrow(() -> new Role(null));
        }
        
        @Test
        @DisplayName("Should create role with empty name")
        void shouldCreateRoleWithEmptyName() {
            // When & Then
            assertDoesNotThrow(() -> new Role(""));
        }
        
        @Test
        @DisplayName("Should create role with any characters")
        void shouldCreateRoleWithAnyCharacters() {
            // When & Then
            assertDoesNotThrow(() -> new Role("ADMIN@ROLE"));
            assertDoesNotThrow(() -> new Role("ROLE$"));
            assertDoesNotThrow(() -> new Role("ROLE%"));
            assertDoesNotThrow(() -> new Role("ROLE!"));
        }
    }
    
    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {
        
        @Test
        @DisplayName("Should allow setting name directly")
        void shouldAllowSettingNameDirectly() {
            // Given
            Role role = new Role("OLD_NAME");
            String newName = "NEW_NAME";
            
            // When
            role.setName(newName);
            
            // Then
            assertEquals(newName, role.getName());
        }
        
        @Test
        @DisplayName("Should allow setting any name value")
        void shouldAllowSettingAnyNameValue() {
            // Given
            Role role = new Role("VALID_NAME");
            
            // When & Then
            assertDoesNotThrow(() -> role.setName(""));
            assertDoesNotThrow(() -> role.setName(null));
            assertDoesNotThrow(() -> role.setName("INVALID@NAME"));
        }
    }
    
    @Nested
    @DisplayName("Equality and Hash Tests")
    class EqualityAndHashTests {
        
        @Test
        @DisplayName("Should be equal when id and name are same")
        void shouldBeEqualWhenIdAndNameAreSame() {
            // Given
            Role role1 = new Role(1L, "ADMIN", LocalDateTime.now());
            Role role2 = new Role(1L, "ADMIN", LocalDateTime.now().plusHours(1));
            
            // When & Then
            assertEquals(role1, role2);
            assertEquals(role1.hashCode(), role2.hashCode());
        }
        
        @Test
        @DisplayName("Should not be equal when names are different")
        void shouldNotBeEqualWhenNamesAreDifferent() {
            // Given
            Role role1 = new Role(1L, "ADMIN", LocalDateTime.now());
            Role role2 = new Role(1L, "USER", LocalDateTime.now());
            
            // When & Then
            assertNotEquals(role1, role2);
        }
        
        @Test
        @DisplayName("Should not be equal when ids are different")
        void shouldNotBeEqualWhenIdsAreDifferent() {
            // Given
            Role role1 = new Role(1L, "ADMIN", LocalDateTime.now());
            Role role2 = new Role(2L, "ADMIN", LocalDateTime.now());
            
            // When & Then
            assertNotEquals(role1, role2);
        }
        
        @Test
        @DisplayName("Should handle null in equals")
        void shouldHandleNullInEquals() {
            // Given
            Role role = new Role("ADMIN");
            
            // When & Then
            assertNotEquals(role, null);
            assertEquals(role, role);
        }
    }
    
    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {
        
        @Test
        @DisplayName("Should generate meaningful toString")
        void shouldGenerateMeaningfulToString() {
            // Given
            Role role = new Role(1L, "ADMIN", LocalDateTime.of(2023, 1, 1, 12, 0));
            
            // When
            String toString = role.toString();
            
            // Then
            assertNotNull(toString);
            assertTrue(toString.contains("Role{"));
            assertTrue(toString.contains("id=1"));
            assertTrue(toString.contains("name='ADMIN'"));
            assertTrue(toString.contains("createdAt="));
        }
    }
    
    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {
        
        @Test
        @DisplayName("Should set and get id correctly")
        void shouldSetAndGetIdCorrectly() {
            // Given
            Role role = new Role();
            Long id = 123L;
            
            // When
            role.setId(id);
            
            // Then
            assertEquals(id, role.getId());
        }
        
        @Test
        @DisplayName("Should set and get name correctly")
        void shouldSetAndGetNameCorrectly() {
            // Given
            Role role = new Role();
            String name = "TEST_ROLE";
            
            // When
            role.setName(name);
            
            // Then
            assertEquals(name, role.getName());
        }
        
        @Test
        @DisplayName("Should set and get createdAt correctly")
        void shouldSetAndGetCreatedAtCorrectly() {
            // Given
            Role role = new Role();
            LocalDateTime createdAt = LocalDateTime.now();
            
            // When
            role.setCreatedAt(createdAt);
            
            // Then
            assertEquals(createdAt, role.getCreatedAt());
        }
    }
}