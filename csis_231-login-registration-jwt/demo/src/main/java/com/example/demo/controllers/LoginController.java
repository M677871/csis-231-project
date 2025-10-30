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

public class LoginController {
    @FXML private TextField username;
    @FXML private PasswordField password;

    public void onLogin() {
        try {
            String u = username.getText() == null ? "" : username.getText().trim();
            String p = password.getText() == null ? "" : password.getText().trim();
            if (u.isEmpty() || p.isEmpty()) { AlertUtils.warn("Please enter username and password."); return; }

            AuthResponse res = AuthApi.login(new LoginRequest(u, p));

            if (res.otpRequired()) {
                TempAuth.username = u;
                try { AuthApi.requestOtp(u); } catch (Exception ignored) {} // best-effort
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

    public void goRegister(){ Launcher.go("register.fxml", "Register"); }
    public void goForgot(){ Launcher.go("forget.fxml", "Forgot Password"); }
}
