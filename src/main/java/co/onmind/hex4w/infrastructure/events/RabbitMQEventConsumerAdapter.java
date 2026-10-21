package co.onmind.hex4w.infrastructure.events;

import co.onmind.hex4w.application.dto.in.KafkaScriptCommand;
import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import co.onmind.hex4w.application.ports.in.ExecuteScriptTrait;
import co.onmind.hex4w.application.ports.out.EventPublisherPort;
import tools.jackson.databind.json.JsonMapper;
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
    private final JsonMapper objectMapper;
    private final String resultsExchange;

    public RabbitMQEventConsumerAdapter(
            ExecuteScriptTrait executeScriptTrait,
            EventPublisherPort eventPublisher,
            JsonMapper objectMapper,
            @Value("${app.rabbitmq.exchange.results:hex4w.script.results}") String resultsExchange) {
        this.executeScriptTrait = executeScriptTrait;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.resultsExchange = resultsExchange;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.script-commands:hex4w.script.commands}")
    public void onScriptCommand(String message) {
        logger.debug("Received RabbitMQ command: {}", message);

        KafkaScriptCommand command;
        try {
            command = objectMapper.readValue(message, KafkaScriptCommand.class);
        } catch (Exception e) {
            // Unlike Kafka, this is NOT rethrown: the RabbitMQ container requeues a
            // rejected message by default, so a malformed payload would be redelivered
            // forever. The command is dropped with a loud log instead.
            logger.error("Failed to deserialize command, dropping it: {}", e.getMessage());
            return;
        }

        // The listener MUST NOT return before the work is done. Subscribing and returning
        // would ack the message while the script is still running: a crash in the middle
        // would lose the command, and a failure to publish the result would be swallowed
        // with nothing left to retry. Blocking the consumer thread is safe here — it is
        // not a Reactor event loop.
        executeScriptTrait.executeScript(command.script())
            .flatMap(result -> {
                publishResult(command.correlationId(), result, null);
                return Mono.just(true);
            })
            .onErrorResume(e -> {
                // A failing script is a business outcome: publish the error and let the
                // message be acked. A failure to publish THAT result propagates instead,
                // so the container requeues and retries the command.
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
