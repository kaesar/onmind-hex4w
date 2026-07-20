package co.onmind.hex4w.application.ports.in;

import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import reactor.core.publisher.Mono;

public interface ExecuteScriptTrait {
    Mono<ScriptResultResponseDto> executeScript(String script);
}