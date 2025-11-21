package com.example.demo.controllers;

import com.example.demo.Launcher;
import com.example.demo.api.AuthApi;
import com.example.demo.model.AuthResponse;
import com.example.demo.model.LoginRequest;
import com.example.demo.security.TempAuth;
import com.example.demo.security.TokenStore;
import com.example.demo.util.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the login screen of the desktop client.
 *
 * <p>This controller:</p>
 * <ul>
 *   <li>Reads the username and password from the UI fields</li>
 *   <li>Calls {@link com.example.demo.api.AuthApi#login(com.example.demo.model.LoginRequest)}
 *       to authenticate against the backend</li>
 *   <li>If the backend requires OTP, stores the username in
 *       {@link com.example.demo.security.TempAuth} and navigates to the OTP screen</li>
 *   <li>Otherwise stores the JWT in {@link com.example.demo.security.TokenStore}
 *       and opens the main dashboard</li>
 * </ul>
 */

public class LoginController {
    @FXML private TextField username;
    @FXML private PasswordField password;

    /**
     * Handles the login button action.
     *
     * <p>Validates that both username and password are non-blank. If either is
     * empty, a warning dialog is shown. Otherwise a login request is sent to
     * the backend and the result is handled as follows:</p>
     * <ul>
     *   <li>If {@link AuthResponse#otpRequired()} is {@code true}, the username
     *       is stored in {@link TempAuth#username} and the OTP screen is opened.</li>
     *   <li>Otherwise the JWT from {@link AuthResponse#token()} is stored in
     *       {@link TokenStore} and the dashboard screen is opened.</li>
     * </ul>
     */

    public void onLogin() {
        try {
            String u = username.getText() == null ? "" : username.getText().trim();
            String p = password.getText() == null ? "" : password.getText().trim();
            if (u.isEmpty() || p.isEmpty()) { AlertUtils.warn("Please enter username and password."); return; }

            AuthResponse res = AuthApi.login(new LoginRequest(u, p));

            if (res.otpRequired()) {
                TempAuth.username = u;
                AlertUtils.info("We’ve sent a 6-digit code to your email. Please check your inbox.");
                Launcher.go("otp.fxml", "Verify OTP");
            } else {
                TokenStore.set(res.token());
                Launcher.go("dashboard.fxml", "Dashboard");
            }
        } catch (Exception ex) {
            AlertUtils.error("Login failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    /**
     * Navigates from the login screen to the registration screen.
     */

    public void goRegister(){ Launcher.go("register.fxml", "Register"); }

    /**
     * Navigates from the login screen to the "forgot password" screen.
     */

    public void goForgot(){ Launcher.go("forget.fxml", "Forgot Password"); }
}
