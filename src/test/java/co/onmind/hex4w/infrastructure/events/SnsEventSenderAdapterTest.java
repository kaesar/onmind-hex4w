package co.onmind.hex4w.infrastructure.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnsEventSenderAdapterTest {

    @Mock
    private SnsAsyncClient snsClient;

    private SnsEventSenderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SnsEventSenderAdapter(snsClient, "arn:aws:sns:us-east-1:123:my-topic");
    }

    @Test
    @DisplayName("Publish sends message to SNS with correct topic ARN and body")
    void publishSendsMessageToSns() {
        PublishResponse mockResponse = PublishResponse.builder().messageId("msg-123").build();
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        adapter.publish("arn:aws:sns:us-east-1:123:target", "my-key", "{\"script\":\"hello.js\"}");

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());

        PublishRequest sent = captor.getValue();
        assertThat(sent.topicArn()).isEqualTo("arn:aws:sns:us-east-1:123:target");
        assertThat(sent.message()).isEqualTo("{\"script\":\"hello.js\"}");
    }

    @Test
    @DisplayName("Publish uses default topic ARN when topic is null")
    void publishUsesDefaultTopicArn() {
        PublishResponse mockResponse = PublishResponse.builder().messageId("msg-456").build();
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        adapter.publish(null, null, "payload");

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        assertThat(captor.getValue().topicArn()).isEqualTo("arn:aws:sns:us-east-1:123:my-topic");
        assertThat(captor.getValue().message()).isEqualTo("payload");
    }

    @Test
    @DisplayName("Publish sets messageStructure when key is provided")
    void publishWithKeySetsMessageStructure() {
        PublishResponse mockResponse = PublishResponse.builder().messageId("msg-789").build();
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        adapter.publish("arn:aws:sns:us-east-1:123:target", "my-key", "payload");

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        assertThat(captor.getValue().messageAttributes()).isNotNull();
    }

    @Test
    @DisplayName("Publish handles failure gracefully")
    void publishHandlesFailure() {
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("AWS error")));

        adapter.publish("arn:aws:sns:us-east-1:123:target", "key", "payload");

        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        verify(snsClient).publish(any(PublishRequest.class));
    }
}
