package co.onmind.hex4w.domain.exceptions;

/**
 * Exception thrown when attempting to create a role that already exists in the system.
 * 
 * This is a domain-specific exception that represents a business rule violation
 * when trying to create a duplicate role, typically based on the role name.
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
public class DuplicateRoleException extends RuntimeException {
    
    /**
     * Constructs a new DuplicateRoleException with the specified detail message.
     * 
     * @param message the detail message
     */
    public DuplicateRoleException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new DuplicateRoleException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public DuplicateRoleException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new DuplicateRoleException for a role with the specified name.
     * 
     * @param roleName the name of the role that already exists
     * @return a new DuplicateRoleException with a formatted message
     */
    public static DuplicateRoleException forName(String roleName) {
        return new DuplicateRoleException("Role with name '" + roleName + "' already exists");
    }
    
    /**
     * Constructs a new DuplicateRoleException for a role with the specified ID.
     * 
     * @param roleId the ID of the role that already exists
     * @return a new DuplicateRoleException with a formatted message
     */
    public static DuplicateRoleException forId(Long roleId) {
        return new DuplicateRoleException("Role with ID " + roleId + " already exists");
    }
}