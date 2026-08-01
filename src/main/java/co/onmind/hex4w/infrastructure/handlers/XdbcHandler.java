package co.onmind.hex4w.infrastructure.handlers;

import co.onmind.hex4w.application.ports.in.XdbcSheetTrait;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class XdbcHandler {

    private final XdbcSheetTrait xdbcSheetTrait;

    public XdbcHandler(XdbcSheetTrait xdbcSheetTrait) {
        this.xdbcSheetTrait = xdbcSheetTrait;
    }

    public Mono<ServerResponse> getSheet(ServerRequest request) {
        return xdbcSheetTrait.getSheet()
            .flatMap(result -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(result))
            .onErrorResume(e -> ServerResponse.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ErrorResponse("XDB_ERROR", e.getMessage(), 500)));
    }

    public record ErrorResponse(String code, String message, int status) {}
}