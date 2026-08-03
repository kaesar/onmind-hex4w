package co.onmind.hex4w.infrastructure.events;

import co.onmind.hex4w.application.dto.in.KafkaScriptCommand;
import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import co.onmind.hex4w.application.ports.in.ExecuteScriptTrait;
import co.onmind.hex4w.application.ports.out.EventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Profile("rabbitmq")
public class RabbitMQEventConsumerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQEventConsumerAdapter.class);

    private final ExecuteScriptTrait executeScriptTrait;
    private final EventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;
    private final String resultsExchange;

    public RabbitMQEventConsumerAdapter(
            ExecuteScriptTrait executeScriptTrait,
            EventPublisherPort eventPublisher,
            ObjectMapper objectMapper,
            @Value("${app.rabbitmq.exchange.results:hex4w.script.results}") String resultsExchange) {
        this.executeScriptTrait = executeScriptTrait;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.resultsExchange = resultsExchange;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.script-commands:hex4w.script.commands}")
    public void onScriptCommand(String message) {
        logger.debug("Received RabbitMQ command: {}", message);

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
            eventPublisher.publish(resultsExchange, correlationId, payload);
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
