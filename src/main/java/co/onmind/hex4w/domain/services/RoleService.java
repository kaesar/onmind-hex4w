package co.onmind.hex4w.domain.services;

import co.onmind.hex4w.domain.models.Role;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reactive domain service for Role business logic.
 * 
 * This service contains the core business rules and validation logic for roles
 * using reactive programming with Mono and Flux. It operates on domain models 
 * and enforces business constraints independent of any infrastructure concerns.
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
@Component
public class RoleService {
    
    /**
     * Creates a new role with the specified name reactively.
     * 
     * This method encapsulates the business logic for role creation,
     * including validation and setting default values.
     * 
     * @param name the role name
     * @return a Mono containing the new Role instance
     */
    public Mono<Role> createRole(String name) {
        return Mono.fromCallable(() -> {
            validateRoleName(name);
            
            Role role = new Role();
            role.setName(name.trim());
            role.setCreatedAt(LocalDateTime.now());
            
            return role;
        })
        .flatMap(this::validateRoleBusinessRules);
    }
    
    /**
     * Updates an existing role with a new name reactively.
     * 
     * @param existingRole the role to update
     * @param newName the new role name
     * @return a Mono containing the updated role
     */
    public Mono<Role> updateRole(Role existingRole, String newName) {
        return Mono.fromCallable(() -> {
            if (existingRole == null) {
                throw new IllegalArgumentException("Existing role cannot be null");
            }
            
            validateRoleName(newName);
            existingRole.setName(newName.trim());
            
            return existingRole;
        })
        .flatMap(this::validateRoleBusinessRules);
    }
    
    /**
     * Validates role business rules reactively.
     * 
     * This method contains domain-specific validation logic that goes beyond
     * simple field validation. It enforces business constraints and rules.
     * 
     * @param role the role to validate
     * @return a Mono containing the validated role
     */
    public Mono<Role> validateRoleBusinessRules(Role role) {
        return Mono.fromCallable(() -> {
            if (role == null) {
                throw new IllegalArgumentException("Role cannot be null");
            }
            
            // Validate the role name using service validation
            validateRoleName(role.getName());
            
            // Additional business rules
            validateRoleNameBusinessRules(role.getName());
            
            // Ensure the role is in a valid state
            if (!isRoleActiveSync(role)) {
                throw new IllegalArgumentException("Role must be in an active state");
            }
            
            return role;
        });
    }
    
    /**
     * Validates that a role can be deleted according to business rules reactively.
     * 
     * @param role the role to validate for deletion
     * @return a Mono that completes if validation passes
     */
    public Mono<Void> validateRoleDeletion(Role role) {
        return Mono.fromRunnable(() -> {
            if (role == null) {
                throw new IllegalArgumentException("Role cannot be null");
            }
            
            // Business rule: Cannot delete system roles
            if (role.getName() != null && role.getName().toUpperCase().contains("SYSTEM")) {
                throw new IllegalArgumentException("System roles cannot be deleted");
            }
        });
    }
    
    /**
     * Checks if a role name is valid according to business rules reactively.
     * 
     * @param name the role name to check
     * @return a Mono containing true if the name is valid, false otherwise
     */
    public Mono<Boolean> isValidRoleName(String name) {
        return Mono.fromCallable(() -> {
            try {
                validateRoleName(name);
                validateRoleNameBusinessRules(name);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        });
    }
    
    /**
     * Normalizes a role name according to business rules reactively.
     * 
     * @param name the role name to normalize
     * @return a Mono containing the normalized role name
     */
    public Mono<String> normalizeRoleName(String name) {
        if (name == null) {
            return Mono.empty();
        }
        
        return Mono.fromCallable(() -> {
            // Trim whitespace and convert to proper case
            String normalized = name.trim();
            
            // Replace multiple spaces with single space
            normalized = normalized.replaceAll("\\s+", " ");
            
            return normalized;
        });
    }
    
    /**
     * Checks if a role is considered active reactively.
     * A role is active if it has been created and has a valid name.
     * 
     * @param role the role to check
     * @return a Mono containing true if the role is active, false otherwise
     */
    public Mono<Boolean> isRoleActive(Role role) {
        return Mono.fromCallable(() -> 
            role != null && 
            role.getName() != null && 
            !role.getName().trim().isEmpty() && 
            role.getCreatedAt() != null
        );
    }
    
    /**
     * Finds active roles from a collection reactively.
     * 
     * @param roles the flux of roles to filter
     * @return a Flux containing only active roles
     */
    public Flux<Role> findActiveRoles(Flux<Role> roles) {
        return roles.filterWhen(this::isRoleActive);
    }
    
    /**
     * Validates role name according to business rules.
     * 
     * @param name the role name to validate
     * @throws IllegalArgumentException if the name violates business rules
     */
    private void validateRoleName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Role name cannot be null or empty");
        }
        
        String trimmedName = name.trim();
        
        if (trimmedName.length() < 2) {
            throw new IllegalArgumentException("Role name must be at least 2 characters long");
        }
        
        if (trimmedName.length() > 50) {
            throw new IllegalArgumentException("Role name cannot exceed 50 characters");
        }
        
        if (!trimmedName.matches("^[a-zA-Z0-9_\\s-]+$")) {
            throw new IllegalArgumentException("Role name can only contain letters, numbers, spaces, hyphens and underscores");
        }
    }
    
    /**
     * Validates role name according to additional business rules.
     * 
     * @param name the role name to validate
     * @throws IllegalArgumentException if the name violates business rules
     */
    private void validateRoleNameBusinessRules(String name) {
        // Check for reserved role names
        List<String> reservedNames = List.of("SYSTEM", "ROOT", "NULL", "UNDEFINED");
        
        if (reservedNames.contains(name.toUpperCase())) {
            throw new IllegalArgumentException("Role name '" + name + "' is reserved and cannot be used");
        }
        
        // Check for inappropriate prefixes
        if (name.toUpperCase().startsWith("SYS_") || name.toUpperCase().startsWith("INTERNAL_")) {
            throw new IllegalArgumentException("Role name cannot start with system prefixes (SYS_, INTERNAL_)");
        }
    }
    
    /**
     * Synchronous version of isRoleActive for internal use.
     * 
     * @param role the role to check
     * @return true if the role is active, false otherwise
     */
    private boolean isRoleActiveSync(Role role) {
        return role != null && 
               role.getName() != null && 
               !role.getName().trim().isEmpty() && 
               role.getCreatedAt() != null;
    }
}