package com.example.demo.controllers;

import com.example.demo.Launcher;
import com.example.demo.api.AuthApi;
import com.example.demo.model.MeResponse;
import com.example.demo.security.TokenStore;
import com.example.demo.util.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * JavaFX controller for the main dashboard screen.
 *
 * <p>After login, this screen shows basic profile information about the
 * current user (name, e-mail and role) and provides navigation shortcuts
 * to other management screens such as users and categories.</p>
 */

public class DashboardController {

    @FXML private Label welcome;
    @FXML private Label name;
    @FXML private Label email;
    @FXML private Label role;

    /**
     * Initializes the dashboard after the FXML has been loaded.
     *
     * <p>Using {@link Platform#runLater(Runnable)}, this method:</p>
     * <ul>
     *   <li>Checks that a JWT is present in {@link TokenStore}; if not, the
     *       user is redirected back to the login screen</li>
     *   <li>Calls {@link AuthApi#me()} to fetch the current user's profile</li>
     *   <li>Builds a display name from first and last name (or falls back to
     *       the username) and updates the welcome labels accordingly</li>
     * </ul>
     *
     * <p>If any error occurs while loading the profile, an error dialog is
     * shown to the user.</p>
     */

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

    /**
     * Opens the user management screen.
     */

    @FXML private void openUsers()      { Launcher.go("user_dashboard.fxml", "Users"); }

/**
 * Opens the category management screen.
 */

    @FXML private void openCategories() { Launcher.go("category.fxml", "Categories"); }

    /**
     * Logs the user out by clearing the stored JWT and returning to the login screen.
     */

    @FXML
    public void onLogout() {
        TokenStore.clear();
        Launcher.go("login.fxml", "Login");
    }
}
