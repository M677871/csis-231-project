package com.example.demo;

import com.example.demo.api.AuthClient;
import com.example.demo.api.BackendClient;
import com.example.demo.model.AuthResponse;
import com.example.demo.model.LoginRequest;
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

public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Hyperlink registerLink;

    private final AuthClient authClient = new AuthClient();

    @FXML
    protected void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please enter both username and password").showAndWait();
            return;
        }

        try {
            LoginRequest request = new LoginRequest(username, password);
            AuthResponse response = authClient.login(request);

            // Store authentication token
            BackendClient backendClient = new BackendClient();
            backendClient.setAuthToken(response.getToken());

            // Open main application
            openMainApplication(backendClient, response);

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Login failed: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    protected void onRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/fxml/register-view.fxml"));
            Scene scene = new Scene(loader.load(), 450, 600);
            scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("Register");
            stage.setResizable(true);
            stage.setMinWidth(400);
            stage.setMinHeight(500);
            stage.setScene(scene);
            stage.show();

            // Close login window
            ((Stage) loginButton.getScene().getWindow()).close();

        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to open registration form: " + e.getMessage()).showAndWait();
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

            // Close login window
            ((Stage) loginButton.getScene().getWindow()).close();

        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to open main application: " + e.getMessage()).showAndWait();
        }
    }
}
