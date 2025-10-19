package com.csis231.api.auth.Otp;

public class OtpRequiredException extends RuntimeException {
    private final String username;

    public OtpRequiredException(String username) {
        super("OTP required for user: " + username);
        this.username = username;
    }

    public String getUsername() { return username; }
}
