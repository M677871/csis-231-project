package com.example.demo;

import com.example.demo.api.AuthClient;
import com.example.demo.api.BackendClient;
import com.example.demo.model.AuthResponse;
import com.example.demo.model.RegisterRequest;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {
    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button registerButton;

    @FXML
    private Hyperlink loginLink;

    private final AuthClient authClient = new AuthClient();

    @FXML
    protected void onRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please fill in all required fields").showAndWait();
            return;
        }

        if (password.length() < 6) {
            new Alert(Alert.AlertType.WARNING, "Password must be at least 6 characters long").showAndWait();
            return;
        }

        try {
            RegisterRequest request = new RegisterRequest(username, email, password, firstName, lastName);
            AuthResponse response = authClient.register(request);

            // Store authentication token
            BackendClient backendClient = new BackendClient();
            backendClient.setAuthToken(response.getToken());

            // Open main application
            openMainApplication(backendClient, response);

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Registration failed: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    protected void onLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 400, 400);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("Login");
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();

            // Close register window
            ((Stage) registerButton.getScene().getWindow()).close();

        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to open login form: " + e.getMessage()).showAndWait();
        }
    }

    private void openMainApplication(BackendClient backendClient, AuthResponse authResponse) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/hello-view.fxml"));
            Scene scene = new Scene(loader.load(), 900, 600);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            HelloController controller = loader.getController();
            controller.setBackendClient(backendClient);
            controller.setAuthResponse(authResponse);

            Stage stage = new Stage();
            stage.setTitle("Customer Management System - Welcome " + authResponse.getUsername());
            stage.setMinWidth(800);
            stage.setMinHeight(500);
            stage.setScene(scene);
            stage.show();

            // Close register window
            ((Stage) registerButton.getScene().getWindow()).close();

        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to open main application: " + e.getMessage()).showAndWait();
        }
    }
}
