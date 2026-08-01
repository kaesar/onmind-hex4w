package co.onmind.hex4w.infrastructure.webclients;

import co.onmind.hex4w.infrastructure.webclients.dto.AbcRequest;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcResponse;
import co.onmind.hex4w.infrastructure.webclients.auth.XdbToken;
import co.onmind.hex4w.transverse.WebClientGeneric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;

public class AbcWebClient {

    private static final Logger logger = LoggerFactory.getLogger(AbcWebClient.class);
    private static final String ABC_ENDPOINT = "/abc";

    private final WebClientGeneric webClientGeneric;
    private final XdbToken abcToken;

    public AbcWebClient(WebClientGeneric webClientGeneric) {
        this(webClientGeneric, XdbToken.none());
    }

    public AbcWebClient(WebClientGeneric webClientGeneric, XdbToken abcToken) {
        this.webClientGeneric = webClientGeneric;
        this.abcToken = abcToken != null ? abcToken : XdbToken.none();
    }

    public Mono<AbcResponse> sheet(String showColumns, String from, String some) {
        AbcRequest request = AbcRequest.builder()
            .what("find")
            .from(from != null ? from : "xykit")
            .some(some != null ? some : "sheet")
            .show(showColumns != null ? showColumns : "kit01 sheetid, kit02 name, kit03 title, kit05 model")
            .build();
        logger.debug("XDB sheet request: show={}", request.show());
        return webClientGeneric.post(ABC_ENDPOINT, request, AbcResponse.class, abcToken)
            .doOnSuccess(r -> logger.debug("XDB sheet response: ok={}, total={}", r.ok(), r.total()));
    }

    public Mono<AbcResponse> find(AbcRequest request) {
        AbcRequest findRequest = new AbcRequest(
            request.way(), "find", request.from(), request.some(), request.show(),
            null, request.with(), null, request.where(), request.sort(), request.limit(), request.offset()
        );
        logger.debug("XDB find request: from={}, some={}", findRequest.from(), findRequest.some());
        return webClientGeneric.post(ABC_ENDPOINT, findRequest, AbcResponse.class, abcToken)
            .doOnSuccess(r -> logger.debug("XDB find response: ok={}, total={}", r.ok(), r.total()));
    }

    public Mono<AbcResponse> insert(AbcRequest request) {
        AbcRequest insertRequest = new AbcRequest(
            request.way(), "insert", request.from(), request.some(), request.show(),
            null, request.with(), request.puts(), request.where(), request.sort(), request.limit(), request.offset()
        );
        logger.debug("XDB insert request: from={}, some={}, puts={}", insertRequest.from(), insertRequest.some(), insertRequest.puts());
        return webClientGeneric.post(ABC_ENDPOINT, insertRequest, AbcResponse.class, abcToken)
            .doOnSuccess(r -> logger.debug("XDB insert response: ok={}, status={}", r.ok(), r.status()));
    }

    public Mono<AbcResponse> update(AbcRequest request) {
        AbcRequest updateRequest = new AbcRequest(
            request.way(), "update", request.from(), request.some(), request.show(),
            null, request.with(), request.puts(), request.where(), request.sort(), request.limit(), request.offset()
        );
        logger.debug("XDB update request: from={}, some={}", updateRequest.from(), updateRequest.some());
        return webClientGeneric.post(ABC_ENDPOINT, updateRequest, AbcResponse.class, abcToken)
            .doOnSuccess(r -> logger.debug("XDB update response: ok={}, status={}", r.ok(), r.status()));
    }

    public Mono<AbcResponse> remove(AbcRequest request) {
        AbcRequest deleteRequest = new AbcRequest(
            request.way(), "delete", request.from(), request.some(), request.show(),
            null, request.with(), null, request.where(), request.sort(), request.limit(), request.offset()
        );
        logger.debug("XDB delete request: from={}, some={}", deleteRequest.from(), deleteRequest.some());
        return webClientGeneric.post(ABC_ENDPOINT, deleteRequest, AbcResponse.class, abcToken)
            .doOnSuccess(r -> logger.debug("XDB delete response: ok={}, status={}", r.ok(), r.status()));
    }

