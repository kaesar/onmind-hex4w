package co.onmind.hex4w.infrastructure.events;

import co.onmind.hex4w.application.ports.out.EventPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQEventPublisherAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private EventPublisherPort adapter;

    @BeforeEach
    void setUp() {
        adapter = new RabbitMQEventPublisherAdapter(rabbitTemplate, "my-exchange");
    }

    @Test
    @DisplayName("Publish sends message to correct exchange and routing key")
    void publishSendsToCorrectExchange() {
        adapter.publish("my-exchange", "my-routing-key", "{\"script\":\"hello.js\"}");

        verify(rabbitTemplate).convertAndSend(eq("my-exchange"), eq("my-routing-key"), any(String.class));
    }

    @Test
    @DisplayName("Publish uses default exchange when topic is null")
    void publishUsesDefaultExchange() {
        adapter.publish(null, "key", "payload");

        verify(rabbitTemplate).convertAndSend(eq("my-exchange"), eq("key"), eq("payload"));
    }

    @Test
    @DisplayName("Publish uses empty routing key when key is null")
    void publishWithNullKey() {
        adapter.publish("my-exchange", null, "payload");

        verify(rabbitTemplate).convertAndSend(eq("my-exchange"), eq(""), eq("payload"));
    }

    @Test
    @DisplayName("Publish uses default exchange and empty key when both null")
    void publishWithNullTopicAndKey() {
        adapter.publish(null, null, "payload");

        verify(rabbitTemplate).convertAndSend(eq("my-exchange"), eq(""), eq("payload"));
    }
}
