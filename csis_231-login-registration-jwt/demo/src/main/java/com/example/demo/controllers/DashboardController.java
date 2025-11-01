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
        welcome.setText("Welcome");
        name.setText(""); email.setText(""); role.setText("");

        Platform.runLater(() -> {
            try {
                if (!TokenStore.hasToken()) {
                    AlertUtils.warn("No token present. Please login again.");
                    Launcher.go("login.fxml", "Login");
                    return;
                }
                MeResponse me = AuthApi.me(); // your tolerant/profile call
                String full = (me.getFirstName() == null ? "" : me.getFirstName());
                if (me.getLastName() != null && !me.getLastName().isBlank()) full += (full.isBlank() ? "" : " ") + me.getLastName();
                if (full.isBlank()) full = me.getUsername();

                welcome.setText(full.isBlank() ? "Welcome" : ("Welcome, " + full));
                name.setText(full);
                email.setText(me.getEmail());
                role.setText(me.getRole());
            } catch (Exception ex) {
                AlertUtils.error("Failed to load profile: " + ex.getMessage());
            }
        });
    }

    @FXML private void openUsers()      { Launcher.go("user_dashboard.fxml", "Users"); }
    @FXML private void openCategories() { Launcher.go("category.fxml", "Categories"); }

    @FXML
    public void onLogout() {
        TokenStore.clear();
        Launcher.go("login.fxml", "Login");
    }
}
