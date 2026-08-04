package co.onmind.hex4w.infrastructure.scripts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ClasspathScriptSourceAdapterTest {

    @Mock
    ResourceLoader resourceLoader;

    private final String scriptContent = "// script content\nservices.invoke('fn','{}');";

    @Test
    @DisplayName("loadScript reads file content from classpath")
    void loadScriptReadsContent() throws IOException {
        Resource mockResource = new ByteArrayResource(scriptContent.getBytes(StandardCharsets.UTF_8));
        when(resourceLoader.getResource("classpath:scripts/hello.js"))
            .thenReturn(mockResource);

        ClasspathScriptSourceAdapter adapter =
            new ClasspathScriptSourceAdapter(resourceLoader, "classpath:scripts/");

        StepVerifier.create(adapter.loadScript("hello.js"))
            .expectNext(scriptContent)
            .verifyComplete();
    }

    @Test
    @DisplayName("loadScript appends trailing slash if missing in location")
    void trailingSlashAdded() throws IOException {
        Resource mockResource = new ByteArrayResource(scriptContent.getBytes(StandardCharsets.UTF_8));
        when(resourceLoader.getResource("classpath:scripts/hello.js"))
            .thenReturn(mockResource);

        ClasspathScriptSourceAdapter adapter =
            new ClasspathScriptSourceAdapter(resourceLoader, "classpath:scripts");

        StepVerifier.create(adapter.loadScript("hello.js"))
            .expectNext(scriptContent)
            .verifyComplete();
    }

    @Test
    @DisplayName("loadScript throws for path traversal attempts (defense in depth)")
    void rejectsPathTraversal() {
        ClasspathScriptSourceAdapter adapter =
            new ClasspathScriptSourceAdapter(resourceLoader, "classpath:scripts/");

        StepVerifier.create(adapter.loadScript("../../../etc/passwd"))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    @DisplayName("loadScript emits error when file not found")
    void fileNotFound() {
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(false);
        lenient().when(mockResource.isReadable()).thenReturn(false);
        when(resourceLoader.getResource(anyString())).thenReturn(mockResource);

        ClasspathScriptSourceAdapter adapter =
            new ClasspathScriptSourceAdapter(resourceLoader, "classpath:scripts/");

        StepVerifier.create(adapter.loadScript("missing.js"))
            .expectError(IllegalArgumentException.class)
            .verify();
    }
}
