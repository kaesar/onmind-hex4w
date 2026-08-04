package co.onmind.hex4w.application.ports.out;

import reactor.core.publisher.Mono;

public interface LambdaPort {
    Mono<String> invoke(String functionName, String payload);

    /**
     * Fire-and-forget invocation (InvocationType.EVENT).
     * AWS Lambda queues the event and returns immediately — no response payload.
     * Completes when the invoke request is accepted (202 Accepted).
     */
    Mono<Void> invokeAsync(String functionName, String payload);
}
