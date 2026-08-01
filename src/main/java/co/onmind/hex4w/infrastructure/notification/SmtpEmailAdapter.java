package co.onmind.hex4w.infrastructure.notification;

import co.onmind.hex4w.application.ports.out.EmailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * SMTP implementation of {@link EmailPort}.
 * <p>
 * Wraps the blocking {@link JavaMailSender} in a {@link Mono} scheduled on
 * {@link Schedulers#boundedElastic()} so the Netty event loop is never blocked.
 * Compatible with any SMTP server, including <a href="https://mailpit.envex.net/">Mailpit</a>
 * for testing.
 */
@Component
public class SmtpEmailAdapter implements EmailPort {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailAdapter.class);

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailAdapter(JavaMailSender mailSender,
            @Value("${app.notification.email.from:hex4w@localhost}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public Mono<Void> send(String to, String subject, String body) {
        return send(to, subject, body, this.from, List.of());
    }

    @Override
    public Mono<Void> send(String to, String subject, String body, String from, List<String> cc) {
        return Mono.fromCallable(() -> {
            logger.debug("Sending email to={} cc={} subject={}", to, cc, subject);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from != null && !from.isBlank() ? from : this.from);
            message.setTo(to);
            if (cc != null && !cc.isEmpty()) {
                message.setCc(cc.toArray(new String[0]));
            }
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.debug("Email sent successfully to={}", to);
            return null;
        })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
