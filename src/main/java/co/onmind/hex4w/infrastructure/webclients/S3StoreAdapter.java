package co.onmind.hex4w.infrastructure.webclients;

import co.onmind.hex4w.application.ports.out.StorePort;
import co.onmind.hex4w.domain.models.StoreItem;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Repository
public class S3StoreAdapter implements StorePort {

    private final S3AsyncClient s3Client;

    public S3StoreAdapter(S3AsyncClient s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public Flux<StoreItem> listItems(String bucket) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
            .bucket(bucket)
            .build();

        return Flux.from(s3Client.listObjectsV2Paginator(request))
            .flatMap(response -> Flux.fromIterable(response.contents()))
            .map(this::toStoreItem);
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