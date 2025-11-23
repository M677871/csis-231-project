package com.example.demo.auth;

/**
 * DTO representing the 202 OTP challenge returned by the backend.
 */
public class OtpChallengeResponse {
    private boolean otpRequired;
    private String username;
    private String purpose;

    public OtpChallengeResponse() {}

    public boolean isOtpRequired() {
        return otpRequired;
    }

    public void setOtpRequired(boolean otpRequired) {
        this.otpRequired = otpRequired;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
