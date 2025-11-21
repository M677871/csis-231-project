package com.example.demo.controllers;

import com.example.demo.Launcher;
import com.example.demo.api.AuthApi;
import com.example.demo.security.TempAuth;
import com.example.demo.util.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the "forgot password" screen.
 *
 * <p>The user can enter either an e-mail address or username. The controller
 * sends a request to the backend to start the password-reset flow and then
 * navigates to the reset screen.</p>
 */

public class ForgotController {
    @FXML private TextField emailOrUsername;

    /**
     * Sends a password-reset request for the value entered in the text field.
     *
     * <p>If the field is empty, a warning dialog is shown. Otherwise
     * {@link com.example.demo.api.AuthApi#requestResetOtp(String)} is called
     * and, when the value looks like an e-mail address, it is also stored in
     * {@link com.example.demo.security.TempAuth#resetEmail} so that the reset
     * screen can prefill it.</p>
     *
     * <p>On success the user is navigated to the reset password screen.</p>
     */

    public void onSend(){
        try {
            String v = emailOrUsername.getText()==null? "": emailOrUsername.getText().trim();
            if (v.isEmpty()) { AlertUtils.warn("Enter your email or username."); return; }

            AuthApi.requestResetOtp(v);
            // If user typed an email, remember it to prefill reset screen
            if (v.contains("@")) TempAuth.resetEmail = v;

            AlertUtils.info("If the account exists, a reset code has been sent.");
            Launcher.go("reset.fxml", "Reset Password");
        } catch (Exception ex) {
            AlertUtils.error("Failed to send reset link: " + ex.getMessage());
        }
    }

    /**
     * Navigates back from the "forgot password" screen to the login screen.
     */

    public void goLogin(){ Launcher.go("login.fxml", "Login"); }
}
