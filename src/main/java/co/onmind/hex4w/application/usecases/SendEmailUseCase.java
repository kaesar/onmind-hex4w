package co.onmind.hex4w.application.usecases;

import co.onmind.hex4w.application.dto.in.SendEmailRequestDto;
import co.onmind.hex4w.application.ports.in.SendEmailTrait;
import co.onmind.hex4w.application.ports.out.EmailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class SendEmailUseCase implements SendEmailTrait {

    private static final Logger logger = LoggerFactory.getLogger(SendEmailUseCase.class);

    private final EmailPort emailPort;

    public SendEmailUseCase(EmailPort emailPort) {
        this.emailPort = emailPort;
    }

    @Override
    public Mono<Void> sendEmail(SendEmailRequestDto request) {
        logger.debug("SendEmailUseCase: dispatching email to={}", request.to());
        return emailPort.send(
                request.to(),
                request.subject(),
                request.body(),
                request.from(),
                request.cc() != null ? request.cc() : List.of()
        );
    }
}
