package co.onmind.hex4w.application.ports.in;

import co.onmind.hex4w.application.dto.out.SheetResponseDto;
import reactor.core.publisher.Mono;

public interface XdbcSheetTrait {
    Mono<SheetResponseDto> getSheet();
}