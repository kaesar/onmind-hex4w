package co.onmind.hex4w.application.ports.out;

import reactor.core.publisher.Mono;

/**
 * Loads JavaScript source for an allowed script file name.
 */
public interface ScriptSourcePort {
    Mono<String> loadScript(String fileName);
}