    public Mono<AbcResponse> create(AbcRequest request) {
        AbcRequest createRequest = new AbcRequest(
            request.way(), "create", request.from(), request.some(), null,
            null, request.with(), null, request.where(), request.sort(), request.limit(), request.offset()
        );
        logger.debug("XDB create request: from={}, some={}", createRequest.from(), createRequest.some());
        return webClientGeneric.post(ABC_ENDPOINT, createRequest, AbcResponse.class, abcToken)
            .doOnSuccess(r -> logger.debug("XDB create response: ok={}, status={}", r.ok(), r.status()));
    }

    public Mono<AbcResponse> drop(AbcRequest request) {
        AbcRequest dropRequest = new AbcRequest(
            request.way(), "drop", request.from(), request.some(), request.show(),
            null, request.with(), null, request.where(), request.sort(), request.limit(), request.offset()
        );
        logger.debug("XDB drop request: from={}, some={}", dropRequest.from(), dropRequest.some());
        return webClientGeneric.post(ABC_ENDPOINT, dropRequest, AbcResponse.class, abcToken)
            .doOnSuccess(r -> logger.debug("XDB drop response: ok={}, status={}", r.ok(), r.status()));
    }

    public Mono<AbcResponse> define(AbcRequest request) {
        if (request.puts() != null) {
            String spec = request.puts().toString();
            if (!"[]".equals(spec) && !spec.isBlank()) {
                validateSpec(spec);
            }
        }
        AbcRequest defineRequest = new AbcRequest(
            request.way(), "define", request.from(), request.some(), request.show(),
            null, request.with(), request.puts(), request.where(), request.sort(), request.limit(), request.offset()
        );
        logger.debug("XDB define request: from={}, some={}, puts={}", defineRequest.from(), defineRequest.some(), defineRequest.puts());
        return webClientGeneric.post(ABC_ENDPOINT, defineRequest, AbcResponse.class, abcToken)
            .doOnSuccess(r -> logger.debug("XDB define response: ok={}, status={}", r.ok(), r.status()));
    }

    public Mono<AbcResponse> whoami() {
        AbcRequest request = AbcRequest.builder().call("whoami").build();
        logger.debug("XDB whoami request");
        return webClientGeneric.post(ABC_ENDPOINT, request, AbcResponse.class, abcToken)
            .doOnSuccess(r -> logger.debug("XDB whoami response: ok={}, data={}", r.ok(), r.data()));
    }

    public Mono<AbcResponse> signup(Map<String, Object> datax) {
        AbcRequest request = AbcRequest.builder().call("signup").with("USER").puts(datax).build();
        logger.debug("XDB signup request");
        return webClientGeneric.post(ABC_ENDPOINT, request, AbcResponse.class, abcToken)
            .doOnSuccess(r -> logger.debug("XDB signup response: ok={}, status={}", r.ok(), r.status()));
    }

    public Mono<AbcResponse> ask(AbcRequest request) {
        return switch (request.what()) {
            case "find" -> find(request);
            case "insert" -> insert(request);
            case "update" -> update(request);
            case "delete" -> remove(request);
            case "create" -> create(request);
            case "drop" -> drop(request);
            case "define" -> define(request);
            default -> {
                logger.debug("XDB generic ask request: what={}", request.what());
                yield webClientGeneric.post(ABC_ENDPOINT, request, AbcResponse.class, abcToken)
                    .doOnSuccess(r -> logger.debug("XDB ask response: ok={}, status={}", r.ok(), r.status()));
            }
        };
    }

    private void validateSpec(String spec) {
        if (!spec.matches("^[a-z0-9]+=\\w+(?:,[a-z0-9]+=\\w+)*$")) {
            throw new IllegalArgumentException("Invalid spec format. Use: column=alias,column2=alias2");
        }
        String[] pairs = spec.split(",");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String pair : pairs) {
            String[] parts = pair.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid spec pair: " + pair);
            }
            if (!seen.add(parts[0])) {
                throw new IllegalArgumentException("Duplicate column: " + parts[0]);
            }
            if (!seen.add(parts[1])) {
                throw new IllegalArgumentException("Duplicate alias: " + parts[1]);
            }
        }
    }
}