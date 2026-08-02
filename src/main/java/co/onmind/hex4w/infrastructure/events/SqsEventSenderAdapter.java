package co.onmind.hex4w.infrastructure.events;

import co.onmind.hex4w.application.ports.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Outbound adapter: publishes events to AWS SQS queues.
 * Implements {@link EventPublisherPort} (hexagonal outbound port).
 * Activated with the 'sqs' Spring profile.
 */
@Component
@Profile("sqs")
public class SqsEventSenderAdapter implements EventPublisherPort {

    private static final Logger logger = LoggerFactory.getLogger(SqsEventSenderAdapter.class);

    private final SqsAsyncClient sqsClient;
    private final String queueUrl;

    public SqsEventSenderAdapter(SqsAsyncClient sqsClient,
                                 @Value("${app.sqs.queue-url:}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publish(String topic, String key, String payload) {
        String targetQueue = topic != null && !topic.isBlank() ? topic : queueUrl;
        logger.debug("Publishing to SQS queue={}, key={}", targetQueue, key);

        SendMessageRequest.Builder request = SendMessageRequest.builder()
                .queueUrl(targetQueue)
                .messageBody(payload);

        if (key != null && !key.isBlank()) {
            request.messageAttributes(Map.of("key",
                    MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(key)
                            .build()));
        }

        CompletableFuture<SendMessageResponse> future = sqsClient.sendMessage(request.build());
        future.whenComplete((result, error) -> {
            if (error != null) {
                logger.error("Failed to publish SQS message to {}: {}", targetQueue, error.getMessage());
                throw new RuntimeException("SQS publish failed", error instanceof SqsException ? error : null);
            }
            logger.debug("SQS message sent to {}, messageId={}", targetQueue, result.messageId());
        });
    }
}
