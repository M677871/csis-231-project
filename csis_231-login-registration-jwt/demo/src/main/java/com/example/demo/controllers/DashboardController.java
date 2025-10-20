package com.example.demo.controllers;

import com.example.demo.Launcher;
import com.example.demo.api.AuthApi;
import com.example.demo.model.MeResponse;
import com.example.demo.security.TokenStore;
import com.example.demo.util.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {
    @FXML private Label welcome;
    @FXML private Label name;
    @FXML private Label email;
    @FXML private Label role;

    @FXML
    public void initialize() {
        // Set some safe defaults so the view renders even if network fails.
        welcome.setText("Welcome");
        name.setText("");
        email.setText("");
        role.setText("");

        // Do network fetch AFTER scene shows to avoid FXMLLoader errors.
        Platform.runLater(() -> {
            try {
                if (!TokenStore.hasToken()) {
                    AlertUtils.warn("No token present. Please login again.");
                    Launcher.go("login.fxml", "Login");
                    return;
                }
                MeResponse me = AuthApi.me();
                String full = me.fullName();
                welcome.setText(full.isBlank() ? "Welcome" : ("Welcome, " + full));
                name.setText(full);
                email.setText(me.email());
                role.setText(me.role());
            } catch (Exception ex) {
                AlertUtils.error("Failed to load profile: " + ex.getMessage());
            }
        });
    }

    public void onLogout() {
        TokenStore.clear();
        Launcher.go("login.fxml", "Login");
    }
}
