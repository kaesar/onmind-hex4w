package co.onmind.hex4w.application.ports.out;

import co.onmind.hex4w.domain.models.StoreItem;
import reactor.core.publisher.Flux;

public interface StorePort {
    Flux<StoreItem> listItems(String bucket);
}