package com.example.demo.controllers;

import com.example.demo.Launcher;
import com.example.demo.api.AuthApi;
import com.example.demo.model.RegisterRequest;
import com.example.demo.util.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField username;
    @FXML private TextField email;
    @FXML private PasswordField password;
    @FXML private PasswordField confirmPassword;
    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private TextField phone;           // optional
    @FXML private ComboBox<String> role;     // "Student" | "Instructor"

    @FXML
    private void initialize() {
        // make sure choices exist even if not set in FXML
        if (role != null && role.getItems().isEmpty()) {
            role.getItems().addAll("Student", "Instructor");
        }
        // optional: choose a sensible default
        if (role != null && role.getValue() == null) {
            role.setValue("Student");
        }
    }

    @FXML
    private void onRegister() {
        try {
            String u  = safe(username);
            String e  = safe(email);
            String p  = safe(password);
            String cp = safe(confirmPassword);
            String f  = safe(firstName);
            String l  = safe(lastName);
            String ph = safe(phone);
            String r  = (role == null || role.getValue() == null) ? "" : role.getValue().trim();

            // required checks
            if (u.isEmpty() || e.isEmpty() || p.isEmpty() || cp.isEmpty()) {
                AlertUtils.warn("Fill all required fields (username, email, password, confirm).");
                return;
            }
            if (!p.equals(cp)) {
                AlertUtils.warn("Passwords do not match.");
                return;
            }
            if (r.isEmpty()) {
                AlertUtils.warn("Please choose a role (Student or Instructor).");
                return;
            }

            // map to backend enum values
            String roleEnum = mapRoleToEnum(r); // STUDENT | INSTRUCTOR

            RegisterRequest req = new RegisterRequest(
                    u,           // username
                    e,           // email
                    p,           // password
                    emptyToNull(f),
                    emptyToNull(l),
                    emptyToNull(ph),
                    roleEnum     // role must be enum string
            );

            AuthApi.register(req);
            AlertUtils.info("Account created. Please login.");
            Launcher.go("login.fxml", "Login");
        } catch (Exception ex) {
            com.example.demo.util.AlertUtils.warn(ex.getMessage());
        }
    }

    @FXML
    private void goLogin() {
        Launcher.go("login.fxml", "Login");
    }

    // helpers
    private static String safe(TextField t) {
        return (t == null || t.getText() == null) ? "" : t.getText().trim();
    }
    private static String safe(PasswordField t) {
        return (t == null || t.getText() == null) ? "" : t.getText().trim();
    }
    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
    private static String mapRoleToEnum(String r) {
        return r.equalsIgnoreCase("instructor") ? "INSTRUCTOR" : "STUDENT";
    }
}
