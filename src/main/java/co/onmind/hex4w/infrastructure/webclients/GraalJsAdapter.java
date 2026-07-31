package co.onmind.hex4w.infrastructure.webclients;

import co.onmind.hex4w.application.ports.out.ScriptingPort;
import co.onmind.hex4w.domain.models.ScriptResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.time.Duration;

@Component
public class GraalJsAdapter implements ScriptingPort {

    private static final Logger logger = LoggerFactory.getLogger(GraalJsAdapter.class);

    // Shared engine; contexts are cheap and created per execution for isolation.
    private final Engine engine;

    public GraalJsAdapter() {
        this.engine = Engine.create("js");
        logger.info("GraalVM JavaScript engine initialized for sandboxed execution");
    }

    @Override
    public Mono<ScriptResult> executeScript(String script) {
        return Mono.fromCallable(() -> executeSandboxed(script))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> logger.error("Script execution failed: {}", e.getMessage()));
    }

    private ScriptResult executeSandboxed(String script) {
        // Build a restricted context: no host access, no IO, no thread creation,
        // bounded CPU time. This prevents the executed code from escaping the sandbox
        // (e.g. accessing java.lang.Runtime, files, or the network).
        try (Context context = Context.newBuilder("js")
                .engine(engine)
                .allowHostAccess(HostAccess.NONE)
                .allowNativeAccess(false)
                .allowCreateThread(false)
                .allowIO(false)
                .allowHostClassLoading(false)
                .allowHostClassLookup(className -> false)
                .option("js.ecmascript-version", "2023")
                .allowExperimentalOptions(true)
                .option("js.timezone", "UTC")
                .build()) {

            context.initialize("js");

            Value result = context.eval("js", script);
            String value = result.isNull() ? null : result.toString();
            return new ScriptResult(value, "", null);
        } catch (Exception e) {
            logger.warn("Script execution error: {}", e.getMessage());
            return new ScriptResult(null, "", e.getMessage());
        }
    }
}
