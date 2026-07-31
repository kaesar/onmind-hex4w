package co.onmind.hex4w.domain.models;

import co.onmind.hex4w.domain.exceptions.ScriptNotAllowedException;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Whitelist of JavaScript files that may be executed.
 * Only file names listed here can be requested via the scripts API.
 */
public enum AllowedScript {

    HELLO("hello.js"),
    EXAMPLE("example.js");

    private final String fileName;

    AllowedScript(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }

    public static AllowedScript requireByFileName(String fileName) {
        return findByFileName(fileName)
            .orElseThrow(() -> new ScriptNotAllowedException(fileName, allowedFileNames()));
    }

    public static Optional<AllowedScript> findByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        String normalized = fileName.trim();
        return Arrays.stream(values())
            .filter(script -> script.fileName.equalsIgnoreCase(normalized))
            .findFirst();
    }

    public static String allowedFileNames() {
        return Arrays.stream(values())
            .map(AllowedScript::fileName)
            .collect(Collectors.joining(", "));
    }
}
