package co.onmind.hex4w.application.usecases;

import co.onmind.hex4w.application.dto.out.SheetResponseDto;
import co.onmind.hex4w.application.ports.in.XdbcSheetTrait;
import co.onmind.hex4w.infrastructure.webclients.AbcWebClient;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class XdbcUseCase implements XdbcSheetTrait {

    private final AbcWebClient abcWebClient;

    public XdbcUseCase(AbcWebClient abcWebClient) {
        this.abcWebClient = abcWebClient;
    }

    @Override
    public Mono<SheetResponseDto> getSheet() {
        return abcWebClient.sheet(null, null, null)
            .map(this::toDto);
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