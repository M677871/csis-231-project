package com.example.demo.admin;

import com.example.demo.Launcher;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TokenStore;
import com.example.demo.model.EnrollmentResponse;
import com.example.demo.student.EnrollmentApi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Admin view to inspect a student's enrollments.
 */
public class AdminEnrollmentsController {
    @FXML private TextField studentIdField;
    @FXML private Button loadButton;
    @FXML private Label statusLabel;

    @FXML private TableView<EnrollmentResponse> enrollmentTable;
    @FXML private TableColumn<EnrollmentResponse, Long> courseIdColumn;
    @FXML private TableColumn<EnrollmentResponse, String> courseTitleColumn;
    @FXML private TableColumn<EnrollmentResponse, String> statusColumn;
    @FXML private TableColumn<EnrollmentResponse, String> enrolledAtColumn;

    private final EnrollmentApi enrollmentApi = new EnrollmentApi();
    private final ObservableList<EnrollmentResponse> enrollments = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        courseIdColumn.setCellValueFactory(new PropertyValueFactory<>("courseId"));
        courseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("courseTitle"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        enrolledAtColumn.setCellValueFactory(new PropertyValueFactory<>("enrolledAt"));
        enrollmentTable.setItems(enrollments);
    }

    @FXML
    private void onLoad() {
        String idText = studentIdField.getText() == null ? "" : studentIdField.getText().trim();
        if (idText.isEmpty()) {
            AlertUtils.warn("Enter a student ID.");
            return;
        }
        Long studentId;
        try {
            studentId = Long.parseLong(idText);
        } catch (NumberFormatException e) {
            AlertUtils.warn("Student ID must be numeric.");
            return;
        }

        loadButton.setDisable(true);
        statusLabel.setText("Loading...");
        CompletableFuture.runAsync(() -> {
            try {
                EnrollmentResponse[] resp = enrollmentApi.listByStudent(studentId);
                Platform.runLater(() -> {
                    enrollments.setAll(resp != null ? Arrays.asList(resp) : java.util.List.of());
                    statusLabel.setText(enrollments.size() + " enrollments");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load enrollments: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> loadButton.setDisable(false));
            }
        });
    }

    @FXML
    private void backToDashboard() { Launcher.go("dashboard.fxml", "Dashboard"); }

    @FXML
    private void onLogout() {
        TokenStore.clear();
        SessionStore.clearAll();
        Launcher.go("login.fxml", "Login");
    }
}
