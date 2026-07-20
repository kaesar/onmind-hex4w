package co.onmind.hex4w.application.ports.in;

import co.onmind.hex4w.application.dto.out.StoreItemResponseDto;
import reactor.core.publisher.Flux;

public interface ListStoreTrait {
    Flux<StoreItemResponseDto> listItems(String bucket);
}