package co.onmind.hex4w.infrastructure.events;

import co.onmind.hex4w.application.dto.in.KafkaScriptCommand;
import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import co.onmind.hex4w.application.ports.in.ExecuteScriptTrait;
import co.onmind.hex4w.application.ports.out.EventPublisherPort;
import tools.jackson.databind.json.JsonMapper;
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
    private final JsonMapper objectMapper;
    private final String resultsTopic;

    public KafkaEventConsumerAdapter(
            ExecuteScriptTrait executeScriptTrait,
            EventPublisherPort eventPublisher,
            JsonMapper objectMapper,
            @Value("${app.kafka.topic.script-results:hex.script.results}") String resultsTopic) {
        this.executeScriptTrait = executeScriptTrait;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.resultsTopic = resultsTopic;
    }

    @KafkaListener(topics = "${app.kafka.topic.script-commands:hex.script.commands}")
    public void onScriptCommand(String message) {
        logger.debug("Received Kafka command: {}", message);

        KafkaScriptCommand command;
        try {
            command = objectMapper.readValue(message, KafkaScriptCommand.class);
        } catch (RuntimeException e) {
            // Rethrown so the container error handler applies: it retries a few times
            // and then skips the record. Swallowing it here would drop the command
            // silently, with the offset already advanced.
            logger.error("Failed to deserialize command: {}", e.getMessage());
            throw e;
        }

        // The listener MUST NOT return before the work is done. Subscribing and returning
        // would let the container advance the offset while the script is still running:
        // a crash in the middle would lose the command, and a failure to publish the
        // result would be swallowed with nothing left to retry. Blocking the consumer
        // thread is safe here — it is not a Reactor event loop.
        executeScriptTrait.executeScript(command.script())
            .flatMap(result -> {
                publishResult(command.correlationId(), result, null);
                return Mono.just(true);
            })
            .onErrorResume(e -> {
                // A failing script is a business outcome: publish the error and let the
                // offset advance. A failure to publish THAT result propagates instead,
                // so the container retries the whole command.
                logger.error("Script execution failed: {}", e.getMessage());
                publishResult(command.correlationId(), null, e.getMessage());
                return Mono.just(true);
            })
            .block();
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
