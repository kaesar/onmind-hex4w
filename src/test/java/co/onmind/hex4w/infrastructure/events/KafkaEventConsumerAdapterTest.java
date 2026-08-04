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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventConsumerAdapterTest {

    @Mock
    ExecuteScriptTrait executeScriptTrait;
    @Mock
    EventPublisherPort eventPublisher;
    @Mock
    ObjectMapper objectMapper;

    private KafkaEventConsumerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaEventConsumerAdapter(
            executeScriptTrait, eventPublisher, objectMapper, "hex4w.script.results");
    }

    @Test
    @DisplayName("onScriptCommand deserializes and executes script, publishes result")
    void consumesAndPublishesResult() throws Exception {
        KafkaScriptCommand command = new KafkaScriptCommand("hello.js", "corr-001");
        ScriptResultResponseDto result = new ScriptResultResponseDto(null, "Hello!", "");

        when(objectMapper.readValue("msg", KafkaScriptCommand.class))
            .thenReturn(command);
        when(executeScriptTrait.executeScript("hello.js"))
            .thenReturn(Mono.just(result));
        when(objectMapper.writeValueAsString(any()))
            .thenReturn("{\"correlationId\":\"corr-001\"}");

        adapter.onScriptCommand("msg");

        verify(objectMapper).readValue("msg", KafkaScriptCommand.class);
        verify(executeScriptTrait).executeScript("hello.js");
        verify(eventPublisher).publish(eq("hex4w.script.results"), eq("corr-001"), anyString());
    }

    @DisplayName("onScriptCommand handles execution errors, publishes error payload")
    @Test
    void handlesExecutionErrors() throws Exception {
        KafkaScriptCommand command = new KafkaScriptCommand("bad.js", "corr-002");

        when(objectMapper.readValue("msg", KafkaScriptCommand.class))
            .thenReturn(command);
        when(executeScriptTrait.executeScript("bad.js"))
            .thenReturn(Mono.error(new RuntimeException("script failed")));
        when(objectMapper.writeValueAsString(any()))
            .thenReturn("error-payload");

        adapter.onScriptCommand("msg");

        verify(eventPublisher).publish(eq("hex4w.script.results"), eq("corr-002"), eq("error-payload"));
        verify(executeScriptTrait).executeScript("bad.js");
    }

    @DisplayName("onScriptCommand swallows deserialization errors gracefully")
    @Test
    void handlesDeserializationError() throws Exception {
        doThrow(new RuntimeException("bad json"))
            .when(objectMapper).readValue(anyString(), eq(KafkaScriptCommand.class));

        // Should not throw — errors are logged and swallowed
        assertDoesNotThrow(() -> adapter.onScriptCommand("bad-json"));
        verify(executeScriptTrait, never()).executeScript(anyString());
    }

    @DisplayName("Adapter is @Profile(\"kafka\") gated")
    @Test
    void profileAnnotationPresent() {
        var profile = KafkaEventConsumerAdapter.class.getAnnotation(
            org.springframework.context.annotation.Profile.class);
        assertNotNull(profile);
        assertArrayEquals(new String[]{"kafka"}, profile.value());
    }
}
