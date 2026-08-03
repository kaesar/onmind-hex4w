package co.onmind.hex4w.infrastructure.configuration;

import co.onmind.grpc.proto.AbcServiceGrpc;
import co.onmind.hex4w.application.ports.out.AbcPort;
import co.onmind.hex4w.application.ports.out.CachePort;
import co.onmind.hex4w.infrastructure.webclients.CachedAbcAdapter;
import co.onmind.hex4w.infrastructure.webclients.GrpcAbcAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/**
 * gRPC configuration for XDB communication.
 * Activated with Spring profile 'grpc'.
 * Provides a {@link ManagedChannel} bean and wires {@link GrpcAbcAdapter}
 * as the {@code @Primary} {@link AbcPort}, parallel to the HTTP-based
 * configuration in {@link WebClientConfiguration}.
 */
@Configuration
@Profile("grpc")
public class GrpcConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(GrpcConfiguration.class);

    @Bean(destroyMethod = "shutdown")
    public ManagedChannel grpcManagedChannel(
            @Value("${app.grpc.host:localhost}") String host,
            @Value("${app.grpc.port:9991}") int port) {
        logger.info("gRPC channel -> {}:{}", host, port);
        return ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();
    }

    @Bean
    public AbcServiceGrpc.AbcServiceStub grpcAbcServiceStub(ManagedChannel grpcManagedChannel) {
        return AbcServiceGrpc.newStub(grpcManagedChannel);
    }

    @Bean
    @Primary
    public AbcPort grpcAbcPort(
            AbcServiceGrpc.AbcServiceStub grpcStub,
            CircuitBreaker abcCircuitBreaker,
            CachePort cachePort,
            ObjectMapper objectMapper,
            @Value("${app.xdb.cache.ttl-seconds:300}") int ttlSeconds) {
        GrpcAbcAdapter grpcAdapter = new GrpcAbcAdapter(grpcStub, abcCircuitBreaker, objectMapper);
        return new CachedAbcAdapter(grpcAdapter, cachePort, objectMapper,
                Duration.ofSeconds(ttlSeconds));
    }
}
