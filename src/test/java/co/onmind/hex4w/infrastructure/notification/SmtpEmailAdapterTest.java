package co.onmind.hex4w.infrastructure.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpEmailAdapterTest {

    @Mock
    private JavaMailSender mailSender;

    private SmtpEmailAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SmtpEmailAdapter(mailSender, "noreply@hex4w.local");
    }

    @Test
    @DisplayName("Sends email with correct fields")
    void sendsEmailWithCorrectFields() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        StepVerifier.create(adapter.send("user@example.com", "Hello", "World"))
                .verifyComplete();

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("noreply@hex4w.local");
        assertThat(sent.getTo()).containsExactly("user@example.com");
        assertThat(sent.getSubject()).isEqualTo("Hello");
        assertThat(sent.getText()).isEqualTo("World");
    }

    @Test
    @DisplayName("Sends email with custom from and cc")
    void sendsEmailWithCustomFromAndCc() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        StepVerifier.create(adapter.send("user@example.com", "Test", "Body",
                "custom@hex4w.local", List.of("cc1@example.com", "cc2@example.com")))
                .verifyComplete();

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("custom@hex4w.local");
        assertThat(sent.getTo()).containsExactly("user@example.com");
        assertThat(sent.getCc()).containsExactly("cc1@example.com", "cc2@example.com");
        assertThat(sent.getSubject()).isEqualTo("Test");
        assertThat(sent.getText()).isEqualTo("Body");
    }
}
