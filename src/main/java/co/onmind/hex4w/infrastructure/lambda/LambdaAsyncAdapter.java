package co.onmind.hex4w.infrastructure.lambda;

import co.onmind.hex4w.application.ports.out.LambdaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.LambdaException;

@Component
public class LambdaAsyncAdapter implements LambdaPort {

    private static final Logger logger = LoggerFactory.getLogger(LambdaAsyncAdapter.class);

    private final LambdaAsyncClient lambdaClient;

    public LambdaAsyncAdapter(LambdaAsyncClient lambdaClient) {
        this.lambdaClient = lambdaClient;
    }

    @Override
    public Mono<String> invoke(String functionName, String payload) {
        logger.debug("Invoking Lambda function={}, payloadSize={}", functionName, payload.length());

        InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .payload(SdkBytes.fromUtf8String(payload))
                .build();

        return Mono.fromFuture(lambdaClient.invoke(request))
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
                );
    }
}
