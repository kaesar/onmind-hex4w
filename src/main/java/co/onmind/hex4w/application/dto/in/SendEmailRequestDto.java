package co.onmind.hex4w.application.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SendEmailRequestDto(
    @NotBlank(message = "To cannot be blank")
    @Email(message = "To must be a valid email")
    String to,

    @NotBlank(message = "Subject cannot be blank")
    String subject,

    String from,

    List<@Email String> cc,

    @NotBlank(message = "Body cannot be blank")
    String body
) {}
