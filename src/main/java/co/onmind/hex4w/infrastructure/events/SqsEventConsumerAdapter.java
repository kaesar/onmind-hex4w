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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Inbound adapter: polls AWS SQS queues for messages, processes them,
 * and deletes them from the queue after successful processing.
 * Activated with the 'sqs' Spring profile.
 */
@Component
@Profile("sqs")
public class SqsEventConsumerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SqsEventConsumerAdapter.class);

    private final SqsAsyncClient sqsClient;
    private final ExecuteScriptTrait executeScriptTrait;
    private final EventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;
    private final String queueUrl;
    private final String resultsQueueUrl;

    public SqsEventConsumerAdapter(
            SqsAsyncClient sqsClient,
            ExecuteScriptTrait executeScriptTrait,
            EventPublisherPort eventPublisher,
            ObjectMapper objectMapper,
            @Value("${app.sqs.queue-url:}") String queueUrl,
            @Value("${app.sqs.topic.script-results:hex4w.script.results}") String resultsQueueUrl) {
        this.sqsClient = sqsClient;
        this.executeScriptTrait = executeScriptTrait;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
        this.resultsQueueUrl = resultsQueueUrl;
    }

    public Flux<Message> pollMessages() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(20)
                .visibilityTimeout(30)
                .build();

        return Mono.fromFuture(sqsClient.receiveMessage(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(resp -> resp.messages())
                .flatMapMany(Flux::fromIterable)
                .doOnNext(msg -> logger.debug("Received SQS message: id={}", msg.messageId()));
    }

    public Mono<Void> processMessage(Message message) {
        return Mono.fromCallable(() ->
                        objectMapper.readValue(message.body(), KafkaScriptCommand.class))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(command -> processScript(command, message)
                        .onErrorResume(e -> {
                            publishError(command.correlationId(), e.getMessage());
                            return Mono.empty();
                        }))
                .onErrorResume(e -> {
                    logger.error("Failed to deserialize SQS message: {}", e.getMessage());
                    return deleteMessage(message).then(Mono.<ScriptResultResponseDto>empty());
                })
                .then();
    }

    private Mono<ScriptResultResponseDto> processScript(KafkaScriptCommand command, Message message) {
        return executeScriptTrait.executeScript(command.script())
                .flatMap(result -> {
                    publishResult(command.correlationId(), result, null);
                    return deleteMessage(message).thenReturn(result);
                });
    }

    public Mono<Void> deleteMessage(Message message) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build();

        return Mono.fromFuture(sqsClient.deleteMessage(request))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private void publishResult(String correlationId, ScriptResultResponseDto result, String error) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new ScriptResultEnvelope(correlationId, result, error)
            );
            eventPublisher.publish(resultsQueueUrl, correlationId, payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize/publish SQS result", e);
        }
    }

    private void publishError(String correlationId, String errorMessage) {
        publishResult(correlationId, null, errorMessage);
    }

    private record ScriptResultEnvelope(
            String correlationId,
            ScriptResultResponseDto result,
            String error
    ) {}
}
