package co.onmind.hex4w.infrastructure.events;

import co.onmind.hex4w.application.dto.in.KafkaScriptCommand;
import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import co.onmind.hex4w.application.ports.in.ExecuteScriptTrait;
import co.onmind.hex4w.application.ports.out.EventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import reactor.core.publisher.Mono;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsEventConsumerAdapterTest {

    @Mock
    private SqsAsyncClient sqsClient;

    @Mock
    private ExecuteScriptTrait executeScriptTrait;

    @Mock
    private EventPublisherPort eventPublisher;

    private SqsEventConsumerAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new SqsEventConsumerAdapter(
                sqsClient,
                executeScriptTrait,
                eventPublisher,
                objectMapper,
                "https://sqs.us-east-1.amazonaws.com/123/commands",
                "https://sqs.us-east-1.amazonaws.com/123/results"
        );
    }

    @Test
    @DisplayName("pollMessages emits messages received from SQS")
    void pollMessagesEmitsReceivedMessages() {
        Message msg1 = Message.builder().messageId("m1").body("body1").receiptHandle("rh1").build();
        Message msg2 = Message.builder().messageId("m2").body("body2").receiptHandle("rh2").build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(msg1, msg2)
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        StepVerifier.create(adapter.pollMessages())
                .expectNext(msg1)
                .expectNext(msg2)
                .verifyComplete();

        ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqsClient).receiveMessage(captor.capture());
        assertThat(captor.getValue().queueUrl()).isEqualTo("https://sqs.us-east-1.amazonaws.com/123/commands");
        assertThat(captor.getValue().maxNumberOfMessages()).isEqualTo(10);
        assertThat(captor.getValue().waitTimeSeconds()).isEqualTo(20);
    }

    @Test
    @DisplayName("pollMessages completes empty when no messages")
    void pollMessagesEmptyWhenNoMessages() {
        ReceiveMessageResponse response = ReceiveMessageResponse.builder().build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        StepVerifier.create(adapter.pollMessages())
                .verifyComplete();
    }

    @Test
    @DisplayName("processMessage executes script and deletes message on success")
    void processMessageExecutesAndDeletes() throws Exception {
        KafkaScriptCommand command = new KafkaScriptCommand("hello.js", "corr-1");
        String body = objectMapper.writeValueAsString(command);
        Message message = Message.builder()
                .messageId("m1")
                .body(body)
                .receiptHandle("rh1")
                .build();

        ScriptResultResponseDto result = new ScriptResultResponseDto("output", "stdout", "stderr");

        when(executeScriptTrait.executeScript("hello.js"))
                .thenReturn(Mono.just(result));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(adapter.processMessage(message))
                .verifyComplete();

        verify(executeScriptTrait).executeScript("hello.js");
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
        verify(eventPublisher).publish(
                eq("https://sqs.us-east-1.amazonaws.com/123/results"),
                eq("corr-1"),
                any(String.class)
        );
    }

    @Test
    @DisplayName("processMessage deletes message when deserialization fails")
    void processMessageDeletesOnDeserializationError() {
        Message message = Message.builder()
                .messageId("m1")
                .body("invalid-json")
                .receiptHandle("rh1")
                .build();

        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(adapter.processMessage(message))
                .verifyComplete();

        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
        verifyNoInteractions(executeScriptTrait);
    }

    @Test
    @DisplayName("processMessage publishes error when script execution fails")
    void processMessagePublishesErrorOnFailure() throws Exception {
        KafkaScriptCommand command = new KafkaScriptCommand("hello.js", "corr-2");
        String body = objectMapper.writeValueAsString(command);
        Message message = Message.builder()
                .messageId("m1")
                .body(body)
                .receiptHandle("rh1")
                .build();

        RuntimeException error = new RuntimeException("Script failed");
        when(executeScriptTrait.executeScript("hello.js"))
                .thenReturn(Mono.error(error));

        StepVerifier.create(adapter.processMessage(message))
                .verifyComplete();

        verify(eventPublisher).publish(
                eq("https://sqs.us-east-1.amazonaws.com/123/results"),
                eq("corr-2"),
                any(String.class)
        );
        // Should NOT delete message on execution failure (allows reprocessing)
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("deleteMessage calls SQS deleteMessage with correct queue URL and receipt handle")
    void deleteMessageCallsSqsCorrectly() {
        Message message = Message.builder()
                .messageId("m1")
                .receiptHandle("receipt-handle-123")
                .build();

        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(adapter.deleteMessage(message))
                .verifyComplete();

        ArgumentCaptor<DeleteMessageRequest> captor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient).deleteMessage(captor.capture());
        assertThat(captor.getValue().queueUrl()).isEqualTo("https://sqs.us-east-1.amazonaws.com/123/commands");
        assertThat(captor.getValue().receiptHandle()).isEqualTo("receipt-handle-123");
    }
}
