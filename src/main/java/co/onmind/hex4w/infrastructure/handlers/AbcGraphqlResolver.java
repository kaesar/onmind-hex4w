package co.onmind.hex4w.infrastructure.handlers;

import co.onmind.hex4w.application.ports.in.AbcSheetTrait;
import co.onmind.hex4w.application.ports.in.AbcSheetTrait.SheetRequest;
import co.onmind.hex4w.application.dto.out.SheetResponseDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.graphql.enabled", havingValue = "true")
public class AbcGraphqlResolver {

    private final AbcSheetTrait abcSheetTrait;

    public AbcGraphqlResolver(AbcSheetTrait abcSheetTrait) {
        this.abcSheetTrait = abcSheetTrait;
    }

    @QueryMapping
    public Mono<SheetResponseDto> abcSheet(
            @Argument String show,
            @Argument String from,
            @Argument String some) {
        return abcSheetTrait.sheet(show, from, some);
    }

    @QueryMapping
    public Flux<SheetResponseDto> abcSheets(
            @Argument List<AbcSheetInput> requests) {
        List<SheetRequest> reqs = requests.stream()
                .map(r -> new SheetRequest(r.show(), r.from(), r.some))
                .toList();
        return abcSheetTrait.sheets(reqs);
    }

    public record AbcSheetInput(String show, String from, String some) {}
}
