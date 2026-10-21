package co.onmind.hex4w.infrastructure.scripts;

import co.onmind.hex4w.application.ports.out.ScriptServicesPort;
import co.onmind.hex4w.domain.models.ScriptResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GraalJsAdapterTest {

    private GraalJsAdapter adapter;

    @BeforeEach
    void setUp() {
        ScriptServicesPort services = mock(ScriptServicesPort.class);
        adapter = new GraalJsAdapter(services, 5_000_000);
    }

    @Test
    @DisplayName("executes a simple expression and returns its value")
    void executesExpression() {
        StepVerifier.create(adapter.executeScript("1 + 2"))
            .assertNext(result -> assertEquals("3", result.value()))
            .verifyComplete();
    }

    @Test
    @DisplayName("returns null value for console.log-only scripts")
    void nullForNoReturnValue() {
        StepVerifier.create(adapter.executeScript("console.log('hi')"))
            .assertNext(result -> assertNull(result.value()))
            .verifyComplete();
    }

    @Test
    @DisplayName("captures script errors in stderr instead of failing")
    void capturesErrorsInStderr() {
        StepVerifier.create(adapter.executeScript("throw new Error('boom')"))
            .assertNext(result -> {
                assertNull(result.value());
                assertNotNull(result.stderr());
                assertTrue(result.stderr().contains("boom"));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("returns object JSON via toString")
    void returnsObjectToString() {
        StepVerifier.create(adapter.executeScript("JSON.stringify({a: 1})"))
            .assertNext(result -> assertEquals("{\"a\":1}", result.value()))
            .verifyComplete();
    }

    @Test
    @DisplayName("cancels a runaway script instead of spinning forever")
    void cancelsRunawayScript() {
        ScriptServicesPort services = mock(ScriptServicesPort.class);
        GraalJsAdapter limited = new GraalJsAdapter(services, 10_000);

        long start = System.currentTimeMillis();
        StepVerifier.create(limited.executeScript("while (true) { }"))
            .assertNext(result -> {
                assertNull(result.value());
                assertNotNull(result.stderr());
                assertTrue(result.stderr().contains("statement limit"), result.stderr());
            })
            .verifyComplete();

        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 30_000,
                "the runaway script must be cancelled instead of blocking the worker, took " + elapsed + "ms");
    }

    @Test
    @DisplayName("a statement limit of 0 disables the guard")
    void statementLimitCanBeDisabled() {
        ScriptServicesPort services = mock(ScriptServicesPort.class);
        GraalJsAdapter unlimited = new GraalJsAdapter(services, 0);

        StepVerifier.create(unlimited.executeScript("1 + 2"))
            .assertNext(result -> assertEquals("3", result.value()))
            .verifyComplete();
    }
}
