package co.onmind.hex4w.infrastructure.webclients;

import co.onmind.hex4w.infrastructure.webclients.dto.NotificationRequest;
import co.onmind.hex4w.transverse.WebClientGeneric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Web client for notification service integration.
 * 
 * This component provides reactive integration with external notification services,
 * allowing the application to send notifications when roles are created, updated,
 * or deleted. It demonstrates how to integrate with external services in a
 * reactive hexagonal architecture.
 * 
 * <p>Features provided:</p>
 * <ul>
 *   <li>Reactive notification sending</li>
 *   <li>Error handling and retry logic</li>
 *   <li>Structured logging for external calls</li>
 *   <li>Timeout and circuit breaker support</li>
 * </ul>
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 * @since 1.0.0
 */
public class NotificationWebClient {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationWebClient.class);
    
    private final WebClient notificationWebClient;
    private final WebClientGeneric webClientGeneric;
    
    /**
     * Constructs a new NotificationWebClient with the required dependencies.
     * 
     * @param notificationWebClient the configured WebClient for notification service
     * @param webClientGeneric the generic WebClient utility
     */
    public NotificationWebClient(
            @Qualifier("notificationWebClient") WebClient notificationWebClient,
            WebClientGeneric webClientGeneric) {
        this.notificationWebClient = notificationWebClient;
        this.webClientGeneric = webClientGeneric;
    }
    
    /**
     * Sends a role creation notification to the external notification service.
     * 
     * This method sends a notification when a new role is created in the system.
     * It uses reactive programming to ensure non-blocking operation and includes
     * proper error handling and logging.
     * 
     * @param roleName the name of the created role
     * @param roleId the ID of the created role
     * @return a Mono<Void> indicating completion of the notification
     */
    public Mono<Void> sendRoleCreatedNotification(String roleName, Long roleId) {
        logger.info("Sending role created notification for role: {} (ID: {})", roleName, roleId);
        
        NotificationRequest request = new NotificationRequest(
            "ROLE_CREATED",
            String.format("Role '%s' has been created successfully", roleName),
            "system",
            roleId.toString()
        );
        
        return notificationWebClient.post()
            .uri("/api/v1/notifications")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(unused -> 
                logger.info("Role created notification sent successfully for role: {}", roleName))
            .doOnError(error -> 
                logger.error("Failed to send role created notification for role: {} - Error: {}", 
                    roleName, error.getMessage()))
            .onErrorResume(error -> {
                // Log error but don't fail the main operation
                logger.warn("Notification service unavailable, continuing without notification");
                return Mono.empty();
            });
    }
    
    /**
     * Sends a role updated notification to the external notification service.
     * 
     * This method sends a notification when an existing role is updated in the system.
     * 
     * @param roleName the name of the updated role
     * @param roleId the ID of the updated role
     * @return a Mono<Void> indicating completion of the notification
     */
    public Mono<Void> sendRoleUpdatedNotification(String roleName, Long roleId) {
        logger.info("Sending role updated notification for role: {} (ID: {})", roleName, roleId);
        
        NotificationRequest request = new NotificationRequest(
            "ROLE_UPDATED",
            String.format("Role '%s' has been updated successfully", roleName),
            "system",
            roleId.toString()
        );
        
        return notificationWebClient.post()
            .uri("/api/v1/notifications")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(unused -> 
                logger.info("Role updated notification sent successfully for role: {}", roleName))
            .doOnError(error -> 
                logger.error("Failed to send role updated notification for role: {} - Error: {}", 
                    roleName, error.getMessage()))
            .onErrorResume(error -> {
                logger.warn("Notification service unavailable, continuing without notification");
                return Mono.empty();
            });
    }
    
    /**
     * Sends a role deleted notification to the external notification service.
     * 
     * This method sends a notification when a role is deleted from the system.
     * 
     * @param roleName the name of the deleted role
     * @param roleId the ID of the deleted role
     * @return a Mono<Void> indicating completion of the notification
     */
    public Mono<Void> sendRoleDeletedNotification(String roleName, Long roleId) {
        logger.info("Sending role deleted notification for role: {} (ID: {})", roleName, roleId);
        
        NotificationRequest request = new NotificationRequest(
            "ROLE_DELETED",
            String.format("Role '%s' has been deleted successfully", roleName),
            "system",
            roleId.toString()
        );
        
        return notificationWebClient.post()
            .uri("/api/v1/notifications")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(unused -> 
                logger.info("Role deleted notification sent successfully for role: {}", roleName))
            .doOnError(error -> 
                logger.error("Failed to send role deleted notification for role: {} - Error: {}", 
                    roleName, error.getMessage()))
            .onErrorResume(error -> {
                logger.warn("Notification service unavailable, continuing without notification");
                return Mono.empty();
            });
    }
    
    /**
     * Checks the health of the notification service.
     * 
     * This method can be used to verify that the notification service is
     * available and responding to requests.
     * 
     * @return a Mono<Boolean> indicating if the service is healthy
     */
    public Mono<Boolean> checkNotificationServiceHealth() {
        logger.debug("Checking notification service health");
        
        return notificationWebClient.get()
            .uri("/actuator/health")
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> true)
            .doOnSuccess(healthy -> 
                logger.debug("Notification service health check: {}", healthy ? "UP" : "DOWN"))
            .onErrorReturn(false);
    }
}