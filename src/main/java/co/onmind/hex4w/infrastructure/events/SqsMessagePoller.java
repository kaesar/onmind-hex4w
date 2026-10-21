package co.onmind.hex4w.infrastructure.events;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Drives the polling loop of the SQS command consumer.
 * <p>
 * {@link SqsEventConsumerAdapter} knows how to receive, process and delete a message, but
 * nothing invoked it: the consumer was dead code. This component owns the loop and its
 * lifecycle, so the adapter stays a plain adapter.
 * <p>
 * The loop is a plain Reactor chain, so no thread is blocked: the SQS call is a long poll
 * ({@code waitTimeSeconds = 20}) handled by the AWS async client, and the subscription is
 * resumed {@code poll-interval-ms} after each batch. Resuming instead of re-subscribing on
 * a timer avoids the burst that a {@code Flux.interval} would produce when a poll takes
 * longer than the interval.
 */
@Component
@Profile("sqs")
public class SqsMessagePoller {

    private static final Logger logger = LoggerFactory.getLogger(SqsMessagePoller.class);

    private final SqsEventConsumerAdapter adapter;
    private final Duration pollInterval;

    private Disposable subscription;

    public SqsMessagePoller(
            SqsEventConsumerAdapter adapter,
            @Value("${app.sqs.poll-interval-ms:1000}") long pollIntervalMs) {
        this.adapter = adapter;
        this.pollInterval = Duration.ofMillis(Math.max(0, pollIntervalMs));
    }

    @PostConstruct
    public void start() {
        subscription = Flux.defer(() -> adapter.pollMessages()
                        .concatMap(adapter::processMessage)
                        .onErrorResume(e -> {
                            // One failed batch must not stop the consumer.
                            logger.error("SQS polling cycle failed: {}", e.getMessage());
                            return Mono.empty();
                        }))
                .repeatWhen(repeat -> repeat.delayElements(pollInterval))
                .subscribe(
                        ignored -> { },
                        e -> logger.error("SQS poller stopped: {}", e.getMessage()));
        logger.info("SQS message poller started (poll interval {}ms)", pollInterval.toMillis());
    }

    @PreDestroy
    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            logger.info("SQS message poller stopped");
        }
    }
}
