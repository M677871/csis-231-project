package com.csis231.api.auth.Otp;

import com.csis231.api.user.User;

import java.time.Instant;
/**
 * Strategy interface for sending one-time password (OTP) codes to a user.
 *
 * <p>Typical implementations deliver the code via e-mail, SMS or any other
 * out-of-band channel. The interface is intentionally small so that it can
 * easily be mocked in tests.</p>
 */
public interface OtpSender {
    /**
     * Sends the given OTP code to the specified user.
     *
     * @param user      the user who should receive the OTP; normally the e-mail or
     *                  phone number is taken from this entity
     * @param purpose   the logical purpose of the OTP (for example
     *                  {@link OtpPurposes#LOGIN_2FA} or {@link OtpPurposes#PASSWORD_RESET})
     * @param code      the generated one-time code that will be validated later
     * @param expiresAt the instant at which the code will no longer be accepted
     */
    void send(User user, String purpose, String code, Instant expiresAt);
}
