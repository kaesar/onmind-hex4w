package co.onmind.hex4w.application.dto.in;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kafka command message to execute a whitelisted script file.
 */
public record KafkaScriptCommand(
    @JsonProperty("script") String script,
    @JsonProperty("correlationId") String correlationId
) {}