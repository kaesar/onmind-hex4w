package co.onmind.hex4w.infrastructure.storage;

import co.onmind.hex4w.application.ports.out.StorePort;
import co.onmind.hex4w.domain.models.StoreItem;
import co.onmind.hex4w.transverse.resilience.CircuitBreakerGeneric;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class S3StoreAdapter implements StorePort {

    private static final Logger logger = LoggerFactory.getLogger(S3StoreAdapter.class);

    private final S3AsyncClient s3Client;
    private final CircuitBreaker circuitBreaker;

    public S3StoreAdapter(S3AsyncClient s3Client, CircuitBreaker s3CircuitBreaker) {
        this.s3Client = s3Client;
        this.circuitBreaker = s3CircuitBreaker;
    }

    @Override
    public Flux<StoreItem> listItems(String bucket) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .build();

        return CircuitBreakerGeneric.withCircuitBreaker(
                Flux.from(s3Client.listObjectsV2Paginator(request).contents())
                        .map(this::toStoreItem),
                circuitBreaker);
    }

    private StoreItem toStoreItem(S3Object s3Object) {
        LocalDateTime lastModified = s3Object.lastModified() != null
            ? LocalDateTime.ofInstant(s3Object.lastModified(), ZoneOffset.UTC)
            : null;

        return new StoreItem(
            s3Object.key(),
            s3Object.size(),
            lastModified,
            s3Object.eTag()
        );
    }
}
