package co.onmind.hex4w.application.usecases;

import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import co.onmind.hex4w.application.mappers.ScriptingMapper;
import co.onmind.hex4w.application.ports.in.ExecuteScriptTrait;
import co.onmind.hex4w.application.ports.out.ScriptingPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ScriptingUseCase implements ExecuteScriptTrait {

    private final ScriptingPort scriptingPort;
    private final ScriptingMapper scriptingMapper;

    public ScriptingUseCase(ScriptingPort scriptingPort, ScriptingMapper scriptingMapper) {
        this.scriptingPort = scriptingPort;
        this.scriptingMapper = scriptingMapper;
    }

    @Override
    public Mono<ScriptResultResponseDto> executeScript(String script) {
        return scriptingPort.executeScript(script)
            .map(scriptingMapper::toResponseDto);
    }
}