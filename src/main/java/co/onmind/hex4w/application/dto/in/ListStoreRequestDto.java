package co.onmind.hex4w.application.dto.in;

import jakarta.validation.constraints.NotBlank;

public record ListStoreRequestDto(
    @NotBlank(message = "Bucket name is required") String bucket
) {}