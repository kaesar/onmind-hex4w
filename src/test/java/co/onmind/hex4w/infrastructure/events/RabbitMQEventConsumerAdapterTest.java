package co.onmind.hex4w.infrastructure.events;

import co.onmind.hex4w.application.dto.in.KafkaScriptCommand;
import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import co.onmind.hex4w.application.ports.in.ExecuteScriptTrait;
import co.onmind.hex4w.application.ports.out.EventPublisherPort;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQEventConsumerAdapterTest {

    @Mock
    private ExecuteScriptTrait executeScriptTrait;

    @Mock
    private EventPublisherPort eventPublisher;

    private JsonMapper objectMapper;
    private RabbitMQEventConsumerAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new JsonMapper();
        adapter = new RabbitMQEventConsumerAdapter(
                executeScriptTrait,
                eventPublisher,
                objectMapper,
                "hex4w.script.results"
        );
    }

    @Test
    @DisplayName("onScriptCommand executes script and publishes result on success")
    void executesScriptAndPublishesResult() throws Exception {
        KafkaScriptCommand command = new KafkaScriptCommand("hello.js", "corr-1");
        String message = objectMapper.writeValueAsString(command);

        ScriptResultResponseDto result = new ScriptResultResponseDto("Hello", "", null);
        when(executeScriptTrait.executeScript("hello.js"))
                .thenReturn(Mono.just(result));

        adapter.onScriptCommand(message);

        verify(eventPublisher).publish(
                eq("hex4w.script.results"),
                eq("corr-1"),
                any(String.class)
        );
    }

    @Test
    @DisplayName("onScriptCommand publishes error when script execution fails")
    void publishesErrorOnFailure() throws Exception {
        KafkaScriptCommand command = new KafkaScriptCommand("hello.js", "corr-2");
        String message = objectMapper.writeValueAsString(command);

        when(executeScriptTrait.executeScript("hello.js"))
                .thenReturn(Mono.error(new RuntimeException("Script failed")));

        adapter.onScriptCommand(message);

        verify(eventPublisher).publish(
                eq("hex4w.script.results"),
                eq("corr-2"),
                any(String.class)
        );
    }

    @Test
    @DisplayName("onScriptCommand handles deserialization error gracefully")
    void handlesDeserializationError() {
        String badMessage = "not valid json";

        // The RabbitMQ container requeues a rejected message by default, so a malformed
        // payload is dropped with a log instead of being requeued forever.
        assertDoesNotThrow(() -> adapter.onScriptCommand(badMessage));

        verifyNoInteractions(executeScriptTrait, eventPublisher);
    }

    @Test
    @DisplayName("onScriptCommand waits for the result to be published before returning")
    void waitsForTheResultBeforeReturning() throws Exception {
        KafkaScriptCommand command = new KafkaScriptCommand("hello.js", "corr-3");
        String message = objectMapper.writeValueAsString(command);

        when(executeScriptTrait.executeScript("hello.js"))
                .thenReturn(Mono.just(new ScriptResultResponseDto("Hello", "", null)));

        AtomicBoolean published = new AtomicBoolean(false);
        doAnswer(invocation -> {
            published.set(true);
            return null;
        }).when(eventPublisher).publish(anyString(), anyString(), anyString());

        adapter.onScriptCommand(message);

        assertTrue(published.get(),
                "the listener must not return before the result has been published, "
                        + "otherwise the message is acked while the work is still running");
    }

    @Test
    @DisplayName("onScriptCommand propagates a publication failure so the message is requeued")
    void propagatesPublicationFailure() throws Exception {
        KafkaScriptCommand command = new KafkaScriptCommand("hello.js", "corr-4");
        String message = objectMapper.writeValueAsString(command);

        when(executeScriptTrait.executeScript("hello.js"))
                .thenReturn(Mono.just(new ScriptResultResponseDto("Hello", "", null)));
        doThrow(new RuntimeException("broker down"))
                .when(eventPublisher).publish(anyString(), anyString(), anyString());

        assertThrows(RuntimeException.class, () -> adapter.onScriptCommand(message));
    }
}
