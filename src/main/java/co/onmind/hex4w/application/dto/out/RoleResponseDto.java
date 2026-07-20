package co.onmind.hex4w.application.dto.out;

import java.time.LocalDateTime;

/**
 * DTO for role response data.
 * 
 * This record represents the output data structure for role information
 * returned by the application. It provides a clean interface for external
 * consumers without exposing internal domain model details.
 * 
 * @param id The unique identifier of the role
 * @param name The name of the role
 * @param createdAt The timestamp when the role was created
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
public record RoleResponseDto(
    Long id,
    String name,
    LocalDateTime createdAt
) {}