package com.example.demo.controllers;

import com.example.demo.Launcher;
import com.example.demo.api.UserApi;
import com.example.demo.model.User;
import com.example.demo.security.TokenStore;
import com.example.demo.util.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UserController {

    // table
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Long>   userIdColumn;
    @FXML private TableColumn<User, String> userNameColumn;
    @FXML private TableColumn<User, String> userEmailColumn;
    @FXML private TableColumn<User, String> firstNameColumn;
    @FXML private TableColumn<User, String> lastNameColumn;
    @FXML private TableColumn<User, String> phoneColumn;
    @FXML private TableColumn<User, String> userRoleColumn;
    @FXML private TableColumn<User, Boolean> activeColumn;
    @FXML private TableColumn<User, Boolean> twoFaColumn;
    @FXML private TableColumn<User, Boolean> emailVerColumn;

    // form
    @FXML private TextField userNameField, userEmailField, firstNameField, lastNameField, phoneField;
    @FXML private PasswordField userPasswordField;
    @FXML private ChoiceBox<String> userRoleChoiceBox;
    @FXML private CheckBox activeCheck, twoFaCheck, emailVerifiedCheck;

    private final UserApi userApi = new UserApi();

    @FXML
    public void initialize() {
        // columns — match your backend POJO property names exactly
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        userEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        userRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("isActive")); // Lombok generates getActive() for Boolean isActive
        twoFaColumn.setCellValueFactory(new PropertyValueFactory<>("twoFactorEnabled"));
        emailVerColumn.setCellValueFactory(new PropertyValueFactory<>("emailVerified"));

        userRoleChoiceBox.getItems().setAll("STUDENT","INSTRUCTOR","ADMIN");
        userRoleChoiceBox.setValue("STUDENT");

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, u) -> {
            if (u != null) {
                userNameField.setText(u.getUsername());
                userEmailField.setText(u.getEmail());
                firstNameField.setText(u.getFirstName());
                lastNameField.setText(u.getLastName());
                phoneField.setText(u.getPhone());
                userRoleChoiceBox.setValue(u.getRole());
                activeCheck.setSelected(Boolean.TRUE.equals(u.getIsActive()));
                twoFaCheck.setSelected(Boolean.TRUE.equals(u.getTwoFactorEnabled()));
                emailVerifiedCheck.setSelected(Boolean.TRUE.equals(u.getEmailVerified()));
                userPasswordField.clear(); // never show old password
            }
        });

        loadUsers();
    }

    private void loadUsers() {
        CompletableFuture.runAsync(() -> {
            try {
                List<User> list = userApi.list();
                Platform.runLater(() -> userTable.getItems().setAll(list));
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.error("Failed to load users: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void onAddUser() {
        String username = userNameField.getText().trim();
        String email    = userEmailField.getText().trim();
        String password = userPasswordField.getText();
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            AlertUtils.warn("Please fill in username, email and password.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                User u = new User();
                u.setUsername(username);
                u.setEmail(email);
                u.setPassword(password);
                u.setFirstName(trimOrNull(firstNameField.getText()));
                u.setLastName(trimOrNull(lastNameField.getText()));
                u.setPhone(trimOrNull(phoneField.getText()));
                u.setRole(userRoleChoiceBox.getValue());
                u.setIsActive(activeCheck.isSelected());
                u.setTwoFactorEnabled(twoFaCheck.isSelected());
                u.setEmailVerified(emailVerifiedCheck.isSelected());

                User created = userApi.create(u);
                Platform.runLater(() -> {
                    userTable.getItems().add(created);
                    clearForm();
                    AlertUtils.info("User added successfully!");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.error("Failed to add user: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void onUpdateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtils.warn("Please select a user to update."); return; }

        String username = userNameField.getText().trim();
        String email    = userEmailField.getText().trim();
        if (username.isEmpty() || email.isEmpty()) {
            AlertUtils.warn("Username and email cannot be empty.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                User u = new User();
                u.setId(selected.getId());
                u.setUsername(username);
                u.setEmail(email);
                String pw = userPasswordField.getText();
                if (pw != null && !pw.isBlank()) u.setPassword(pw); // optional change

                u.setFirstName(trimOrNull(firstNameField.getText()));
                u.setLastName(trimOrNull(lastNameField.getText()));
                u.setPhone(trimOrNull(phoneField.getText()));
                u.setRole(userRoleChoiceBox.getValue());
                u.setIsActive(activeCheck.isSelected());
                u.setTwoFactorEnabled(twoFaCheck.isSelected());
                u.setEmailVerified(emailVerifiedCheck.isSelected());

                User updated = userApi.update(u);
                Platform.runLater(() -> {
                    int idx = userTable.getItems().indexOf(selected);
                    if (idx >= 0) userTable.getItems().set(idx, updated);
                    clearForm();
                    AlertUtils.info("User updated successfully!");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.error("Failed to update user: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void onDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtils.warn("Please select a user to delete."); return; }
        CompletableFuture.runAsync(() -> {
            try {
                userApi.delete(selected.getId());
                Platform.runLater(() -> {
                    userTable.getItems().remove(selected);
                    clearForm();
                    AlertUtils.info("User deleted successfully!");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.error("Failed to delete user: " + ex.getMessage()));
            }
        });
    }

    private void clearForm() {
        userNameField.clear();
        userEmailField.clear();
        userPasswordField.clear();
        firstNameField.clear();
        lastNameField.clear();
        phoneField.clear();
        userRoleChoiceBox.setValue("STUDENT");
        activeCheck.setSelected(true);
        twoFaCheck.setSelected(false);
        emailVerifiedCheck.setSelected(false);
        userTable.getSelectionModel().clearSelection();
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    @FXML private void backToMain() { Launcher.go("dashboard.fxml", "Dashboard"); }
    @FXML public void onLogout() {
        TokenStore.clear();
        Launcher.go("login.fxml", "Login");
    }
}
