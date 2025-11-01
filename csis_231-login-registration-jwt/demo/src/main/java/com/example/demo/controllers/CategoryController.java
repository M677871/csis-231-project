package com.example.demo.controllers;

import com.example.demo.api.CategoryApi;
import com.example.demo.model.Category;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CategoryController {
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Long> idColumn;
    @FXML private TableColumn<Category, String> nameColumn;
    @FXML private TextField nameField;

    private final CategoryApi api = new CategoryApi();
    private final ObservableList<Category> categories = FXCollections.observableArrayList();

    @FXML private void initialize() {
        idColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getId()));
        nameColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        categoryTable.setItems(categories);
        loadCategories();
    }

    private void loadCategories() {
        CompletableFuture.runAsync(() -> {
            try {
                List<Category> list = api.list();
                Platform.runLater(() -> {
                    categories.setAll(list);
                });
            } catch (Exception e) {
                e.printStackTrace();
                // In a real app, display an alert
            }
        });
    }

    @FXML private void handleAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) return;
        CompletableFuture.runAsync(() -> {
            try {
                Category created = api.create(name);
                Platform.runLater(() -> {
                    categories.add(created);
                    nameField.clear();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML private void handleDelete() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                api.delete(selected.getId());
                Platform.runLater(() -> categories.remove(selected));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
