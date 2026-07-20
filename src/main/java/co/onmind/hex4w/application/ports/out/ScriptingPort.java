package co.onmind.hex4w.application.ports.out;

import co.onmind.hex4w.domain.models.ScriptResult;
import reactor.core.publisher.Mono;

public interface ScriptingPort {
    Mono<ScriptResult> executeScript(String script);
}