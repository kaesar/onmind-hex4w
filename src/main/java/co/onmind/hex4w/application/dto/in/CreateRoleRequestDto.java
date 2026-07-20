package co.onmind.hex4w.application.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new role.
 * 
 * This record represents the input data required to create a new role in the system.
 * It includes validation annotations to ensure data integrity at the application boundary.
 * 
 * @param name The name of the role to be created. Must not be blank and should be between 1 and 100 characters.
 * 
 * @author OnMind (Cesar Andres Arcila Buitrago)
 * @version 1.0.0
 */
public record CreateRoleRequestDto(
    @NotBlank(message = "Role name cannot be blank")
    @Size(min = 1, max = 100, message = "Role name must be between 1 and 100 characters")
    String name
) {}