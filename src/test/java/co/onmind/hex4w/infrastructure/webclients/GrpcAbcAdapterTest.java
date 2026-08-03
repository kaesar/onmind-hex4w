package co.onmind.hex4w.infrastructure.webclients;

import co.onmind.grpc.proto.AbcServiceGrpc;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcRequest;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;

class GrpcAbcAdapterTest {

    private AbcServiceGrpc.AbcServiceStub stub;
    private CircuitBreaker circuitBreaker;
    private ObjectMapper objectMapper;

    static class TestableGrpcAbcAdapter extends GrpcAbcAdapter {
        private final reactor.core.publisher.Mono<co.onmind.grpc.proto.AbcResponse> grpcResponse;

        TestableGrpcAbcAdapter(AbcServiceGrpc.AbcServiceStub stub, CircuitBreaker cb, ObjectMapper om,
                               reactor.core.publisher.Mono<co.onmind.grpc.proto.AbcResponse> grpcResponse) {
            super(stub, cb, om);
            this.grpcResponse = grpcResponse;
        }

        @Override
        protected reactor.core.publisher.Mono<AbcResponse> callExecute(co.onmind.grpc.proto.AbcRequest grpcRequest) {
            return grpcResponse.map(this::toAbcResponse);
        }
    }

    @BeforeEach
    void setUp() {
        stub = mock(AbcServiceGrpc.AbcServiceStub.class);
        circuitBreaker = CircuitBreaker.ofDefaults("grpc-abc");
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("sheet() maps gRPC response to AbcResponse with parsed data")
    void sheetMapsResponse() {
        co.onmind.grpc.proto.AbcResponse grpcResponse = co.onmind.grpc.proto.AbcResponse.newBuilder()
            .setOk(true)
            .setStatus("200")
            .setMessage("ok")
            .setTotal(1)
            .setDataJson("[{\"id\":42}]")
            .build();

        TestableGrpcAbcAdapter adapter = new TestableGrpcAbcAdapter(stub, circuitBreaker, objectMapper,
            reactor.core.publisher.Mono.just(grpcResponse));

        StepVerifier.create(adapter.sheet("id", "xykit", "sheet"))
            .expectNextMatches(r -> r.ok() == true
                && r.total() == 1
                && r.data() instanceof JsonNode)
            .verifyComplete();
    }

    @Test
    @DisplayName("exec() propagates gRPC errors")
    void execPropagatesErrors() {
        TestableGrpcAbcAdapter adapter = new TestableGrpcAbcAdapter(stub, circuitBreaker, objectMapper,
            reactor.core.publisher.Mono.error(new RuntimeException("gRPC failure")));

        AbcRequest req = AbcRequest.builder().what("insert").from("xykit").build();

        StepVerifier.create(adapter.exec(req))
            .expectErrorMatches(e -> e.getMessage().equals("gRPC failure"))
            .verify();
    }

    @Test
    @DisplayName("toGrpcRequest maps where/sort/limit/offset into puts JSON")
    void mapsExtrasIntoPuts() {
        AbcRequest req = new AbcRequest(
            "sql", "find", "table1", "row1", "id,name", "cols",
            null, "{\"value\":1}", "{\"active\":true}", "id ASC", 10, 0
        );

        TestableGrpcAbcAdapter adapter = new TestableGrpcAbcAdapter(stub, circuitBreaker, objectMapper,
            reactor.core.publisher.Mono.empty());

        co.onmind.grpc.proto.AbcRequest grpcRequest = adapter.toGrpcRequest(req);
        String puts = grpcRequest.getPuts();
        assert puts.contains("\"where\"") : "where should be in puts JSON";
        assert puts.contains("\"limit\":10") : "limit should be in puts JSON";
        assert puts.contains("\"offset\":0") : "offset should be in puts JSON";
        assert puts.contains("\"sort\":\"id ASC\"") : "sort should be in puts JSON";
    }

    @Test
    @DisplayName("toGrpcRequest maps puts directly when no where/sort/limit/offset")
    void mapsPutsDirectly() {
        AbcRequest req = new AbcRequest(
            "sql", "insert", "table1", null, null, null, "ROW",
            "{\"value\":1}", null, null, null, null
        );

        TestableGrpcAbcAdapter adapter = new TestableGrpcAbcAdapter(stub, circuitBreaker, objectMapper,
            reactor.core.publisher.Mono.empty());

        co.onmind.grpc.proto.AbcRequest grpcRequest = adapter.toGrpcRequest(req);
        assert grpcRequest.getPuts().equals("{\"value\":1}");
    }

    @Test
    @DisplayName("toAbcResponse handles null data_json")
    void handlesNullDataJson() {
        co.onmind.grpc.proto.AbcResponse grpcResponse = co.onmind.grpc.proto.AbcResponse.newBuilder()
            .setOk(false)
            .setStatus("500")
            .setMessage("Internal error")
            .setTotal(0)
            .build();

        TestableGrpcAbcAdapter adapter = new TestableGrpcAbcAdapter(stub, circuitBreaker, objectMapper,
            reactor.core.publisher.Mono.just(grpcResponse));

        AbcResponse response = adapter.toAbcResponse(grpcResponse);
        assert response.ok() == false;
        assert response.data() == null;
        assert response.status() == 500;
    }

    @Test
    @DisplayName("toAbcResponse parses data_json as JsonNode")
    void parsesDataJsonAsJsonNode() {
        co.onmind.grpc.proto.AbcResponse grpcResponse = co.onmind.grpc.proto.AbcResponse.newBuilder()
            .setOk(true)
            .setStatus("200")
            .setMessage("ok")
            .setTotal(2)
            .setDataJson("[{\"id\":1},{\"id\":2}]")
            .build();

        TestableGrpcAbcAdapter adapter = new TestableGrpcAbcAdapter(stub, circuitBreaker, objectMapper,
            reactor.core.publisher.Mono.just(grpcResponse));

        AbcResponse response = adapter.toAbcResponse(grpcResponse);
        assert response.data() instanceof JsonNode;
    }
}
