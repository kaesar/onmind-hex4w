package co.onmind.hex4w.infrastructure.events;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherAdapterTest {

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    @DisplayName("publish sends message to correct topic and key")
    void publishSendsToTopicAndKey() {
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
            .thenReturn(CompletableFuture.completedFuture(new SendResult<>(
                new ProducerRecord<>("hex4w.events", "script-complete", "{\"ok\":true}"),
                null)));

        KafkaEventPublisherAdapter adapter = new KafkaEventPublisherAdapter(kafkaTemplate);
        adapter.publish("hex4w.events", "script-complete", "{\"ok\":true}");

        verify(kafkaTemplate).send("hex4w.events", "script-complete", "{\"ok\":true}");
    }

    @DisplayName("Adapter is @Profile(\"kafka\") gated")
    @Test
    void profileAnnotationPresent() {
        var profile = KafkaEventPublisherAdapter.class.getAnnotation(
            org.springframework.context.annotation.Profile.class);
        assertNotNull(profile);
        assertArrayEquals(new String[]{"kafka"}, profile.value());
    }
}
