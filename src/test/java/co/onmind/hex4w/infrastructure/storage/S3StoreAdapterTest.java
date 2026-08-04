package co.onmind.hex4w.infrastructure.storage;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3StoreAdapterTest {

    private S3AsyncClient s3Client;
    private S3StoreAdapter adapter;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3AsyncClient.class);
        CircuitBreaker cb = CircuitBreaker.ofDefaults("s3");
        adapter = new S3StoreAdapter(s3Client, cb);
    }

    @Test
    @DisplayName("listItems returns StoreItems mapped from S3 objects")
    void listItemsReturnsItems() {
        S3Object obj1 = S3Object.builder()
            .key("file1.txt")
            .size(1024L)
            .eTag("\"etag1\"")
            .lastModified(Instant.parse("2026-01-01T00:00:00Z"))
            .build();
        S3Object obj2 = S3Object.builder()
            .key("file2.txt")
            .size(2048L)
            .eTag("\"etag2\"")
            .lastModified(Instant.parse("2026-01-02T00:00:00Z"))
            .build();

        ListObjectsV2Publisher paginator = mock(ListObjectsV2Publisher.class);
        when(paginator.contents()).thenReturn(SdkPublisher.fromIterable(List.of(obj1, obj2)));
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
            .thenReturn(paginator);

        StepVerifier.create(adapter.listItems("my-bucket"))
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    @DisplayName("listItems emits empty flux when bucket has no objects")
    void emptyBucket() {
        ListObjectsV2Publisher paginator = mock(ListObjectsV2Publisher.class);
        when(paginator.contents()).thenReturn(SdkPublisher.fromIterable(List.of()));
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
            .thenReturn(paginator);

        StepVerifier.create(adapter.listItems("empty-bucket"))
            .expectNextCount(0)
            .verifyComplete();
    }

    @Test
    @DisplayName("listItems maps S3 object fields into StoreItem")
    void mapsS3Fields() {
        S3Object obj = S3Object.builder()
            .key("folder/file.txt")
            .size(42L)
            .eTag("\"abc\"")
            .lastModified(Instant.parse("2026-03-15T10:30:00Z"))
            .build();

        ListObjectsV2Publisher paginator = mock(ListObjectsV2Publisher.class);
        when(paginator.contents()).thenReturn(SdkPublisher.fromIterable(List.of(obj)));
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
            .thenReturn(paginator);

        StepVerifier.create(adapter.listItems("bucket"))
            .assertNext(item -> {
                assertEquals("folder/file.txt", item.key());
                assertEquals(42L, item.size());
                assertEquals("\"abc\"", item.eTag());
                assertEquals(Instant.parse("2026-03-15T10:30:00Z"), item.lastModified().toInstant(java.time.ZoneOffset.UTC));
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("listItems propagates errors from S3 client")
    void propagatesErrors() {
        ListObjectsV2Publisher paginator = mock(ListObjectsV2Publisher.class);
        when(paginator.contents())
            .thenReturn(SdkPublisher.adapt(reactor.core.publisher.Flux.error(
                new RuntimeException("S3 connection failed"))));
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
            .thenReturn(paginator);

        StepVerifier.create(adapter.listItems("bad-bucket"))
            .expectErrorMatches(e -> e instanceof RuntimeException &&
                e.getMessage().contains("S3 connection failed"))
            .verify();
    }

    @Test
    @DisplayName("listItems sends correct bucket name in request")
    void correctBucketName() {
        ListObjectsV2Publisher paginator = mock(ListObjectsV2Publisher.class);
        when(paginator.contents()).thenReturn(SdkPublisher.fromIterable(List.of()));
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
            .thenReturn(paginator);

        adapter.listItems("my-target-bucket").blockLast();

        var captor = org.mockito.ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3Client).listObjectsV2Paginator(captor.capture());
        assertEquals("my-target-bucket", captor.getValue().bucket());
    }
}
