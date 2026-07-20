package co.onmind.hex4w.infrastructure.webclients.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for notification requests to external services.
 * 
 * This record represents the structure of notification requests sent to
 * external notification services. It encapsulates all the necessary
 * information required to create and send notifications about system events.
 * 
 * <p>The notification request includes:</p>
 * <ul>
 *   <li>Event type for categorizing the notification</li>
 *   <li>Message content for the notification</li>
 *   <li>Source system identification</li>
 *   <li>Reference ID for tracking</li>
 *   <li>Timestamp for event ordering</li>
 * </ul>
 * 
 * @param eventType the type of event that triggered the notification
 * @param message the notification message content
 * @param source the source system or component that generated the event
 * @param referenceId the ID of the entity related to the notification
 * @param timestamp the timestamp when the event occurred
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
public record NotificationRequest(
    
    /**
     * The type of event that triggered this notification.
     * 
     * This field categorizes the notification and helps the notification
     * service route and process the notification appropriately.
     * 
     * Examples: "ROLE_CREATED", "ROLE_UPDATED", "ROLE_DELETED"
     */
    @JsonProperty("eventType")
    @NotBlank(message = "Event type cannot be blank")
    String eventType,
    
    /**
     * The human-readable message content for the notification.
     * 
     * This field contains the actual notification message that will be
     * displayed to users or logged by the notification service.
     */
    @JsonProperty("message")
    @NotBlank(message = "Message cannot be blank")
    String message,
    
    /**
     * The source system or component that generated this notification.
     * 
     * This field identifies which system or component triggered the
     * notification, useful for tracking and debugging purposes.
     */
    @JsonProperty("source")
    @NotBlank(message = "Source cannot be blank")
    String source,
    
    /**
     * The reference ID of the entity related to this notification.
     * 
     * This field provides a reference to the specific entity (e.g., role ID)
     * that the notification is about, enabling correlation and tracking.
     */
    @JsonProperty("referenceId")
    String referenceId,
    
    /**
     * The timestamp when the event occurred.
     * 
     * This field provides temporal information about when the event that
     * triggered the notification actually happened.
     */
    @JsonProperty("timestamp")
    @NotNull(message = "Timestamp cannot be null")
    LocalDateTime timestamp
) {
    
    /**
     * Convenience constructor that automatically sets the timestamp to now.
     * 
     * This constructor is useful when creating notifications for events
     * that are happening in real-time.
     * 
     * @param eventType the type of event
     * @param message the notification message
     * @param source the source system
     * @param referenceId the reference ID
     */
    public NotificationRequest(String eventType, String message, String source, String referenceId) {
        this(eventType, message, source, referenceId, LocalDateTime.now());
    }
    
    /**
     * Creates a role creation notification request.
     * 
     * This factory method creates a standardized notification request
     * for role creation events.
     * 
     * @param roleName the name of the created role
     * @param roleId the ID of the created role
     * @return a NotificationRequest for role creation
     */
    public static NotificationRequest forRoleCreated(String roleName, Long roleId) {
        return new NotificationRequest(
            "ROLE_CREATED",
            String.format("Role '%s' has been created successfully", roleName),
            "hex4j-webflux",
            roleId.toString()
        );
    }
    
    /**
     * Creates a role update notification request.
     * 
     * This factory method creates a standardized notification request
     * for role update events.
     * 
     * @param roleName the name of the updated role
     * @param roleId the ID of the updated role
     * @return a NotificationRequest for role update
     */
    public static NotificationRequest forRoleUpdated(String roleName, Long roleId) {
        return new NotificationRequest(
            "ROLE_UPDATED",
            String.format("Role '%s' has been updated successfully", roleName),
            "hex4j-webflux",
            roleId.toString()
        );
    }
    
    /**
     * Creates a role deletion notification request.
     * 
     * This factory method creates a standardized notification request
     * for role deletion events.
     * 
     * @param roleName the name of the deleted role
     * @param roleId the ID of the deleted role
     * @return a NotificationRequest for role deletion
     */
    public static NotificationRequest forRoleDeleted(String roleName, Long roleId) {
        return new NotificationRequest(
            "ROLE_DELETED",
            String.format("Role '%s' has been deleted successfully", roleName),
            "hex4j-webflux",
            roleId.toString()
        );
    }
}