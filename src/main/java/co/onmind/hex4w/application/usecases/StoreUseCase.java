package co.onmind.hex4w.application.usecases;

import co.onmind.hex4w.application.dto.out.StoreItemResponseDto;
import co.onmind.hex4w.application.mappers.StoreMapper;
import co.onmind.hex4w.application.ports.in.ListStoreTrait;
import co.onmind.hex4w.application.ports.out.StorePort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class StoreUseCase implements ListStoreTrait {

    private final StorePort storePort;
    private final StoreMapper storeMapper;

    public StoreUseCase(StorePort storePort, StoreMapper storeMapper) {
        this.storePort = storePort;
        this.storeMapper = storeMapper;
    }

    @Override
    public Flux<StoreItemResponseDto> listItems(String bucket) {
        return storePort.listItems(bucket)
            .map(storeMapper::toResponseDto);
    }
}