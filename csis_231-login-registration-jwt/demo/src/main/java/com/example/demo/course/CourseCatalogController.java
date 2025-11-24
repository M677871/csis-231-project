package com.example.demo.course;

import com.example.demo.Launcher;
import com.example.demo.auth.AuthApi;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TableUtils;
import com.example.demo.model.CourseDto;
import com.example.demo.model.EnrollmentRequest;
import com.example.demo.model.EnrollmentResponse;
import com.example.demo.model.MeResponse;
import com.example.demo.student.EnrollmentApi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Course catalog screen shared by students and instructors.
 */
public class CourseCatalogController {
    @FXML private TableView<CourseDto> courseTable;
    @FXML private TableColumn<CourseDto, String> titleColumn;
    @FXML private TableColumn<CourseDto, String> instructorColumn;
    @FXML private TableColumn<CourseDto, Boolean> publishedColumn;
    @FXML private TableColumn<CourseDto, Void> actionColumn;
    @FXML private TextField searchField;
    @FXML private Button refreshButton;
    @FXML private Label statusLabel;

    private final CourseApi courseApi = new CourseApi();
    private final EnrollmentApi enrollmentApi = new EnrollmentApi();
    private final AuthApi authApi = new AuthApi();
    private final ObservableList<CourseDto> courses = FXCollections.observableArrayList();
    private MeResponse me;
    private final java.util.Set<Long> enrolledCourseIds = new java.util.HashSet<>();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        instructorColumn.setCellValueFactory(new PropertyValueFactory<>("instructorName"));
        publishedColumn.setCellValueFactory(new PropertyValueFactory<>("published"));
        publishedColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : (Boolean.TRUE.equals(v) ? "Published" : "Draft"));
            }
        });

        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button primary = new Button();
            {
                primary.getStyleClass().add("primary-button");
                primary.setOnAction(e -> {
                    CourseDto course = getTableView().getItems().get(getIndex());
                    onAction(course);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                CourseDto course = getTableView().getItems().get(getIndex());
                boolean enrolled = course.getId() != null && enrolledCourseIds.contains(course.getId());
                primary.setDisable(enrolled);
                primary.setText(enrolled ? "Enrolled" : (isOwner(course) ? "Edit" : "Enroll"));
                setGraphic(primary);
            }
        });

        courseTable.setItems(courses);
        TableUtils.style(courseTable, titleColumn, instructorColumn, publishedColumn, actionColumn);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> loadCourses());
        }
        loadMeAndCourses();
    }

    private void loadMeAndCourses() {
        CompletableFuture.runAsync(() -> {
            try {
                MeResponse cached = SessionStore.getMe();
                me = cached != null ? cached : authApi.me();
                if (cached == null) SessionStore.setMe(me);
                loadEnrollments();
                loadCourses();
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load profile: " + ex.getMessage()));
            }
        });
    }

    private void loadEnrollments() {
        if (me == null || me.getId() == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                EnrollmentResponse[] resp = enrollmentApi.listByCurrentUser(me.getId());
                enrolledCourseIds.clear();
                if (resp != null) {
                    for (EnrollmentResponse e : resp) {
                        if (e.getCourseId() != null) enrolledCourseIds.add(e.getCourseId());
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private void loadCourses() {
        String search = searchField != null ? searchField.getText() : null;
        CompletableFuture.runAsync(() -> {
            try {
                var page = courseApi.listPublished(0, 50, null, search);
                var items = page != null && page.getContent() != null
                        ? page.getContent()
                        : java.util.List.<CourseDto>of();
                Platform.runLater(() -> {
                    courses.setAll(items);
                    statusLabel.setText(items.size() + " courses");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load courses: " + ex.getMessage()));
            }
        });
    }

    private void onAction(CourseDto course) {
        if (course == null) return;
        boolean enrolled = course.getId() != null && enrolledCourseIds.contains(course.getId());
        if (enrolled) return;

        if (isStudent() || isInstructor() || isAdmin()) {
            // instructors/admins enroll unless they own it (then edit)
            if (isOwner(course) && !isStudent()) {
                SessionStore.setActiveCourse(course);
                Launcher.go("course_editor.fxml", "Edit Course");
            } else {
                doEnroll(course);
            }
        } else {
            doEnroll(course);
        }
    }

    private void doEnroll(CourseDto course) {
        if (course == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                enrollmentApi.enroll(new EnrollmentRequest(null, course.getId()));
                Platform.runLater(() -> AlertUtils.info("Enrolled in " + course.getTitle()));
                if (course.getId() != null) enrolledCourseIds.add(course.getId());
                courseTable.refresh();
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to enroll: " + ex.getMessage()));
            }
        });
    }

    private boolean isStudent() { return me != null && "STUDENT".equalsIgnoreCase(me.getRole()); }
    private boolean isInstructor() { return me != null && "INSTRUCTOR".equalsIgnoreCase(me.getRole()); }
    private boolean isAdmin() { return me != null && "ADMIN".equalsIgnoreCase(me.getRole()); }
    private boolean isOwner(CourseDto c) {
        return c != null && me != null && me.getId() != null && c.getInstructorUserId() != null
                && c.getInstructorUserId().equals(me.getId());
    }

    @FXML
    private void onOpenCourseDetail() {
        CourseDto selected = courseTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtils.warn("Select a course first."); return; }
        SessionStore.setActiveCourse(selected);
        Launcher.go("course_detail.fxml", "Course Detail");
    }

    @FXML
    private void onRefresh() { loadCourses(); }

    @FXML
    private void onBack() {
        String role = me != null ? me.getRole() : null;
        if ("INSTRUCTOR".equalsIgnoreCase(role)) {
            Launcher.go("instructor_dashboard.fxml", "Instructor Dashboard");
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            Launcher.go("dashboard.fxml", "Admin Dashboard");
        } else if ("STUDENT".equalsIgnoreCase(role)) {
            Launcher.go("student_dashboard.fxml", "Student Dashboard");
        } else {
            Launcher.go("dashboard.fxml", "Dashboard");
        }
    }
}
