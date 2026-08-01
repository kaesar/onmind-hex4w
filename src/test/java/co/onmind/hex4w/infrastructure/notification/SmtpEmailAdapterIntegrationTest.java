package co.onmind.hex4w.infrastructure.notification;

import co.onmind.hex4w.application.ports.out.EmailPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.test.StepVerifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for SmtpEmailAdapter using a running Mailpit instance.
 * <p>
 * Mailpit must be running (SMTP on localhost:1025, API on localhost:8025).
 * Start it with: docker run -d -p 1025:1025 -p 8025:8025 mailpit/mailpit
 */
@SpringBootTest
class SmtpEmailAdapterIntegrationTest {

    private static final String MAILPIT_HOST = "localhost";
    private static final int SMTP_PORT = 1025;
    private static final int API_PORT = 8025;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> MAILPIT_HOST);
        registry.add("spring.mail.port", () -> String.valueOf(SMTP_PORT));
        registry.add("app.notification.email.from", () -> "noreply@hex4w.local");
    }

    @Autowired
    private EmailPort emailPort;

    private static boolean isMailpitRunning() {
        try {
            HttpClient http = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + MAILPIT_HOST + ":" + API_PORT + "/api/v1/messages"))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void shouldSendEmailAndVerifyInMailpit() throws Exception {
        assumeTrue(isMailpitRunning(),
                "Mailpit must be running on localhost:1025/8025 — start with: " +
                "docker run -d -p 1025:1025 -p 8025:8025 mailpit/mailpit");

        StepVerifier.create(emailPort.send("test@example.com", "Test Subject", "Hello from hex4w"))
                .verifyComplete();

        HttpClient http = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + MAILPIT_HOST + ":" + API_PORT + "/api/v1/messages"))
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("test@example.com");
        assertThat(response.body()).contains("Test Subject");
        assertThat(response.body()).contains("Hello from hex4w");
        // Clear messages after test
        http.send(HttpRequest.newBuilder()
                .uri(URI.create("http://" + MAILPIT_HOST + ":" + API_PORT + "/api/v1/messages"))
                .DELETE()
                .build(), HttpResponse.BodyHandlers.ofString());
    }
}
