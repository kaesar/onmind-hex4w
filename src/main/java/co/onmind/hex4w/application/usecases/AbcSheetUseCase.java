package co.onmind.hex4w.application.usecases;

import co.onmind.hex4w.application.dto.out.SheetResponseDto;
import co.onmind.hex4w.application.ports.in.AbcSheetTrait;
import co.onmind.hex4w.application.ports.out.AbcPort;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class AbcSheetUseCase implements AbcSheetTrait {

    private final AbcPort abcPort;

    public AbcSheetUseCase(AbcPort abcPort) {
        this.abcPort = abcPort;
    }

    @Override
    public Mono<SheetResponseDto> sheet(String show, String from, String some) {
        return abcPort.sheet(show, from, some)
                .map(this::toDto);
    }

    @Override
    public Flux<SheetResponseDto> sheets(Iterable<SheetRequest> requests) {
        return Flux.fromIterable(requests)
                .flatMap(req -> sheet(req.show(), req.from(), req.some()));
    }

    private SheetResponseDto toDto(AbcResponse response) {
        return new SheetResponseDto(
                response.ok(),
                response.status(),
                response.message(),
                response.total(),
                response.data()
        );
    }
}
