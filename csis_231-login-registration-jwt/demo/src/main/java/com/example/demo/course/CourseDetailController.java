package com.example.demo.course;

import com.example.demo.Launcher;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TableUtils;
import com.example.demo.model.CourseDetailDto;
import com.example.demo.model.CourseDto;
import com.example.demo.model.CourseMaterialDto;
import com.example.demo.model.QuizSummaryDto;
import com.example.demo.quiz.QuizApi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;


import java.awt.*;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Course detail view for students and instructors.
 */
public class CourseDetailController {
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label instructorLabel;
    @FXML private Label publishedLabel;

    @FXML private TableView<CourseMaterialDto> materialTable;
    @FXML private TableColumn<CourseMaterialDto, String> materialTitleColumn;
    @FXML private TableColumn<CourseMaterialDto, String> materialTypeColumn;
    @FXML private TableColumn<CourseMaterialDto, String> materialUrlColumn;

    @FXML private TableView<QuizSummaryDto> quizTable;
    @FXML private TableColumn<QuizSummaryDto, String> quizNameColumn;
    @FXML private TableColumn<QuizSummaryDto, Integer> quizQuestionsColumn;
    @FXML private TableColumn<QuizSummaryDto, Void> quizActionColumn;

    private final CourseApi courseApi = new CourseApi();
    private final QuizApi quizApi = new QuizApi();
    private final ObservableList<CourseMaterialDto> materials = FXCollections.observableArrayList();
    private final ObservableList<QuizSummaryDto> quizzes = FXCollections.observableArrayList();
    private CourseDto course;

    @FXML
    public void initialize() {
        materialTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        materialTypeColumn.setCellValueFactory(new PropertyValueFactory<>("materialType"));
        materialUrlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        materialTable.setItems(materials);
        materialTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) openSelectedMaterial();
        });

        quizNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        quizQuestionsColumn.setCellValueFactory(new PropertyValueFactory<>("questionCount"));
        quizActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button takeBtn = new Button("Take");
            {
                takeBtn.getStyleClass().add("primary-button");
                takeBtn.setOnAction(e -> {
                    QuizSummaryDto q = getTableView().getItems().get(getIndex());
                    onTakeQuiz(q);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : takeBtn);
            }
        });
        quizTable.setItems(quizzes);
        TableUtils.style(materialTable, materialTitleColumn, materialTypeColumn, materialUrlColumn);
        TableUtils.style(quizTable, quizNameColumn, quizQuestionsColumn, quizActionColumn);

        course = SessionStore.getActiveCourse();
        if (course == null) {
            AlertUtils.warn("No course selected.");
            return;
        }
        loadDetail(course.getId());
    }

    private void loadDetail(Long courseId) {
        CompletableFuture.runAsync(() -> {
            try {
                CourseDetailDto detail = courseApi.get(courseId);
                Platform.runLater(() -> populate(detail));
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load course: " + ex.getMessage()));
            }
        });
    }

    private void populate(CourseDetailDto d) {
        titleLabel.setText(d.getTitle());
        descriptionLabel.setText(d.getDescription());
        instructorLabel.setText(d.getInstructorName());
        publishedLabel.setText(Boolean.TRUE.equals(d.getPublished()) ? "Published" : "Draft");
        materials.setAll(d.getMaterials() != null ? d.getMaterials() : java.util.List.of());
        quizzes.setAll(d.getQuizzes() != null ? d.getQuizzes() : java.util.List.of());
    }

    private void openSelectedMaterial() {
        CourseMaterialDto selected = materialTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getUrl() == null || selected.getUrl().isBlank()) return;
        if (!Desktop.isDesktopSupported()) {
            AlertUtils.warn("Opening materials is not supported on this system.");
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(selected.getUrl()));
        } catch (Exception ex) {
            ErrorDialog.showError("Could not open material: " + ex.getMessage());
        }
    }

    private void onTakeQuiz(QuizSummaryDto quiz) {
        if (quiz == null) { return; }
        SessionStore.setActiveQuiz(quiz);
        Launcher.go("quiz_taker.fxml", "Take Quiz");
    }

    @FXML
    private void onBack() {
        var me = SessionStore.getMe();
        if (me != null && "INSTRUCTOR".equalsIgnoreCase(me.getRole())) {
            Launcher.go("instructor_dashboard.fxml", "Instructor Dashboard");
        } else if (me != null && "ADMIN".equalsIgnoreCase(me.getRole())) {
            Launcher.go("dashboard.fxml", "Admin Dashboard");
        } else {
            Launcher.go("student_dashboard.fxml", "Student Dashboard");
        }
    }
}
