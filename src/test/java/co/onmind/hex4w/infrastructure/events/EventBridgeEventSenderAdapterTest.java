package co.onmind.hex4w.infrastructure.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventBridgeEventSenderAdapterTest {

    @Mock
    private EventBridgeAsyncClient eventBridgeClient;

    private EventBridgeEventSenderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EventBridgeEventSenderAdapter(eventBridgeClient, "default");
    }

    @Test
    @DisplayName("Publish sends event to EventBridge with correct bus and detail")
    void publishSendsEventToEventBridge() {
        PutEventsResultEntry resultEntry = PutEventsResultEntry.builder().eventId("event-123").build();
        PutEventsResponse mockResult = PutEventsResponse.builder().entries(resultEntry).build();
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResult));

        adapter.publish("custom-bus", "my-key", "{\"script\":\"hello.js\"}");

        ArgumentCaptor<PutEventsRequest> captor = ArgumentCaptor.forClass(PutEventsRequest.class);
        verify(eventBridgeClient).putEvents(captor.capture());

        PutEventsRequest sent = captor.getValue();
        PutEventsRequestEntry entry = sent.entries().get(0);
        assertThat(entry.eventBusName()).isEqualTo("custom-bus");
        assertThat(entry.detailType()).isEqualTo("ScriptExecution");
        assertThat(entry.source()).isEqualTo("hex4w.application");
        assertThat(entry.detail()).isEqualTo("{\"script\":\"hello.js\"}");
    }

    @Test
    @DisplayName("Publish uses default event bus when topic is null")
    void publishUsesDefaultEventBus() {
        PutEventsResultEntry resultEntry = PutEventsResultEntry.builder().eventId("event-456").build();
        PutEventsResponse mockResult = PutEventsResponse.builder().entries(resultEntry).build();
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResult));

        adapter.publish(null, null, "payload");

        ArgumentCaptor<PutEventsRequest> captor = ArgumentCaptor.forClass(PutEventsRequest.class);
        verify(eventBridgeClient).putEvents(captor.capture());
        PutEventsRequestEntry entry = captor.getValue().entries().get(0);
        assertThat(entry.eventBusName()).isEqualTo("default");
    }

    @Test
    @DisplayName("Publish handles failure gracefully")
    void publishHandlesFailure() {
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("AWS error")));

        adapter.publish("custom-bus", "key", "payload");

        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        verify(eventBridgeClient).putEvents(any(PutEventsRequest.class));
    }
}
