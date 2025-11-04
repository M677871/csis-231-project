package com.example.demo.controllers;

import com.example.demo.Launcher;
import com.example.demo.api.UserApi;
import com.example.demo.model.User;
import com.example.demo.security.TokenStore;
import com.example.demo.util.AlertUtils;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UserController {

    // Table (Users only)
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Long>    userIdColumn;
    @FXML private TableColumn<User, String>  userNameColumn;
    @FXML private TableColumn<User, String>  userEmailColumn;
    @FXML private TableColumn<User, String>  firstNameColumn;
    @FXML private TableColumn<User, String>  lastNameColumn;
    @FXML private TableColumn<User, String>  phoneColumn;
    @FXML private TableColumn<User, String>  userRoleColumn;
    @FXML private TableColumn<User, Boolean> activeColumn;
    @FXML private TableColumn<User, Boolean> twoFaColumn;
    @FXML private TableColumn<User, Boolean> emailVerColumn;

    // Form
    @FXML private TextField userNameField, userEmailField, firstNameField, lastNameField, phoneField;
    @FXML private PasswordField userPasswordField;
    @FXML private ChoiceBox<String> userRoleChoiceBox;
    @FXML private CheckBox activeCheck, twoFaCheck, emailVerifiedCheck;

    // Toolbar filters
    @FXML private TextField userSearchField;
    @FXML private ChoiceBox<String> roleFilter;          // "All Roles", "STUDENT", "INSTRUCTOR", "ADMIN"
    @FXML private ChoiceBox<String> activeFilterChoice;  // "All", "Active", "Inactive"
    @FXML private Label userCountLabel;

    private final UserApi userApi = new UserApi();

    // Data pipeline (strictly User)
    private final ObservableList<User> master   = FXCollections.observableArrayList();
    private final FilteredList<User>   filtered = new FilteredList<>(master, u -> true);
    private final SortedList<User>     sorted   = new SortedList<>(filtered);

    @FXML
    public void initialize() {
        // Column value factories
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        userEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        userRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("isActive"));
        twoFaColumn.setCellValueFactory(new PropertyValueFactory<>("twoFactorEnabled"));
        emailVerColumn.setCellValueFactory(new PropertyValueFactory<>("emailVerified"));

        // Optional: ✓/✗ render
        activeColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : (Boolean.TRUE.equals(v) ? "✓" : "✗"));
            }
        });
        twoFaColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : (Boolean.TRUE.equals(v) ? "✓" : "✗"));
            }
        });
        emailVerColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : (Boolean.TRUE.equals(v) ? "✓" : "✗"));
            }
        });

        // Sort + show all rows
        sorted.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sorted);

        // Selection -> form
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
                userPasswordField.clear();
            }
        });

        // Form role choices
        userRoleChoiceBox.getItems().setAll("STUDENT","INSTRUCTOR","ADMIN");
        userRoleChoiceBox.setValue("STUDENT");

        // Toolbar defaults
        if (roleFilter.getSelectionModel().isEmpty()) {
            roleFilter.getItems().setAll("All Roles","STUDENT","INSTRUCTOR","ADMIN");
            roleFilter.getSelectionModel().select("All Roles");
        }
        if (activeFilterChoice.getSelectionModel().isEmpty()) {
            activeFilterChoice.getItems().setAll("All","Active","Inactive");
            activeFilterChoice.getSelectionModel().select("All");
        }

        // Live filtering
        ChangeListener<Object> refilter = (obs, o, n) -> refreshUserFilters();
        userSearchField.textProperty().addListener(refilter);
        roleFilter.getSelectionModel().selectedItemProperty().addListener(refilter);
        activeFilterChoice.getSelectionModel().selectedItemProperty().addListener(refilter);

        // Count updates
        filtered.addListener((ListChangeListener<User>) c -> updateUserCount());

        // Load data
        loadUsers();
    }

    private void loadUsers() {
        CompletableFuture.runAsync(() -> {
            try {
                List<User> list = userApi.list(); // USERS ONLY
                Platform.runLater(() -> {
                    master.setAll(list);
                    refreshUserFilters(); // applies defaults and shows all rows
                    updateUserCount();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.error("Failed to load users: " + ex.getMessage()));
            }
        });
    }

    private void refreshUserFilters() {
        final String q = safeLower(userSearchField.getText());
        final String roleSel   = roleFilter.getSelectionModel().getSelectedItem();
        final String activeSel = activeFilterChoice.getSelectionModel().getSelectedItem();

        filtered.setPredicate(u -> {
            if (u == null) return false;

            // ✅ Show ALL when search box is empty
            boolean textOk = q.isEmpty()
                    || contains(u.getUsername(), q)
                    || contains(u.getEmail(), q)
                    || contains(u.getFirstName(), q)
                    || contains(u.getLastName(), q)
                    || contains(u.getPhone(), q);

            boolean roleOk = (roleSel == null || "All Roles".equals(roleSel))
                    || (u.getRole() != null && u.getRole().equalsIgnoreCase(roleSel));

            boolean activeOk = (activeSel == null || "All".equals(activeSel))
                    || ("Active".equals(activeSel) && Boolean.TRUE.equals(u.getIsActive()))
                    || ("Inactive".equals(activeSel) && Boolean.FALSE.equals(u.getIsActive()));

            return textOk && roleOk && activeOk;
        });
    }

    private void updateUserCount() {
        userCountLabel.setText(filtered.size() + " items");
    }

    // ---- CRUD ----

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
                    master.add(created);
                    clearForm();
                    updateUserCount();
                    // No need to call refresh; current predicate already shows all when q is empty
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
                if (pw != null && !pw.isBlank()) u.setPassword(pw);

                u.setFirstName(trimOrNull(firstNameField.getText()));
                u.setLastName(trimOrNull(lastNameField.getText()));
                u.setPhone(trimOrNull(phoneField.getText()));
                u.setRole(userRoleChoiceBox.getValue());
                u.setIsActive(activeCheck.isSelected());
                u.setTwoFactorEnabled(twoFaCheck.isSelected());
                u.setEmailVerified(emailVerifiedCheck.isSelected());

                User updated = userApi.update(u);
                Platform.runLater(() -> {
                    for (int i = 0; i < master.size(); i++) {
                        if (master.get(i).getId().equals(selected.getId())) {
                            master.set(i, updated);
                            break;
                        }
                    }
                    clearForm();
                    updateUserCount();
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
                    master.removeIf(u -> u.getId().equals(selected.getId()));
                    clearForm();
                    updateUserCount();
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
    private static boolean contains(String s, String q) { return s != null && !q.isEmpty() && s.toLowerCase().contains(q); }
    private static String safeLower(String s) { return s == null ? "" : s.toLowerCase().trim(); }

    @FXML private void backToMain() { Launcher.go("dashboard.fxml", "Dashboard"); }
    @FXML public void onLogout() {
        TokenStore.clear();
        Launcher.go("login.fxml", "Login");
    }
}
