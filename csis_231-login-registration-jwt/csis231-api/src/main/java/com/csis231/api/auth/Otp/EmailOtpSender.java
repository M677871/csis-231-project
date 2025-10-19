package com.csis231.api.auth.Otp;

import com.csis231.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
// to email sent
@Component
@RequiredArgsConstructor
public class EmailOtpSender implements OtpSender {

    private final JavaMailSender mailSender;

    @Value("${mail.from:}")
    private String from;

    @Override
    public void send(User user, String purpose, String code, Instant expiresAt) {
        var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                .withZone(ZoneId.systemDefault());

        String subject = "[CSIS231] Your " + purpose + " code";
        String text =
                "Hello " + (user.getFirstName() != null ? user.getFirstName() : user.getUsername()) + ",\n\n" +
                        "Your one-time code is: " + code + "\n" +
                        "It will expire at: " + fmt.format(expiresAt) + "\n\n" +
                        "If you didn’t request this, you can ignore this email.";

        SimpleMailMessage msg = new SimpleMailMessage();
        if (from != null && !from.isBlank()) msg.setFrom(from);
        msg.setTo(user.getEmail());
        msg.setSubject(subject);
        msg.setText(text);

        mailSender.send(msg);
    }
}
