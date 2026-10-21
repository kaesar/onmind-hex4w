package co.onmind.hex4w.infrastructure.events;

import co.onmind.hex4w.application.dto.in.KafkaScriptCommand;
import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import co.onmind.hex4w.application.ports.in.ExecuteScriptTrait;
import co.onmind.hex4w.application.ports.out.EventPublisherPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsEventConsumerAdapterPollingTest {

    @Mock
    private SqsAsyncClient sqsClient;
    @Mock
    private ExecuteScriptTrait executeScriptTrait;
    @Mock
    private EventPublisherPort eventPublisher;

    private final JsonMapper objectMapper = new JsonMapper();

    private SqsEventConsumerAdapter adapter() {
        return new SqsEventConsumerAdapter(
                sqsClient, executeScriptTrait, eventPublisher, objectMapper,
                "https://sqs.us-east-1.amazonaws.com/123/commands",
                "https://sqs.us-east-1.amazonaws.com/123/results",
                120);
    }

    private Message message(String id, String body) {
        return Message.builder().messageId(id).body(body).receiptHandle("rh-" + id).build();
    }

    @Test
    @DisplayName("the receive request uses the configured visibility timeout")
    void usesConfiguredVisibilityTimeout() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(ReceiveMessageResponse.builder().build()));

        adapter().pollMessages().blockLast(java.time.Duration.ofSeconds(5));

        ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
        verify(sqsClient).receiveMessage(captor.capture());

        // The visibility timeout must outlast the time a script can take, otherwise
        // the message reappears while it is still being processed and the script
        // runs twice in parallel.
        assertEquals(120, captor.getValue().visibilityTimeout());
    }

    @Test
    @DisplayName("processMessage consumes the message and publishes the result")
    void consumesAndPublishes() throws Exception {
        Message message = message("m1", objectMapper.writeValueAsString(
                new KafkaScriptCommand("hello.js", "corr-1")));
        when(executeScriptTrait.executeScript("hello.js"))
                .thenReturn(Mono.just(new ScriptResultResponseDto("out", "", null)));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        adapter().processMessage(message).block();

        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
        verify(eventPublisher).publish(
                eq("https://sqs.us-east-1.amazonaws.com/123/results"),
                eq("corr-1"), anyString());
    }

    @Test
    @DisplayName("a failing script publishes the error AND consumes the message")
    void failingScriptConsumesTheMessage() throws Exception {
        Message message = message("m1", objectMapper.writeValueAsString(
                new KafkaScriptCommand("boom.js", "corr-2")));
        when(executeScriptTrait.executeScript("boom.js"))
                .thenReturn(Mono.error(new RuntimeException("script exploded")));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        adapter().processMessage(message).block();

        verify(eventPublisher).publish(
                eq("https://sqs.us-east-1.amazonaws.com/123/results"),
                eq("corr-2"), anyString());
        // The message is consumed: leaving it in the queue would make it reappear after
        // the visibility timeout and run the script again, forever.
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("the poller consumes the messages it polls")
    void pollerConsumesTheBatch() {
        Message first = message("m1", "{\"script\":\"hello.js\",\"correlationId\":\"c1\"}");
        Message second = message("m2", "{\"script\":\"hello.js\",\"correlationId\":\"c2\"}");
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        ReceiveMessageResponse.builder().messages(first, second).build()));
        when(executeScriptTrait.executeScript("hello.js"))
                .thenReturn(Mono.just(new ScriptResultResponseDto("out", "", null)));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        SqsMessagePoller poller = new SqsMessagePoller(adapter(), 50);
        assertDoesNotThrow(poller::start);
        try {
            await(() -> verify(sqsClient, atLeastOnce()).deleteMessage(any(DeleteMessageRequest.class)));
        } finally {
            poller.stop();
        }

        verify(eventPublisher).publish(
                eq("https://sqs.us-east-1.amazonaws.com/123/results"),
                eq("c1"), anyString());
        verify(eventPublisher).publish(
                eq("https://sqs.us-east-1.amazonaws.com/123/results"),
                eq("c2"), anyString());
    }

    @Test
    @DisplayName("the poller survives a failing poll and keeps going")
    void pollerSurvivesAFailingPoll() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("queue unreachable")));

        SqsMessagePoller poller = new SqsMessagePoller(adapter(), 50);
        assertDoesNotThrow(poller::start);
        try {
            await(() -> verify(sqsClient, atLeastOnce()).receiveMessage(any(ReceiveMessageRequest.class)));
        } finally {
            poller.stop();
        }
    }

    @Test
    @DisplayName("the poller can be stopped")
    void pollerStops() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(ReceiveMessageResponse.builder().build()));

        SqsMessagePoller poller = new SqsMessagePoller(adapter(), 50);
        poller.start();
        assertDoesNotThrow(poller::stop);
    }

    private void await(Runnable assertion) {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                assertion.run();
                return;
            } catch (Throwable retry) {
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        assertion.run();
    }
}
