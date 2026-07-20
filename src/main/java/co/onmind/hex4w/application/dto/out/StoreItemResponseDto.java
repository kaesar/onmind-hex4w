package co.onmind.hex4w.application.dto.out;

import java.time.LocalDateTime;

public record StoreItemResponseDto(
    String key,
    Long size,
    LocalDateTime lastModified,
    String eTag
) {}