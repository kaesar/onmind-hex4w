package co.onmind.hex4w.application.dto.in;

import jakarta.validation.constraints.NotBlank;

public record ExecuteScriptRequestDto(
    @NotBlank(message = "Script is required") String script
) {}