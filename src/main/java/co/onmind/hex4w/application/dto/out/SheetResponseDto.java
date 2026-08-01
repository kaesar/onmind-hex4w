package co.onmind.hex4w.application.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SheetResponseDto(
    @JsonProperty("ok") Boolean ok,
    @JsonProperty("status") Integer status,
    @JsonProperty("message") String message,
    @JsonProperty("total") Integer total,
    @JsonProperty("data") Object data
) {}