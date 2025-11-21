package com.example.demo.model;

/**
 * Authentication response returned by the backend to the desktop client.
 *
 * <p>If OTP-based login is enabled for the user, the backend returns
 * {@code otpRequired = true} and no token. In that case the client must
 * navigate to the OTP screen and verify a code before getting a token.</p>
 *
 * <p>If OTP is not required, {@code otpRequired = false} and {@code token}
 * contains the issued JWT.</p>
 *
 * @param otpRequired {@code true} if an OTP must be verified before issuing a token;
 *                    {@code false} if the {@code token} field already contains a JWT
 * @param token       the JSON Web Token (JWT) returned by the backend when OTP is not required;
 *                    may be {@code null} or blank when {@code otpRequired} is {@code true}
 */

public record AuthResponse(boolean otpRequired, String token) {}
