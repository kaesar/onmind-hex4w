package co.onmind.hex4w.infrastructure.events;

import co.onmind.hex4w.application.ports.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter: publishes events to Kafka topics.
 * Implements {@link EventPublisherPort} (hexagonal outbound port).
 */
@Component
@Profile("kafka")
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisherAdapter.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventPublisherAdapter(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String topic, String key, String payload) {
        logger.debug("Publishing to topic={}, key={}", topic, key);
        kafkaTemplate.send(topic, key, payload);
    }
}