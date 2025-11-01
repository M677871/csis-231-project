package com.example.demo.controllers;

import com.example.demo.Launcher;
import com.example.demo.api.AuthApi;
import com.example.demo.api.UserApi;
import com.example.demo.api.CategoryApi;
import com.example.demo.model.Category;
import com.example.demo.model.MeResponse;
import com.example.demo.model.User;
import com.example.demo.security.TokenStore;
import com.example.demo.util.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the enhanced admin dashboard.  Displays the current
 * user’s profile and provides CRUD operations for users and categories.
 * The associated FXML defines fx:id values for all UI controls used here.
 */
public class DashboardController {

    // Profile labels (from original dashboard)
    @FXML private Label welcome;
    @FXML private Label name;
    @FXML private Label email;
    @FXML private Label role;

    // User management controls
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Long> userIdColumn;
    @FXML private TableColumn<User, String> userNameColumn;
    @FXML private TableColumn<User, String> userEmailColumn;
    @FXML private TableColumn<User, String> userRoleColumn;
    @FXML private TextField userNameField;
    @FXML private TextField userEmailField;
    @FXML private PasswordField userPasswordField;
    @FXML private ChoiceBox<String> userRoleChoiceBox;

    // Category management controls
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Long> categoryIdColumn;
    @FXML private TableColumn<Category, String> categoryNameColumn;
    @FXML private TextField categoryNameField;

    // APIs for backend communication (assumes these exist in your project)
    private final UserApi userApi = new UserApi();
    private final CategoryApi categoryApi = new CategoryApi();

