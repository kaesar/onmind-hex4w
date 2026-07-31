package co.onmind.hex4w.application.ports.in;

import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import reactor.core.publisher.Mono;

public interface ExecuteScriptTrait {
    /**
     * Executes a whitelisted JavaScript file by name (e.g. {@code hello.js}).
     */
    Mono<ScriptResultResponseDto> executeScript(String scriptFileName);
}