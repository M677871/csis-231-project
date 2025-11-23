package com.example.demo.instructor;

import com.example.demo.Launcher;
import com.example.demo.common.TokenStore;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Placeholder controller for the instructor dashboard.
 */
public class InstructorDashboardController {
    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, Instructor!");
        }
    }

    @FXML
    public void onLogout() {
        TokenStore.clear();
        Launcher.go("login.fxml", "Login");
    }
}
