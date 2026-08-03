package co.onmind.hex4w.infrastructure.lambda;

import co.onmind.hex4w.application.ports.out.LambdaPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LambdaAsyncAdapterTest {

    @Mock
    private LambdaAsyncClient lambdaClient;

    private LambdaPort adapter;

    @BeforeEach
    void setUp() {
        adapter = new LambdaAsyncAdapter(lambdaClient);
    }

    @Test
    @DisplayName("Invoke sends correct request and returns response payload")
    void invokeReturnsPayload() {
        SdkBytes responsePayload = SdkBytes.fromUtf8String("{\"result\":\"ok\"}");
        InvokeResponse mockResponse = InvokeResponse.builder()
                .payload(responsePayload)
                .statusCode(200)
                .build();

        when(lambdaClient.invoke(any(InvokeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(adapter.invoke("my-function", "{\"input\":\"data\"}"))
                .expectNext("{\"result\":\"ok\"}")
                .verifyComplete();

        verify(lambdaClient).invoke(any(InvokeRequest.class));
    }

    @Test
    @DisplayName("Invoke returns empty string when response payload is null")
    void invokeReturnsEmptyWhenPayloadNull() {
        InvokeResponse mockResponse = InvokeResponse.builder()
                .statusCode(206)
                .build();

        when(lambdaClient.invoke(any(InvokeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(adapter.invoke("my-function", "{}"))
                .expectNext("")
                .verifyComplete();
    }

    @Test
    @DisplayName("Invoke throws when Lambda returns functionError")
    void invokeThrowsOnFunctionError() {
        InvokeResponse mockResponse = InvokeResponse.builder()
                .functionError("Unhandled")
                .statusCode(200)
                .payload(SdkBytes.fromUtf8String("{\"errorMessage\":\"fail\"}"))
                .build();

        when(lambdaClient.invoke(any(InvokeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(adapter.invoke("my-function", "{}"))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                        error.getMessage().contains("Lambda function error"))
                .verify();
    }

    @Test
    @DisplayName("Invoke propagates LambdaException")
    void invokeHandlesLambdaException() {
        when(lambdaClient.invoke(any(InvokeRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(
                        LambdaException.builder().message("Access denied").build()));

        StepVerifier.create(adapter.invoke("my-function", "{}"))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                        error.getMessage().contains("Lambda invocation failed"))
                .verify();

        verify(lambdaClient).invoke(any(InvokeRequest.class));
    }

    @Test
    @DisplayName("Invoke sends correct function name and payload")
    void invokeSendsCorrectRequest() {
        InvokeResponse mockResponse = InvokeResponse.builder()
                .payload(SdkBytes.fromUtf8String("ok"))
                .build();
        when(lambdaClient.invoke(any(InvokeRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        adapter.invoke("my-func", "{\"key\":\"value\"}").block();

        var captor = org.mockito.ArgumentCaptor.forClass(InvokeRequest.class);
        verify(lambdaClient).invoke(captor.capture());

        InvokeRequest sent = captor.getValue();
        assertThat(sent.functionName()).isEqualTo("my-func");
        assertThat(sent.payload().asUtf8String()).isEqualTo("{\"key\":\"value\"}");
    }
}
