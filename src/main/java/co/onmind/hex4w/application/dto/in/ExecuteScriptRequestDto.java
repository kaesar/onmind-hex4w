package co.onmind.hex4w.application.dto.in;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to execute a whitelisted JavaScript file by name (e.g. {@code hello.js}).
 */
public record ExecuteScriptRequestDto(
    @NotBlank(message = "Script file name is required") String script
) {}
