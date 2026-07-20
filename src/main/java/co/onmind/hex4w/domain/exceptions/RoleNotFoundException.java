package co.onmind.hex4w.domain.exceptions;

/**
 * Exception thrown when a requested role is not found in the system.
 * 
 * This is a domain-specific exception that represents a business rule violation
 * when attempting to access a role that doesn't exist.
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
public class RoleNotFoundException extends RuntimeException {
    
    /**
     * Constructs a new RoleNotFoundException with the specified detail message.
     * 
     * @param message the detail message
     */
    public RoleNotFoundException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new RoleNotFoundException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public RoleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new RoleNotFoundException for a role with the specified ID.
     * 
     * @param roleId the ID of the role that was not found
     * @return a new RoleNotFoundException with a formatted message
     */
    public static RoleNotFoundException forId(Long roleId) {
        return new RoleNotFoundException("Role with ID " + roleId + " not found");
    }
    
    /**
     * Constructs a new RoleNotFoundException for a role with the specified name.
     * 
     * @param roleName the name of the role that was not found
     * @return a new RoleNotFoundException with a formatted message
     */
    public static RoleNotFoundException forName(String roleName) {
        return new RoleNotFoundException("Role with name '" + roleName + "' not found");
    }
}