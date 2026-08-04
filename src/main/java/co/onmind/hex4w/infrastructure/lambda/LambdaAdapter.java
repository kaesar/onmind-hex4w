package co.onmind.hex4w.infrastructure.lambda;

import co.onmind.hex4w.application.ports.out.LambdaPort;
import co.onmind.hex4w.transverse.resilience.CircuitBreakerGeneric;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.LambdaException;

@Component
public class LambdaAdapter implements LambdaPort {

    private static final Logger logger = LoggerFactory.getLogger(LambdaAdapter.class);

    private final LambdaAsyncClient lambdaClient;
    private final CircuitBreaker circuitBreaker;

    public LambdaAdapter(LambdaAsyncClient lambdaClient, CircuitBreaker lambdaCircuitBreaker) {
        this.lambdaClient = lambdaClient;
        this.circuitBreaker = lambdaCircuitBreaker;
    }

    @Override
    public Mono<String> invoke(String functionName, String payload) {
        logger.debug("Invoking Lambda function={}, payloadSize={}", functionName, payload.length());

        InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .payload(SdkBytes.fromUtf8String(payload))
                .build();

        return CircuitBreakerGeneric.withCircuitBreaker(
                Mono.fromFuture(lambdaClient.invoke(request))
                    .map(response -> {
                        if (response.functionError() != null) {
                            throw new RuntimeException("Lambda function error: " + response.functionError());
                        }
                        SdkBytes responsePayload = response.payload();
                        String result = responsePayload != null ? responsePayload.asUtf8String() : "";
                        logger.debug("Lambda function={} invoked, responseSize={}", functionName, result.length());
                        return result;
                    })
                    .onErrorMap(
                            error -> error instanceof LambdaException,
                            error -> new RuntimeException("Lambda invocation failed: " + error.getMessage(), error)
                    ),
                circuitBreaker);
    }

    @Override
    public Mono<Void> invokeAsync(String functionName, String payload) {
        logger.debug("Async-invoking Lambda function={}, payloadSize={}", functionName, payload.length());

        InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .invocationType(InvocationType.EVENT)
                .payload(SdkBytes.fromUtf8String(payload))
                .build();

        return CircuitBreakerGeneric.withCircuitBreaker(
                Mono.fromFuture(lambdaClient.invoke(request))
                    .then()
                    .onErrorMap(
                            error -> error instanceof LambdaException,
                            error -> new RuntimeException("Lambda invocation failed: " + error.getMessage(), error)
                    ),
                circuitBreaker);
    }
}
