package com.csis231.api.auth.Otp;

import com.csis231.api.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpCodeRepository repo;
    private final JavaMailSender mailSender;

    @Value("${mail.from:}")
    private String from;

    private static final long TTL_SECONDS = 300; // 5 minutes

    @Transactional
    public void createAndSend(User user, String purpose) {
        // Invalidate any still-active codes for this user/purpose
        List<OtpCode> actives = repo.findActive(user.getId(), purpose, Instant.now());
        actives.forEach(c -> c.setConsumedAt(Instant.now()));

        // Generate a 6-digit code
        String code = String.format("%06d",
                ThreadLocalRandom.current().nextInt(0, 1_000_000));

        // Persist
        OtpCode entity = OtpCode.builder()
                .user(user)
                .code(code)
                .purpose(purpose)
                .expiresAt(Instant.now().plusSeconds(TTL_SECONDS))
                .build();
        repo.save(entity);

        // Try email (if configured), always log for dev
        try {
            if (from != null && !from.isBlank() && user.getEmail() != null) {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(from);
                msg.setTo(user.getEmail());
                msg.setSubject("Your OTP code");
                msg.setText("Your one-time code is: " + code + " (valid 5 minutes)");
                mailSender.send(msg);
            }
        } catch (Exception e) {
            log.warn("Failed to send OTP email: {}", e.getMessage());
        }

        log.info("OTP for user={} purpose={} CODE={}", user.getUsername(), purpose, code);
    }

    @Transactional
    public boolean verify(User user, String purpose, String code) {
        Instant now = Instant.now();

        Optional<OtpCode> latest = repo.findTopByUserIdAndPurposeOrderByIdDesc(user.getId(), purpose);

        if (latest.isEmpty()) return false;

        OtpCode c = latest.get();
        boolean ok = c.getConsumedAt() == null
                && now.isBefore(c.getExpiresAt())
                && c.getCode().equals(code);

        if (ok) {
            c.setConsumedAt(now);     // consume it
        }
        return ok;
    }

}
