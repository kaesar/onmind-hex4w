package co.onmind.hex4w.infrastructure.webclients;

import co.onmind.hex4w.application.ports.out.ScriptingPort;
import co.onmind.hex4w.domain.models.ScriptResult;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.StringWriter;

@Repository
public class GraalJsAdapter implements ScriptingPort {

    private static final Logger logger = LoggerFactory.getLogger(GraalJsAdapter.class);

    private final ScriptEngineManager scriptEngineManager;

    public GraalJsAdapter() {
        this.scriptEngineManager = new ScriptEngineManager();
        ScriptEngine engine = scriptEngineManager.getEngineByName("graal.js");
        if (engine != null) {
            logger.info("GraalVM JavaScript Engine initialized via javax.script");
        } else {
            logger.warn("GraalVM JavaScript Engine not found, trying Nashorn");
        }
    }

    @Override
    public Mono<ScriptResult> executeScript(String script) {
        return Mono.fromCallable(() -> executeSync(script))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> logger.error("Script execution failed: {}", e.getMessage()));
    }

    private ScriptResult executeSync(String script) {
        StringWriter outWriter = new StringWriter();
        StringWriter errWriter = new StringWriter();

        try {
            ScriptEngine engine = scriptEngineManager.getEngineByName("graal.js");
            if (engine == null) {
                engine = scriptEngineManager.getEngineByName("nashorn");
            }
            if (engine == null) {
                engine = scriptEngineManager.getEngineByName("JavaScript");
            }

            if (engine == null) {
                return new ScriptResult(null, "", "No JavaScript engine found");
            }

            engine.getContext().setWriter(outWriter);
            engine.getContext().setErrorWriter(errWriter);

            Object result = engine.eval(script);
            String stdout = outWriter.toString();
            String stderr = errWriter.toString();
            String value = result != null ? result.toString() : null;

            return new ScriptResult(value, stdout, stderr);
        } catch (ScriptException e) {
            return new ScriptResult(null, outWriter.toString(), e.getMessage());
        }
    }
}