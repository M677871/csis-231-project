package com.example.demo.model;

/**
 * Request body used to verify an OTP code during the login flow.
 *
 * @param username the username (or e-mail) for which the OTP was issued
 * @param code     the one-time password (OTP) entered by the user
 */
public record OtpVerifyRequest(String username, String code) {}
