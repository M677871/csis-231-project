package com.example.demo.admin;

import com.example.demo.Launcher;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TokenStore;
import com.example.demo.model.CourseDto;
import com.example.demo.model.EnrollmentResponse;
import com.example.demo.student.EnrollmentApi;
import com.example.demo.admin.UserApi;
import com.example.demo.course.CourseApi;
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
    @FXML private TextField studentIdentifierField;
    @FXML private Button loadStudentButton;
    @FXML private Label studentStatusLabel;

    @FXML private TextField instructorIdentifierField;
    @FXML private Button loadInstructorButton;
    @FXML private Label instructorStatusLabel;

    @FXML private TableView<EnrollmentResponse> enrollmentTable;
    @FXML private TableColumn<EnrollmentResponse, Long> courseIdColumn;
    @FXML private TableColumn<EnrollmentResponse, String> courseTitleColumn;
    @FXML private TableColumn<EnrollmentResponse, String> statusColumn;
    @FXML private TableColumn<EnrollmentResponse, String> enrolledAtColumn;

    @FXML private TableView<CourseDto> instructorCourseTable;
    @FXML private TableColumn<CourseDto, Long> instructorCourseIdColumn;
    @FXML private TableColumn<CourseDto, String> instructorCourseTitleColumn;
    @FXML private TableColumn<CourseDto, Boolean> instructorCoursePublishedColumn;

    private final EnrollmentApi enrollmentApi = new EnrollmentApi();
    private final UserApi userApi = new UserApi();
    private final CourseApi courseApi = new CourseApi();
    private final ObservableList<EnrollmentResponse> enrollments = FXCollections.observableArrayList();
    private final ObservableList<CourseDto> instructorCourses = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        courseIdColumn.setCellValueFactory(new PropertyValueFactory<>("courseId"));
        courseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("courseTitle"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        enrolledAtColumn.setCellValueFactory(new PropertyValueFactory<>("enrolledAt"));
        enrollmentTable.setItems(enrollments);

        instructorCourseIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        instructorCourseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        instructorCoursePublishedColumn.setCellValueFactory(new PropertyValueFactory<>("published"));
        instructorCourseTable.setItems(instructorCourses);
    }

    @FXML
    private void onLoadStudent() {
        String identifier = studentIdentifierField.getText() == null ? "" : studentIdentifierField.getText().trim();
        if (identifier.isEmpty()) { AlertUtils.warn("Enter a username or email."); return; }

        loadStudentButton.setDisable(true);
        studentStatusLabel.setText("Loading...");
        CompletableFuture.runAsync(() -> {
            try {
                Long studentId = userApi.findByIdentifier(identifier)
                        .map(com.example.demo.model.User::getId)
                        .orElse(null);
                if (studentId == null) {
                    Platform.runLater(() -> {
                        studentStatusLabel.setText("User not found");
                        enrollments.clear();
                    });
                    return;
                }
                EnrollmentResponse[] resp = enrollmentApi.listByStudent(studentId);
                Platform.runLater(() -> {
                    enrollments.setAll(resp != null ? Arrays.asList(resp) : java.util.List.of());
                    studentStatusLabel.setText(enrollments.size() + " enrollments");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load enrollments: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> loadStudentButton.setDisable(false));
            }
        });
    }

    @FXML
    private void onLoadInstructor() {
        String identifier = instructorIdentifierField.getText() == null ? "" : instructorIdentifierField.getText().trim();
        if (identifier.isEmpty()) { AlertUtils.warn("Enter a username or email."); return; }

        loadInstructorButton.setDisable(true);
        instructorStatusLabel.setText("Loading...");
        CompletableFuture.runAsync(() -> {
            try {
                Long instructorId = userApi.findByIdentifier(identifier)
                        .map(com.example.demo.model.User::getId)
                        .orElse(null);
                if (instructorId == null) {
                    Platform.runLater(() -> {
                        instructorStatusLabel.setText("User not found");
                        instructorCourses.clear();
                    });
                    return;
                }
                CourseDto[] resp = courseApi.listInstructorCourses(instructorId);
                Platform.runLater(() -> {

                    instructorCourses.setAll(resp != null ? Arrays.asList(resp) : java.util.List.of());
                    instructorStatusLabel.setText(instructorCourses.size() + " courses");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load courses: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> loadInstructorButton.setDisable(false));
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
