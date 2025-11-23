package com.example.demo.instructor;

import com.example.demo.Launcher;
import com.example.demo.auth.AuthApi;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TokenStore;
import com.example.demo.common.TableUtils;
import com.example.demo.course.CourseApi;
import com.example.demo.model.EnrollmentResponse;
import com.example.demo.model.CourseDto;
import com.example.demo.model.InstructorDashboardResponse;
import com.example.demo.model.MeResponse;
import com.example.demo.model.CourseStatsDto;
import com.example.demo.dashboard.DashboardApi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Instructor dashboard showing courses and stats.
 */
public class InstructorDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label courseCountLabel;
    @FXML private Label enrollmentCountLabel;
    @FXML private Label lastCourseLabel;

    @FXML private TableView<CourseDto> courseTable;
    @FXML private TableColumn<CourseDto, String> titleColumn;
    @FXML private TableColumn<CourseDto, Boolean> publishedColumn;
    @FXML private TableColumn<CourseDto, String> createdColumn;
    @FXML private TableColumn<CourseDto, Void> actionsColumn;

    @FXML private TableView<EnrollmentResponse> enrollmentTable;
    @FXML private TableColumn<EnrollmentResponse, String> enrollStudentColumn;
    @FXML private TableColumn<EnrollmentResponse, String> enrollEmailColumn;
    @FXML private TableColumn<EnrollmentResponse, String> enrollStatusColumn;
    @FXML private TableColumn<EnrollmentResponse, String> enrollDateColumn;

    private final ObservableList<CourseDto> courses = FXCollections.observableArrayList();
    private final ObservableList<EnrollmentResponse> enrollments = FXCollections.observableArrayList();
    private final CourseApi courseApi = new CourseApi();
    private final DashboardApi dashboardApi = new DashboardApi();
    private final AuthApi authApi = new AuthApi();
    private MeResponse me;

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        publishedColumn.setCellValueFactory(new PropertyValueFactory<>("published"));
        publishedColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : (Boolean.TRUE.equals(v) ? "Published" : "Draft"));
            }
        });
        createdColumn.setCellValueFactory(c -> {
            if (c.getValue().getCreatedAt() == null) return null;
            String formatted = DateTimeFormatter.ISO_INSTANT.format(c.getValue().getCreatedAt());
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button openBtn = new Button("Open");
            private final Button editBtn = new Button("Edit");
            {
                openBtn.getStyleClass().add("ghost-button");
                editBtn.getStyleClass().add("primary-button");
                openBtn.setOnAction(e -> onOpenCourse(getTableView().getItems().get(getIndex())));
                editBtn.setOnAction(e -> onEditCourse(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                var box = new javafx.scene.layout.HBox(8, openBtn, editBtn);
                setGraphic(box);
            }
        });
        courseTable.setItems(courses);
        TableUtils.style(courseTable, titleColumn, publishedColumn, createdColumn, actionsColumn);

        enrollStudentColumn.setCellValueFactory(new PropertyValueFactory<>("studentUsername"));
        enrollEmailColumn.setCellValueFactory(new PropertyValueFactory<>("studentEmail"));
        enrollStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        enrollDateColumn.setCellValueFactory(new PropertyValueFactory<>("enrolledAt"));
        enrollmentTable.setItems(enrollments);
        TableUtils.style(enrollmentTable, enrollStudentColumn, enrollEmailColumn, enrollStatusColumn, enrollDateColumn);

        loadMeAndDashboard();
    }

    private void loadMeAndDashboard() {
        CompletableFuture.runAsync(() -> {
            try {
                MeResponse cached = SessionStore.getMe();
                me = cached != null ? cached : authApi.me();
                if (cached == null) SessionStore.setMe(me);
                loadDashboard();
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load profile: " + ex.getMessage()));
            }
        });
    }

    private void loadDashboard() {
        CompletableFuture.runAsync(() -> {
            try {
                InstructorDashboardResponse resp = dashboardApi.instructorDashboard();
                Platform.runLater(() -> populate(resp));
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load dashboard: " + ex.getMessage()));
            }
        });
    }

    private void populate(InstructorDashboardResponse resp) {
        if (me != null) {
            welcomeLabel.setText("Welcome, " + (me.getFirstName() != null ? me.getFirstName() : me.getUsername()));
        }
        courseCountLabel.setText(String.valueOf(resp.getCourseCount()));
        enrollmentCountLabel.setText(String.valueOf(resp.getTotalEnrollments()));
        if (resp.getCourses() != null && !resp.getCourses().isEmpty()) {
            CourseDto last = resp.getCourses().get(resp.getCourses().size() - 1);
            lastCourseLabel.setText(last.getTitle());
        } else {
            lastCourseLabel.setText("No courses yet");
        }
        courses.setAll(resp.getCourses() != null ? resp.getCourses() : java.util.List.of());
        enrollments.clear();
    }

    private void onOpenCourse(CourseDto course) {
        SessionStore.setActiveCourse(course);
        Launcher.go("course_detail.fxml", "Course Detail");
    }

    private void onEditCourse(CourseDto course) {
        SessionStore.setActiveCourse(course);
        Launcher.go("course_editor.fxml", "Course Editor");
    }

    @FXML
    private void onViewEnrollments() {
        CourseDto selected = courseTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtils.warn("Select a course first."); return; }
        loadEnrollmentsForCourse(selected.getId());
    }

    private void loadEnrollmentsForCourse(Long courseId) {
        CompletableFuture.runAsync(() -> {
            try {
                EnrollmentResponse[] resp = courseApi.listCourseEnrollments(courseId);
                Platform.runLater(() -> enrollments.setAll(resp != null ? java.util.Arrays.asList(resp) : java.util.List.of()));
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load enrollments: " + ex.getMessage()));
            }
        });
    }

    @FXML
    private void onCreateCourse() {
        SessionStore.setActiveCourse(null);
        Launcher.go("course_editor.fxml", "New Course");
    }

    @FXML
    private void onOpenCatalog() { Launcher.go("course_catalog.fxml", "Course Catalog"); }

    @FXML
    private void onLogout() {
        TokenStore.clear();
        SessionStore.clearAll();
        Launcher.go("login.fxml", "Login");
    }
}
