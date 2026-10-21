package co.onmind.hex4w.infrastructure.scripts;

import co.onmind.hex4w.application.ports.out.ScriptingPort;
import co.onmind.hex4w.application.ports.out.ScriptServicesPort;
import co.onmind.hex4w.domain.models.ScriptResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Value;

@Component
public class GraalJsAdapter implements ScriptingPort {

    private static final Logger logger = LoggerFactory.getLogger(GraalJsAdapter.class);

    private final Engine engine;
    private final ScriptServicesPort scriptServices;
    private final long statementLimit;

    public GraalJsAdapter(ScriptServicesPort scriptServices,
                          @org.springframework.beans.factory.annotation.Value("${app.scripts.statement-limit:5000000}") long statementLimit) {
        this.scriptServices = scriptServices;
        this.statementLimit = statementLimit;
        this.engine = Engine.create("js");
        logger.info("GraalVM JavaScript engine initialized for sandboxed execution "
                + "(statement limit: {})", statementLimit > 0 ? statementLimit : "disabled");
    }

    @Override
    public Mono<ScriptResult> executeScript(String script) {
        return Mono.fromCallable(() -> executeSandboxed(script))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> logger.error("Script execution failed: {}", e.getMessage()));
    }

    private ScriptResult executeSandboxed(String script) {
        Context.Builder builder = Context.newBuilder("js")
                .engine(engine)
                .allowHostAccess(HostAccess.ALL)
                .allowNativeAccess(false)
                .allowCreateThread(false)
                .allowIO(false)
                .allowHostClassLoading(false)
                .allowHostClassLookup(className -> false)
                .option("js.ecmascript-version", "2023")
                .allowExperimentalOptions(true)
                .option("js.timezone", "UTC");

        if (statementLimit > 0) {
            // Bounds the work a script can do. GraalVM Community has no CPU-time
            // boxing (sandbox.MaxCPUTime is an Enterprise feature), so the statement
            // count is the only available guard against a runaway script: when the
            // limit is hit the context is closed and the script is cancelled, which
            // releases the worker thread instead of leaving it spinning forever.
            // It is not a wall-clock bound — a script blocked on I/O through the
            // services facade is not counted by it.
            builder.resourceLimits(ResourceLimits.newBuilder()
                    .statementLimit(statementLimit, null)
                    .build());
        }

        try (Context context = builder.build()) {

            context.initialize("js");

            // Expose the facade as the global "services" object — the only host
            // object reachable from scripts. allowHostClassLookup(false) prevents
            // scripts from loading arbitrary Java classes.
            context.getBindings("js").putMember("services", scriptServices);

            Value result = context.eval("js", script);
            String value = result.isNull() ? null : result.toString();
            return new ScriptResult(value, "", null);
        } catch (PolyglotException e) {
            if (e.isResourceExhausted() || e.isCancelled()) {
                logger.warn("Script execution exceeded the statement limit of {}", statementLimit);
                return new ScriptResult(null, "",
                        "Script exceeded the statement limit of " + statementLimit);
            }
            logger.warn("Script execution error: {}", e.getMessage());
            return new ScriptResult(null, "", e.getMessage());
        } catch (Exception e) {
            logger.warn("Script execution error: {}", e.getMessage());
            return new ScriptResult(null, "", e.getMessage());
        }
    }
}
