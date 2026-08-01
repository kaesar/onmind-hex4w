package co.onmind.hex4w.infrastructure.events;

import co.onmind.hex4w.application.dto.in.KafkaScriptCommand;
import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import co.onmind.hex4w.application.ports.in.ExecuteScriptTrait;
import co.onmind.hex4w.application.ports.out.EventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Inbound adapter: consumes Kafka commands and delegates to the use case.
 * Results are published back via {@link EventPublisherPort}.
 */
@Component
@Profile("kafka")
public class KafkaEventConsumerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventConsumerAdapter.class);

    private final ExecuteScriptTrait executeScriptTrait;
    private final EventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;
    private final String resultsTopic;

    public KafkaEventConsumerAdapter(
            ExecuteScriptTrait executeScriptTrait,
            EventPublisherPort eventPublisher,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topic.script-results:hex4w.script.results}") String resultsTopic) {
        this.executeScriptTrait = executeScriptTrait;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.resultsTopic = resultsTopic;
    }

    @KafkaListener(topics = "${app.kafka.topic.script-commands:hex4w.script.commands}")
    public void onScriptCommand(String message) {
        logger.debug("Received Kafka command: {}", message);

        try {
            KafkaScriptCommand command = objectMapper.readValue(message, KafkaScriptCommand.class);

            executeScriptTrait.executeScript(command.script())
                .flatMap(result -> {
                    publishResult(command.correlationId(), result, null);
                    return Mono.just(true);
                })
                .onErrorResume(e -> {
                    logger.error("Script execution failed: {}", e.getMessage());
                    publishResult(command.correlationId(), null, e.getMessage());
                    return Mono.just(true);
                })
                .subscribe(null, e -> logger.error("Failed to publish result: {}", e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to deserialize command: {}", e.getMessage());
        }
    }

    private void publishResult(String correlationId, ScriptResultResponseDto result, String error) {
        try {
            String payload = objectMapper.writeValueAsString(
                new ScriptResultEnvelope(correlationId, result, error)
            );
            eventPublisher.publish(resultsTopic, correlationId, payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize/publish result", e);
        }
    }

    private record ScriptResultEnvelope(
            String correlationId,
            ScriptResultResponseDto result,
            String error
    ) {}
}