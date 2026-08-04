package co.onmind.hex4w.application.ports.out;

import co.onmind.hex4w.domain.models.StoreItem;
import co.onmind.hex4w.infrastructure.webclients.dto.AbcResponse;

import java.util.List;

/**
 * Facade that aggregates all infrastructure output ports into a single
 * synchronous API surface for GraalVM JavaScript scripts.
 * <p>
 * Each method blocks on its reactive counterpart (Mono/Flux) — acceptable
 * because scripts execute on Schedulers.boundedElastic() inside GraalJsAdapter.
 * The gRPC vs HTTP transport is transparent: the injected AbcPort is whichever
 * @Primary bean Spring resolves (GrpcAbcAdapter when profile 'grpc' is active,
 * AbcAdapter otherwise).
 * <p>
 * Security: only the public methods of this facade are reachable from scripts.
 * GraalJsAdapter configures HostAccess.ALLOW with allowHostClassLookup(false)
 * and allowHostClassLoading(false), preventing scripts from touching arbitrary
 * Java classes or system resources.
 */
public interface ScriptServicesPort {

    // -- XDB (HTTP or gRPC) -----------------------------------------------------------------

    /** Read-only sheet query (maps to AbcPort.sheet). */
    AbcResponse abcSheet(String show, String from, String some);

    /** Write/query via ABC exec (maps to AbcPort.exec). */
    AbcResponse abcExec(String what, String from, String some, String with, String puts);

    // -- Events (Kafka / SQS / SNS / EventBridge / RabbitMQ) --------------------------------

    /** Publish an event. Throws if no event publisher bean is available. */
    void publish(String topic, String key, String payload);

    // -- Lambda ---------------------------------------------------------------------------

    /** Invoke an AWS Lambda function synchronously (waits for response). */
    String invoke(String functionName, String payload);

    /** Invoke an AWS Lambda function fire-and-forget (InvocationType.EVENT). */
    void invokeAsync(String functionName, String payload);

    // -- Storage (S3) ---------------------------------------------------------------------

    /** List objects in an S3 bucket. */
    List<StoreItem> listItems(String bucket);

    // -- Email ---------------------------------------------------------------------------

    /** Send an email (best-effort, no circuit breaker on email). */
    void sendEmail(String to, String subject, String body);

    // -- Cache (Redis) -------------------------------------------------------------------

    String cacheGet(String key);
    void cacheSet(String key, String value);
    void cacheEvict(String key);
}
