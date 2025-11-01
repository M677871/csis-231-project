package com.example.demo.controllers;

import com.example.demo.Launcher;
import com.example.demo.api.CategoryApi;
import com.example.demo.model.Category;
import com.example.demo.security.TokenStore;
import com.example.demo.util.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CategoryController {

    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Long>   categoryIdColumn;
    @FXML private TableColumn<Category, String> categoryNameColumn;
    @FXML private TextField categoryNameField;

    private final CategoryApi categoryApi = new CategoryApi();

    @FXML
    public void initialize() {
        categoryIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        categoryNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        loadCategories();

        categoryTable.getSelectionModel().selectedItemProperty().addListener((o, oldSel, c) -> {
            if (c != null) categoryNameField.setText(c.getName());
        });
    }

    private void loadCategories() {
        CompletableFuture.runAsync(() -> {
            try {
                List<Category> list = categoryApi.list();
                Platform.runLater(() -> categoryTable.getItems().setAll(list));
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.error("Failed to load categories: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void onAddCategory() {
        String name = categoryNameField.getText().trim();
        if (name.isEmpty()) { AlertUtils.warn("Please enter a category name."); return; }
        CompletableFuture.runAsync(() -> {
            try {
                Category created = categoryApi.create(name);
                Platform.runLater(() -> {
                    categoryTable.getItems().add(created);
                    clearForm();
                    AlertUtils.info("Category added successfully!");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.error("Failed to add category: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void onUpdateCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtils.warn("Please select a category to update."); return; }
        String name = categoryNameField.getText().trim();
        if (name.isEmpty()) { AlertUtils.warn("Category name cannot be empty."); return; }
        CompletableFuture.runAsync(() -> {
            try {
                Category updated = categoryApi.update(selected.getId(), name);
                Platform.runLater(() -> {
                    int idx = categoryTable.getItems().indexOf(selected);
                    if (idx >= 0) categoryTable.getItems().set(idx, updated);
                    clearForm();
                    AlertUtils.info("Category updated successfully!");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.error("Failed to update category: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void onDeleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtils.warn("Please select a category to delete."); return; }
        CompletableFuture.runAsync(() -> {
            try {
                categoryApi.delete(selected.getId());
                Platform.runLater(() -> {
                    categoryTable.getItems().remove(selected);
                    clearForm();
                    AlertUtils.info("Category deleted successfully!");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.error("Failed to delete category: " + ex.getMessage()));
            }
        });
    }

    private void clearForm() {
        categoryNameField.clear();
        categoryTable.getSelectionModel().clearSelection();
    }

    @FXML private void backToMain() { Launcher.go("dashboard.fxml", "Dashboard"); }
    @FXML public void onLogout() {
        TokenStore.clear();
        Launcher.go("login.fxml", "Login");
    }
}
