package com.csis231.api.auth.Otp;

import com.csis231.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * {@link OtpSender} implementation that delivers OTP codes via e-mail using
 * Spring's {@link org.springframework.mail.javamail.JavaMailSender}.
 *
 * <p>The generated message contains the OTP code, its logical purpose and the
 * expiration time, and is sent to the e-mail address associated with the
 * {@link com.csis231.api.user.User}.</p>
 */

@Component
@RequiredArgsConstructor
public class EmailOtpSender implements OtpSender {

    private final JavaMailSender mailSender;

    /**
     * Sends the provided OTP code to the e-mail address of the given user.
     *
     * @param user      the recipient; their e-mail address is taken from the {@code user} entity
     * @param purpose   the logical purpose of the OTP (for example
     *                  {@link OtpPurposes#LOGIN_2FA})
     * @param code      the one-time password that should be delivered
     * @param expiresAt the instant at which the code will expire and no longer be accepted
     */

    @Override
    public void send(User user, String purpose, String code, Instant expiresAt) {
        String subject = switch (purpose) {
            case OtpPurposes.LOGIN_2FA -> "Your login verification code";
            case OtpPurposes.PASSWORD_RESET -> "Your password reset code";
            default -> "Your verification code";
        };

        String expiresAtLocal = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(expiresAt);

        // plain text body
        String text = "Hello " + (user.getFirstName() != null ? user.getFirstName() : user.getUsername()) + ",\n\n"
                + "Your one-time code is: " + code + "\n"
                + "It will expire at: " + expiresAtLocal + "\n\n"
                + "If you did not request this, you can ignore this email.";

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(user.getEmail());              // <- send to the user who is logging in
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    }
}
