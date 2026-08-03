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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQEventConsumerAdapterTest {

    @Mock
    private ExecuteScriptTrait executeScriptTrait;

    @Mock
    private EventPublisherPort eventPublisher;

    private ObjectMapper objectMapper;
    private RabbitMQEventConsumerAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
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

        // Give async subscription time to complete
        Thread.sleep(100);

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

        Thread.sleep(100);

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

        adapter.onScriptCommand(badMessage);

        verifyNoInteractions(executeScriptTrait, eventPublisher);
    }
}