    @FXML
    public void initialize() {
        // Set safe defaults so the view renders even if network fails.
        welcome.setText("Welcome");
        name.setText("");
        email.setText("");
        role.setText("");

        // Configure table columns for users
        // (PropertyValueFactory assumes User has getId(), getName(), etc.)
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        userEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        userRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Configure table columns for categories
        categoryIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        categoryNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Populate role choice box
        userRoleChoiceBox.getItems().setAll("STUDENT", "INSTRUCTOR", "ADMIN");
        userRoleChoiceBox.setValue("STUDENT");

        // Load profile and then fetch users/categories on the FX thread
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
                email.setText(me.getEmail());
                role.setText(me.getRole());



                loadUsers();
                loadCategories();

                // Preselect first item when a user is selected to populate form
                userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) {
                        userNameField.setText(newSel.getUsername());
                        userEmailField.setText(newSel.getEmail());
                        userRoleChoiceBox.setValue(newSel.getRole());
                        userPasswordField.clear();
                    }
                });

                categoryTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) {
                        categoryNameField.setText(newSel.getName());
                    }
                });

            } catch (Exception ex) {
                AlertUtils.error("Failed to load profile: " + ex.getMessage());
            }
        });
    }

    /** Load all users asynchronously and populate the table. */
    private void loadUsers() {
        CompletableFuture.runAsync(() -> {
            try {
                List<User> list = userApi.list();
                Platform.runLater(() -> userTable.getItems().setAll(list));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> AlertUtils.error("Failed to load users: " + ex.getMessage()));
            }
        });
    }

    /** Load all categories asynchronously and populate the table. */
    private void loadCategories() {
        CompletableFuture.runAsync(() -> {
            try {
                List<Category> list = categoryApi.list();
                Platform.runLater(() -> categoryTable.getItems().setAll(list));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> AlertUtils.error("Failed to load categories: " + ex.getMessage()));
            }
        });
    }

    // ===== User CRUD Handlers =====

    @FXML
    private void onAddUser() {
        String username = userNameField.getText().trim();
        String email    = userEmailField.getText().trim();
        String password = userPasswordField.getText();
        String roleVal  = userRoleChoiceBox.getValue();
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            AlertUtils.warn("Please fill in username, email and password.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setEmail(email);
                newUser.setPassword(password);
                newUser.setRole(roleVal);
                User created = userApi.create(newUser);
                Platform.runLater(() -> {
                    userTable.getItems().add(created);
                    clearUserForm();
                    AlertUtils.info("User added successfully!");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        AlertUtils.error("Failed to add user: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void onUpdateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warn("Please select a user to update.");
            return;
        }
        String username = userNameField.getText().trim();
        String emailVal = userEmailField.getText().trim();
        if (username.isEmpty() || emailVal.isEmpty()) {
            AlertUtils.warn("Username and email cannot be empty.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                User update = new User();
                update.setId(selected.getId());
                update.setUsername(username);
                update.setEmail(emailVal);
                update.setRole(selected.getRole());
                // Leave password null to avoid changing it
                User updated = userApi.update(update);
                Platform.runLater(() -> {
                    int idx = userTable.getItems().indexOf(selected);
                    if (idx >= 0) {
                        userTable.getItems().set(idx, updated);
                    }
                    clearUserForm();
                    AlertUtils.info("User updated successfully!");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        AlertUtils.error("Failed to update user: " + ex.getMessage()));
            }
        });
    }


    @FXML
    private void onDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warn("Please select a user to delete.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                userApi.delete(selected.getId());
                Platform.runLater(() -> {
                    userTable.getItems().remove(selected);
                    clearUserForm();
                    AlertUtils.info("User deleted successfully!");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> AlertUtils.error("Failed to delete user: " + ex.getMessage()));
            }
        });
    }

    private void clearUserForm() {
        userNameField.clear();
        userEmailField.clear();
        userPasswordField.clear();
        userRoleChoiceBox.setValue("STUDENT");
        userTable.getSelectionModel().clearSelection();
    }

    // ===== Category CRUD Handlers =====

    @FXML
    private void onAddCategory() {
        String name = categoryNameField.getText().trim();
        if (name.isEmpty()) {
            AlertUtils.warn("Please enter a category name.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                Category created = categoryApi.create(name);
                Platform.runLater(() -> {
                    categoryTable.getItems().add(created);
                    clearCategoryForm();
                    AlertUtils.info("Category added successfully!");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> AlertUtils.error("Failed to add category: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void onUpdateCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warn("Please select a category to update.");
            return;
        }
        String nameVal = categoryNameField.getText().trim();
        if (nameVal.isEmpty()) {
            AlertUtils.warn("Category name cannot be empty.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                Category updated = categoryApi.update(selected.getId(), nameVal);
                Platform.runLater(() -> {
                    int idx = categoryTable.getItems().indexOf(selected);
                    if (idx >= 0) {
                        categoryTable.getItems().set(idx, updated);
                    }
                    clearCategoryForm();
                    AlertUtils.info("Category updated successfully!");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> AlertUtils.error("Failed to update category: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void onDeleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.warn("Please select a category to delete.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                categoryApi.delete(selected.getId());
                Platform.runLater(() -> {
                    categoryTable.getItems().remove(selected);
                    clearCategoryForm();
                    AlertUtils.info("Category deleted successfully!");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> AlertUtils.error("Failed to delete category: " + ex.getMessage()));
            }
        });
    }

    private void clearCategoryForm() {
        categoryNameField.clear();
        categoryTable.getSelectionModel().clearSelection();
    }

    // ===== Quick‑Action Scroll Handlers =====
    // These methods can be used to scroll to the user or category section.
    // Implement scrolling logic by adjusting the ScrollPane's vvalue if needed.

    @FXML
    private void scrollToUsers() {
        // If your FXML uses a ScrollPane, you can programmatically scroll
        // to the users section.  Here is a simple placeholder.
        userTable.requestFocus();
    }

    @FXML
    private void scrollToCategories() {
        categoryTable.requestFocus();
    }

    // ===== Logout Handler =====
    @FXML
    public void onLogout() {
        TokenStore.clear();
        Launcher.go("login.fxml", "Login");
    }



}
