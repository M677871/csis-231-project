package com.example.demo.student;

import com.example.demo.Launcher;
import com.example.demo.common.TokenStore;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Placeholder controller for the student dashboard.
 */
public class StudentDashboardController {
    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, Student!");
        }
    }

    @FXML
    public void onLogout() {
        TokenStore.clear();
        Launcher.go("login.fxml", "Login");
    }
}
