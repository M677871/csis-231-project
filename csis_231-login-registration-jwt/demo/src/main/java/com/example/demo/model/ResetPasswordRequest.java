package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body used by the client to complete the password-reset flow.
 *
 * @param emailOrUsername the account identifier (e-mail or username) used to request the reset
 * @param token           the password-reset code (OTP) received by the user
 * @param newPassword     the new password that should be stored for this account
 */
public record ResetPasswordRequest(
        String emailOrUsername,
        @JsonProperty("code") String token,
        String newPassword
) {}
