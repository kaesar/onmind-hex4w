package co.onmind.hex4w.application.usecases;

import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import co.onmind.hex4w.application.mappers.ScriptingMapper;
import co.onmind.hex4w.application.ports.in.ExecuteScriptTrait;
import co.onmind.hex4w.application.ports.out.AbcPort;
import co.onmind.hex4w.application.ports.out.ScriptSourcePort;
import co.onmind.hex4w.application.ports.out.ScriptingPort;
import co.onmind.hex4w.application.ports.out.StorePort;
import co.onmind.hex4w.domain.models.AllowedScript;
import co.onmind.hex4w.domain.models.StoreItem;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ScriptingUseCase implements ExecuteScriptTrait {

    private final ScriptSourcePort scriptSourcePort;
    private final ScriptingPort scriptingPort;
    private final ScriptingMapper scriptingMapper;
    private final StorePort storePort;
    private final AbcPort abcPort;

    public ScriptingUseCase(
            ScriptSourcePort scriptSourcePort,
            ScriptingPort scriptingPort,
            ScriptingMapper scriptingMapper,
            StorePort storePort,
            AbcPort abcPort) {
        this.scriptSourcePort = scriptSourcePort;
        this.scriptingPort = scriptingPort;
        this.scriptingMapper = scriptingMapper;
        this.storePort = storePort;
        this.abcPort = abcPort;
    }

    @Override
    public Mono<ScriptResultResponseDto> executeScript(String scriptFileName) {
        return Mono.fromCallable(() -> AllowedScript.requireByFileName(scriptFileName))
            .map(AllowedScript::fileName)
            .flatMap(scriptSourcePort::loadScript)
            .flatMap(scriptingPort::executeScript)
            .map(scriptingMapper::toResponseDto);
    }

    // -- Expose StorePort and AbcPort through the use case --------------------------------------------------

    public Flux<StoreItem> listItems(String bucket) {
        return storePort.listItems(bucket);
    }

    public Mono<AbcResponse> abcSheet(String show, String from, String some) {
        return abcPort.sheet(show, from, some);
    }
}