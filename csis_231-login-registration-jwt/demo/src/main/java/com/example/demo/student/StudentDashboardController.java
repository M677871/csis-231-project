package com.example.demo.student;

import com.example.demo.Launcher;
import com.example.demo.auth.AuthApi;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TokenStore;
import com.example.demo.dashboard.DashboardApi;
import com.example.demo.model.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Student dashboard showing enrollments, progress and quizzes.
 */
public class StudentDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label enrolledCountLabel;
    @FXML private Label upcomingCountLabel;
    @FXML private Label lastScoreLabel;

    @FXML private TableView<CourseDto> enrolledTable;
    @FXML private TableColumn<CourseDto, String> courseTitleColumn;
    @FXML private TableColumn<CourseDto, Void> courseActionColumn;

    @FXML private TableView<QuizSummaryDto> quizTable;
    @FXML private TableColumn<QuizSummaryDto, String> quizNameColumn;
    @FXML private TableColumn<QuizSummaryDto, Integer> quizQuestionColumn;
    @FXML private TableColumn<QuizSummaryDto, Void> quizActionColumn;

    private final ObservableList<CourseDto> enrolledCourses = FXCollections.observableArrayList();
    private final ObservableList<QuizSummaryDto> upcomingQuizzes = FXCollections.observableArrayList();
    private final DashboardApi dashboardApi = new DashboardApi();
    private final AuthApi authApi = new AuthApi();
    private MeResponse me;

    @FXML
    public void initialize() {
        courseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        courseActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button openBtn = new Button("Open");
            {
                openBtn.getStyleClass().add("primary-button");
                openBtn.setOnAction(e -> {
                    CourseDto c = getTableView().getItems().get(getIndex());
                    onOpenCourse(c);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : openBtn);
            }
        });
        enrolledTable.setItems(enrolledCourses);

        quizNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        quizQuestionColumn.setCellValueFactory(new PropertyValueFactory<>("questionCount"));
        quizActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button takeBtn = new Button("Take Quiz");
            {
                takeBtn.getStyleClass().add("secondary-button");
                takeBtn.setOnAction(e -> onTakeQuiz(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : takeBtn);
            }
        });
        quizTable.setItems(upcomingQuizzes);

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
                StudentDashboardResponse resp = dashboardApi.studentDashboard();
                Platform.runLater(() -> populate(resp));
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load dashboard: " + ex.getMessage()));
            }
        });
    }

    private void populate(StudentDashboardResponse resp) {
        if (me != null) {
            welcomeLabel.setText("Welcome, " + (me.getFirstName() != null ? me.getFirstName() : me.getUsername()));
        }
        enrolledCourses.setAll(resp.getEnrolledCourses() != null ? resp.getEnrolledCourses() : java.util.List.of());
        upcomingQuizzes.setAll(resp.getUpcomingQuizzes() != null ? resp.getUpcomingQuizzes() : java.util.List.of());
        enrolledCountLabel.setText(String.valueOf(resp.getEnrolledCourseCount()));
        upcomingCountLabel.setText(String.valueOf(upcomingQuizzes.size()));
        if (resp.getRecentQuizResults() != null && !resp.getRecentQuizResults().isEmpty()) {
            QuizResultDto last = resp.getRecentQuizResults().get(0);
            lastScoreLabel.setText(last.getScore() + "/" + last.getTotalQuestions());
        } else {
            lastScoreLabel.setText("—");
        }
    }

    private void onOpenCourse(CourseDto course) {
        SessionStore.setActiveCourse(course);
        Launcher.go("course_detail.fxml", "Course Detail");
    }

    private void onTakeQuiz(QuizSummaryDto quiz) {
        SessionStore.setActiveQuiz(quiz);
        Launcher.go("quiz_taker.fxml", "Take Quiz");
    }

    @FXML
    private void onOpenCatalog() { Launcher.go("course_catalog.fxml", "Course Catalog"); }

    @FXML
    public void onLogout() {
        TokenStore.clear();
        SessionStore.clearAll();
        Launcher.go("login.fxml", "Login");
    }
}
