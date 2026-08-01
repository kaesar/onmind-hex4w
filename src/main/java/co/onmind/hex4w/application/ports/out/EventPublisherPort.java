package co.onmind.hex4w.application.ports.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface EventPublisherPort {
    void publish(String topic, String key, String payload);
}