package co.onmind.hex4w.infrastructure.events;

import co.onmind.hex4w.application.ports.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.concurrent.CompletableFuture;

@Component
@Profile("sns")
public class SnsEventSenderAdapter implements EventPublisherPort {

    private static final Logger logger = LoggerFactory.getLogger(SnsEventSenderAdapter.class);

    private final SnsAsyncClient snsClient;
    private final String defaultTopicArn;

    public SnsEventSenderAdapter(SnsAsyncClient snsClient,
                                 @Value("${app.sns.topic-arn:}") String defaultTopicArn) {
        this.snsClient = snsClient;
        this.defaultTopicArn = defaultTopicArn;
    }

    @Override
    public void publish(String topic, String key, String payload) {
        String targetArn = topic != null && !topic.isBlank() ? topic : defaultTopicArn;
        logger.debug("Publishing to SNS topic={}, key={}", targetArn, key);

        PublishRequest.Builder request = PublishRequest.builder()
                .topicArn(targetArn)
                .message(payload);

        if (key != null && !key.isBlank()) {
            request.messageStructure("json");
        }

        CompletableFuture<PublishResponse> future = snsClient.publish(request.build());
        future.whenComplete((result, error) -> {
            if (error != null) {
                logger.error("Failed to publish SNS message to {}: {}", targetArn, error.getMessage());
                throw new RuntimeException("SNS publish failed", error instanceof SnsException ? error : null);
            }
            logger.debug("SNS message published to {}, messageId={}", targetArn, result.messageId());
        });
    }
}
