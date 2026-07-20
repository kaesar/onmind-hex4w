package co.onmind.hex4w.application.dto.out;

public record ScriptResultResponseDto(
    Object value,
    String stdout,
    String stderr
) {}