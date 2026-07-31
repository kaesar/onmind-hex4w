package co.onmind.hex4w.application.usecases;

import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import co.onmind.hex4w.application.mappers.ScriptingMapper;
import co.onmind.hex4w.application.ports.in.ExecuteScriptTrait;
import co.onmind.hex4w.application.ports.out.ScriptSourcePort;
import co.onmind.hex4w.application.ports.out.ScriptingPort;
import co.onmind.hex4w.domain.models.AllowedScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ScriptingUseCase implements ExecuteScriptTrait {

    private final ScriptSourcePort scriptSourcePort;
    private final ScriptingPort scriptingPort;
    private final ScriptingMapper scriptingMapper;

    public ScriptingUseCase(
            ScriptSourcePort scriptSourcePort,
            ScriptingPort scriptingPort,
            ScriptingMapper scriptingMapper) {
        this.scriptSourcePort = scriptSourcePort;
        this.scriptingPort = scriptingPort;
        this.scriptingMapper = scriptingMapper;
    }

    /**
     * Executes a script identified by file name. The name must match {@link AllowedScript}.
     *
     * @param scriptFileName file name only (e.g. {@code hello.js})
     */
    @Override
    public Mono<ScriptResultResponseDto> executeScript(String scriptFileName) {
        return Mono.fromCallable(() -> AllowedScript.requireByFileName(scriptFileName))
            .map(AllowedScript::fileName)
            .flatMap(scriptSourcePort::loadScript)
            .flatMap(scriptingPort::executeScript)
            .map(scriptingMapper::toResponseDto);
    }
}
