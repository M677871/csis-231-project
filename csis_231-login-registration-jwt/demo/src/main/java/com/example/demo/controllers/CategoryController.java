package com.example.demo.controllers;

import com.example.demo.Launcher;
import com.example.demo.api.CategoryApi;
import com.example.demo.model.Category;
import com.example.demo.security.TokenStore;
import com.example.demo.util.AlertUtils;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CategoryController {

    // Table
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Long>   categoryIdColumn;
    @FXML private TableColumn<Category, String> categoryNameColumn;

    // Form
    @FXML private TextField categoryNameField;

    // Dynamic (from FXML toolbars)
    @FXML private TextField categorySearchField;
    @FXML private Label categoryCountLabel;

    private final CategoryApi categoryApi = new CategoryApi();

    // Data pipeline
    private final ObservableList<Category> master = FXCollections.observableArrayList();
    private final FilteredList<Category> filtered = new FilteredList<>(master, c -> true);
    private final SortedList<Category>   sorted   = new SortedList<>(filtered);

    @FXML
    public void initialize() {
        // Columns
        categoryIdColumn.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        categoryNameColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));

        // Sorting + show all rows
        sorted.comparatorProperty().bind(categoryTable.comparatorProperty());
        categoryTable.setItems(sorted);

        // Selection -> form
        categoryTable.getSelectionModel().selectedItemProperty().addListener((o, oldSel, c) -> {
            if (c != null) categoryNameField.setText(c.getName());
        });

        // Live search
        categorySearchField.textProperty().addListener((obs, o, n) -> {
            final String q = n == null ? "" : n.toLowerCase().trim();
            filtered.setPredicate(c -> {
                if (c == null) return false;
                boolean byName = c.getName() != null && c.getName().toLowerCase().contains(q);
                boolean byId   = c.getId() != null && String.valueOf(c.getId()).contains(q);
                return q.isEmpty() || byName || byId;
            });
            updateCount();
        });

        // Not ambiguous: explicitly ListChangeListener
        filtered.addListener((ListChangeListener<Category>) change -> updateCount());

        // Initial data
        loadCategories();
    }

    private void loadCategories() {
        CompletableFuture.runAsync(() -> {
            try {
                List<Category> list = categoryApi.list();
                Platform.runLater(() -> {
                    master.setAll(list);
                    updateCount();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.error("Failed to load categories: " + ex.getMessage()));
            }
        });
    }

    private void updateCount() {
        categoryCountLabel.setText(filtered.size() + " items");
    }

    @FXML
    private void onAddCategory() {
        String name = categoryNameField.getText().trim();
        if (name.isEmpty()) { AlertUtils.warn("Please enter a category name."); return; }
        CompletableFuture.runAsync(() -> {
            try {
                Category created = categoryApi.create(name);
                Platform.runLater(() -> {
                    master.add(created);
                    clearForm();
                    updateCount();
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
                    for (int i = 0; i < master.size(); i++) {
                        if (master.get(i).getId().equals(selected.getId())) {
                            master.set(i, updated);
                            break;
                        }
                    }
                    clearForm();
                    updateCount();
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
                    master.removeIf(c -> c.getId().equals(selected.getId()));
                    clearForm();
                    updateCount();
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
