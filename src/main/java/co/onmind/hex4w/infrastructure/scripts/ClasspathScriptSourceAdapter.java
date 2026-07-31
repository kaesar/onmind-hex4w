package co.onmind.hex4w.infrastructure.scripts;

import co.onmind.hex4w.application.ports.out.ScriptSourcePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads script files from the classpath scripts directory (default: {@code classpath:scripts/}).
 */
@Component
public class ClasspathScriptSourceAdapter implements ScriptSourcePort {

    private static final Logger logger = LoggerFactory.getLogger(ClasspathScriptSourceAdapter.class);

    private final ResourceLoader resourceLoader;
    private final String scriptsLocation;

    public ClasspathScriptSourceAdapter(
            ResourceLoader resourceLoader,
            @Value("${app.scripts.location:classpath:scripts/}") String scriptsLocation) {
        this.resourceLoader = resourceLoader;
        this.scriptsLocation = scriptsLocation.endsWith("/") ? scriptsLocation : scriptsLocation + "/";
    }

    @Override
    public Mono<String> loadScript(String fileName) {
        return Mono.fromCallable(() -> readFile(fileName))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> logger.error("Failed to load script '{}': {}", fileName, e.getMessage()));
    }

    private String readFile(String fileName) throws IOException {
        // Defense in depth: reject path traversal even if whitelist is bypassed.
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid script file name: " + fileName);
        }

        Resource resource = resourceLoader.getResource(scriptsLocation + fileName);
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("Script file not found: " + fileName);
        }

        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
