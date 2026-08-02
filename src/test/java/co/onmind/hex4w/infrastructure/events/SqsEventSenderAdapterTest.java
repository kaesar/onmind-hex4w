package co.onmind.hex4w.infrastructure.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsEventSenderAdapterTest {

    @Mock
    private SqsAsyncClient sqsClient;

    private SqsEventSenderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SqsEventSenderAdapter(sqsClient, "https://sqs.us-east-1.amazonaws.com/123/my-queue");
    }

    @Test
    @DisplayName("Publish sends message to SQS with correct queue URL and body")
    void publishSendsMessageToSqs() {
        SendMessageRequest mockRequest = SendMessageRequest.builder()
                .queueUrl("dummy")
                .messageBody("dummy")
                .build();
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        adapter.publish("my-topic", "my-key", "{\"script\":\"hello.js\",\"correlationId\":\"c1\"}");

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest sent = captor.getValue();
        assertThat(sent.queueUrl()).isEqualTo("my-topic");
        assertThat(sent.messageBody()).isEqualTo("{\"script\":\"hello.js\",\"correlationId\":\"c1\"}");
    }

    @Test
    @DisplayName("Publish uses default queue URL when topic is null")
    void publishUsesDefaultQueueUrl() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        adapter.publish(null, null, "payload");

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        assertThat(captor.getValue().queueUrl()).isEqualTo("https://sqs.us-east-1.amazonaws.com/123/my-queue");
    }

    @Test
    @DisplayName("Publish includes message attributes when key is provided")
    void publishIncludesMessageAttributes() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        adapter.publish("my-topic", "my-key", "payload");

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        assertThat(captor.getValue().messageAttributes()).containsKey("key");
        assertThat(captor.getValue().messageAttributes().get("key").stringValue()).isEqualTo("my-key");
    }

    @Test
    @DisplayName("Publish handles failure gracefully")
    void publishHandlesFailure() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("AWS error")));

        adapter.publish("my-topic", "key", "payload");

        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }
}
