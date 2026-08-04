package co.onmind.hex4w.domain.models;

import co.onmind.hex4w.domain.exceptions.ScriptNotAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScriptWhitelistTest {

    private final ScriptWhitelist whitelist = new ScriptWhitelist("hello.js,example.js,services.js");

    @Test
    @DisplayName("Allows scripts listed in whitelist")
    void allowsListedScripts() {
        assertTrue(whitelist.isAllowed("hello.js"));
        assertTrue(whitelist.isAllowed("example.js"));
        assertTrue(whitelist.isAllowed("services.js"));
    }

    @Test
    @DisplayName("Rejects scripts not in whitelist")
    void rejectsUnlistedScripts() {
        assertFalse(whitelist.isAllowed("malicious.js"));
        assertFalse(whitelist.isAllowed("evil.js"));
    }

    @Test
    @DisplayName("Is case-insensitive for filenames")
    void isCaseInsensitive() {
        assertTrue(whitelist.isAllowed("HELLO.JS"));
        assertTrue(whitelist.isAllowed("Services.JS"));
    }

    @Test
    @DisplayName("Throws ScriptNotAllowedException for disallowed scripts")
    void throwsForDisallowed() {
        ScriptNotAllowedException ex = assertThrows(
            ScriptNotAllowedException.class,
            () -> whitelist.requireAllowed("bad.js")
        );
        assertTrue(ex.getMessage().contains("bad.js"));
        assertTrue(ex.getMessage().contains("allowed"));
    }

    @Test
    @DisplayName("Returns allowed filename via requireAllowed for listed scripts")
    void requireAllowedPassesForListed() {
        assertDoesNotThrow(() -> whitelist.requireAllowed("hello.js"));
    }

    @Test
    @DisplayName("Null-safe check returns false")
    void nullSafeCheck() {
        assertFalse(whitelist.isAllowed(null));
        assertFalse(whitelist.isAllowed(""));
        assertFalse(whitelist.isAllowed("   "));
    }

    @Test
    @DisplayName("allowedFileNames returns comma-separated list")
    void allowedFileNames() {
        String names = whitelist.allowedFileNames();
        assertTrue(names.contains("hello.js"));
        assertTrue(names.contains("example.js"));
        assertTrue(names.contains("services.js"));
    }

    @Test
    @DisplayName("Trims whitespace in CSV entries")
    void trimsWhitespace() {
        ScriptWhitelist wl = new ScriptWhitelist("  hello.js , example.js  ");
        assertTrue(wl.isAllowed("hello.js"));
        assertTrue(wl.isAllowed("example.js"));
    }

    @Test
    @DisplayName("Empty CSV yields empty whitelist (no scripts allowed)")
    void emptyCsvYieldsEmpty() {
        ScriptWhitelist wl = new ScriptWhitelist("");
        assertFalse(wl.isAllowed("hello.js"));
    }
}
