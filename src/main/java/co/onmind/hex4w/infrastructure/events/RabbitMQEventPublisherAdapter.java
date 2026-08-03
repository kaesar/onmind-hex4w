package co.onmind.hex4w.infrastructure.events;

import co.onmind.hex4w.application.ports.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("rabbitmq")
public class RabbitMQEventPublisherAdapter implements EventPublisherPort {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQEventPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;
    private final String defaultExchange;

    public RabbitMQEventPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange:default}") String defaultExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.defaultExchange = defaultExchange;
    }

    @Override
    public void publish(String topic, String key, String payload) {
        String targetExchange = topic != null && !topic.isBlank() ? topic : defaultExchange;
        String routingKey = key != null && !key.isBlank() ? key : "";
        logger.debug("Publishing to RabbitMQ exchange={}, routingKey={}, payloadSize={}",
                targetExchange, routingKey, payload.length());

        rabbitTemplate.convertAndSend(targetExchange, routingKey, payload);
    }
}
