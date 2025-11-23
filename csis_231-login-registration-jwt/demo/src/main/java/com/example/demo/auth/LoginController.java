package com.example.demo.auth;

import com.example.demo.Launcher;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.model.AuthResponse;
import com.example.demo.model.LoginRequest;
import com.example.demo.model.MeResponse;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the login screen of the desktop client.
 *
 * <p>This controller:</p>
 * <ul>
 *   <li>Reads the username and password from the UI fields</li>
 *   <li>Calls {@link com.example.demo.auth.AuthApi#login(com.example.demo.model.LoginRequest)}
 *       to authenticate against the backend</li>
 *   <li>If the backend requires OTP, stores the username in
 *       {@link com.example.demo.auth.TempAuth} and navigates to the OTP screen</li>
 *   <li>Otherwise stores the JWT in {@link com.example.demo.common.TokenStore}
 *       and opens the main dashboard</li>
 * </ul>
 */

public class LoginController {
    private final AuthApi authApi = new AuthApi();
    @FXML private TextField username;
    @FXML private PasswordField password;

    /**
     * Handles the login button action.
     *
     * <p>Validates that both username and password are non-blank. If either is
     * empty, a warning dialog is shown. Otherwise a login request is sent to
 * the backend and the result is handled as follows:</p>
 * <ul>
 *   <li>If {@link AuthResponse#isOtpRequired()} is {@code true}, the username
 *       is stored in {@link TempAuth#username} and the OTP screen is opened.</li>
 *   <li>Otherwise the JWT from {@link AuthResponse#getToken()} is stored and the
 *       appropriate dashboard is opened.</li>
 * </ul>
 */

    public void onLogin() {
        try {
            String u = username.getText() == null ? "" : username.getText().trim();
            String p = password.getText() == null ? "" : password.getText().trim();
            if (u.isEmpty() || p.isEmpty()) { AlertUtils.warn("Please enter username and password."); return; }

            AuthResponse res = authApi.login(new LoginRequest(u, p));

            if (res != null && res.isOtpRequired()) {
                TempAuth.username = u;
                AlertUtils.info("We've sent a 6-digit code to your email. Please check your inbox.");
                Launcher.go("otp.fxml", "Verify OTP");
                return;
            }

            if (res == null || res.getToken() == null || res.getToken().isBlank()) {
                ErrorDialog.showError("Login failed: token missing");
                return;
            }

            MeResponse me = authApi.me();
            SessionStore.setMe(me);
            navigateByRole(me.getRole());
        } catch (ApiException ex) {
            ErrorDialog.showError(ex.getMessage(), ex.getErrorCode());
        } catch (Exception ex) {
            ErrorDialog.showError("Login failed: " + ex.getMessage());
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

    private void navigateByRole(String role) {
        if (role != null && role.equalsIgnoreCase("ADMIN")) {
            Launcher.go("dashboard.fxml", "Admin Dashboard");
        } else if (role != null && role.equalsIgnoreCase("INSTRUCTOR")) {
            Launcher.go("instructor_dashboard.fxml", "Instructor Dashboard");
        } else {
            Launcher.go("student_dashboard.fxml", "Student Dashboard");
        }
    }
}
