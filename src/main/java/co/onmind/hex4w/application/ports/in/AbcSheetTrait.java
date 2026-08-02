package co.onmind.hex4w.application.ports.in;

import co.onmind.hex4w.application.dto.out.SheetResponseDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AbcSheetTrait {
    Mono<SheetResponseDto> sheet(String show, String from, String some);

    Flux<SheetResponseDto> sheets(Iterable<SheetRequest> requests);

    record SheetRequest(String show, String from, String some) {}
}
