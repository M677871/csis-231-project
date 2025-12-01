package com.example.demo.graphics;

import com.example.demo.Launcher;
import com.example.demo.auth.AuthApi;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.course.CourseApi;
import com.example.demo.dashboard.DashboardApi;
import com.example.demo.model.CourseDetailDto;
import com.example.demo.model.CourseDto;
import com.example.demo.model.CourseStatsDto;
import com.example.demo.model.InstructorDashboardResponse;
import com.example.demo.model.MeResponse;
import com.example.demo.model.QuizResultDto;
import com.example.demo.model.QuizSummaryDto;
import com.example.demo.model.StudentDashboardResponse;
import com.example.demo.quiz.QuizApi;
import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.SubScene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Playground view that renders instructor enrollments in 3D and student quiz history in 2D.
 */
public class GraphicsPlaygroundController {

    @FXML private AnchorPane threeDContainer;
    @FXML private BarChart<String, Number> progressChart;
    @FXML private CategoryAxis progressXAxis;
    @FXML private NumberAxis progressYAxis;
    @FXML private ComboBox<CourseDto> coursePicker;
    @FXML private javafx.scene.control.Label threeDMetaLabel;
    @FXML private javafx.scene.control.Label twoDMetaLabel;
    @FXML private javafx.scene.control.Label progressNoticeLabel;
    @FXML private javafx.scene.control.Label coursePickerLabel;

    private final DashboardApi dashboardApi = new DashboardApi();
    private final CourseApi courseApi = new CourseApi();
    private final QuizApi quizApi = new QuizApi();
    private final AuthApi authApi = new AuthApi();
    private RotateTransition rotateTransition;
    private MeResponse me;
    private StudentDashboardResponse studentCache;
    private CourseDetailDto selectedCourseDetail;

    @FXML
    public void initialize() {
        progressChart.setLegendVisible(false);
        progressXAxis.setLabel("Quiz");
        progressYAxis.setLabel("Score");
        setupCoursePicker();
        loadProfileAndData();
    }

    @FXML
    private void refresh3d() { load3dData(); }

    @FXML
    private void refresh2d() { loadStudentProgress(); }

    @FXML
    private void onBack() {
        String role = SessionStore.currentRole();
        String dest = "course_catalog.fxml";
        if ("INSTRUCTOR".equals(role)) {
            dest = "instructor_dashboard.fxml";
        } else if ("STUDENT".equals(role)) {
            dest = "student_dashboard.fxml";
        } else if ("ADMIN".equals(role)) {
            dest = "dashboard.fxml";
        }

        Launcher.go(dest, "Dashboard");
    }

    private void loadProfileAndData() {
        CompletableFuture
                .supplyAsync(() -> {
                    MeResponse cached = SessionStore.getMe();
                    if (cached != null) return cached;
                    MeResponse fetched = authApi.me();
                    SessionStore.setMe(fetched);
                    return fetched;
                })
                .thenAccept(m -> Platform.runLater(() -> {
                    me = m;
                    loadCourseChoices();
                    load3dData();
                    loadStudentProgress();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> ErrorDialog.showError("Failed to load profile: " + ex.getMessage()));
                    return null;
                });
    }

    private void load3dData() {
        if (isInstructorOrAdmin() && selectedCourseId() != null) {
            fetch3dForCourse(selectedCourseId());
        } else if (isInstructorOrAdmin()) {
            fetchInstructor3dWithFallback();
        } else {
            fetchStudent3d();
        }
    }

    private void fetchInstructor3dWithFallback() {
        CompletableFuture
                .supplyAsync(() -> dashboardApi.instructorDashboard())
                .thenAccept(resp -> Platform.runLater(() -> render3d(resp, null)))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof ApiException apiEx && (apiEx.getStatusCode() == 401 || apiEx.getStatusCode() == 403)) {
                        // fallback for non-instructor accounts without popping an error
                        fetchStudent3d();
                    } else {
                        Platform.runLater(() -> {
                            ErrorDialog.showError("Failed to load instructor stats: " + cause.getMessage());
                            clear3dWithMessage("Unable to load 3D data.");
                        });
                    }
                    return null;
                });
    }

    private void fetchStudent3d() {
        CompletableFuture
                .supplyAsync(() -> dashboardApi.studentDashboard())
                .thenAccept(resp -> {
                    studentCache = resp;
                    Platform.runLater(() -> render3d(null, resp));
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        if (cause instanceof ApiException apiEx && (apiEx.getStatusCode() == 401 || apiEx.getStatusCode() == 403)) {
                            clear3dWithMessage("3D data is unavailable for this role.");
                        } else if (cause instanceof ApiException apiEx) {
                            ErrorDialog.showError(apiEx.getMessage(), apiEx.getErrorCode());
                            clear3dWithMessage("Unable to load 3D data.");
                        } else {
                            ErrorDialog.showError("Failed to load student data: " + cause.getMessage());
                            clear3dWithMessage("Unable to load 3D data.");
                        }
                    });
                    return null;
                });
    }

    private void render3d(InstructorDashboardResponse instructorResp, StudentDashboardResponse studentResp) {
        List<Box> bars;
        String subtitle;

        if (instructorResp != null && instructorResp.getCourseStats() != null && !instructorResp.getCourseStats().isEmpty()) {
            bars = buildInstructorBars(instructorResp.getCourseStats());
            subtitle = "Instructor enrollments (3D)";
            updateThreeDMeta("Courses: " + instructorResp.getCourseCount() + " • Enrollments: " + instructorResp.getTotalEnrollments());
        } else if (studentResp != null && studentResp.getRecentQuizResults() != null && !studentResp.getRecentQuizResults().isEmpty()) {
            bars = buildStudentBars(studentResp.getRecentQuizResults());
            subtitle = "Recent quiz scores (3D)";
            updateThreeDMeta("Recent quizzes: " + Math.min(10, studentResp.getRecentQuizResults().size()));
        } else {
            clear3dWithMessage("No data to visualize yet. Create courses or take quizzes to see 3D bars.");
            return;
        }

        stopRotation();

        List<CourseStatsDto> stats = instructorResp != null ? instructorResp.getCourseStats().stream()
                .sorted(Comparator.comparing(CourseStatsDto::getEnrollmentCount).reversed())
                .collect(Collectors.toList()) : List.of();
        Group barsGroup = new Group();
        double spacing = 80;
        double startX = -((bars.size() - 1) * spacing) / 2.0;

        for (int i = 0; i < bars.size(); i++) {
            Box bar = bars.get(i);
            bar.setTranslateX(startX + i * spacing);
            barsGroup.getChildren().add(bar);
        }

        javafx.scene.text.Text subtitleText = new javafx.scene.text.Text(subtitle);
        subtitleText.setFill(Color.web("#94a3b8"));
        subtitleText.setTranslateY(-160);
        subtitleText.setTranslateX(-150);
        subtitleText.setScaleX(1.1);
        subtitleText.setScaleY(1.1);

        double planeWidth = Math.max(200, bars.size() * spacing + 60);
        Box ground = new Box(planeWidth, 2, 180);
        ground.setTranslateY(1);
        ground.setMaterial(new PhongMaterial(Color.web("#0b1220")));

        Group root3d = new Group(ground, barsGroup, subtitleText, new AmbientLight(Color.color(0.8, 0.8, 0.8)));

        SubScene subScene = new SubScene(root3d, 900, 520, true, javafx.scene.SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#0a1020"));
        subScene.widthProperty().bind(threeDContainer.widthProperty());
        subScene.heightProperty().bind(threeDContainer.heightProperty());

        javafx.scene.PerspectiveCamera camera = new javafx.scene.PerspectiveCamera(true);
        camera.setTranslateZ(-420);
        camera.setTranslateY(-120);
        camera.setNearClip(0.1);
        camera.setFarClip(2000);
        camera.setRotationAxis(new Point3D(1, 0, 0));
        camera.setRotate(-18);
        subScene.setCamera(camera);

        rotateTransition = new RotateTransition(Duration.seconds(12), barsGroup);
        rotateTransition.setAxis(Rotate.Y_AXIS);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.play();

        threeDContainer.getChildren().setAll(subScene);
        AnchorPane.setTopAnchor(subScene, 0d);
        AnchorPane.setLeftAnchor(subScene, 0d);
        AnchorPane.setRightAnchor(subScene, 0d);
        AnchorPane.setBottomAnchor(subScene, 0d);
    }

    private void loadStudentProgress() {
        if (isInstructorOrAdmin() && selectedCourseId() != null) {
            load2dForCourse(selectedCourseId());
            return;
        }
        CompletableFuture
                .supplyAsync(() -> dashboardApi.studentDashboard())
                .thenAccept(resp -> Platform.runLater(() -> render2d(resp)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        if (cause instanceof ApiException apiEx && (apiEx.getStatusCode() == 401 || apiEx.getStatusCode() == 403)) {
                            progressChart.getData().clear();
                            updateTwoDMeta("Progress data requires a student account.");
                            showProgressNotice("Progress data requires a student account.");
                        } else if (cause instanceof ApiException apiEx) {
                            ErrorDialog.showError(apiEx.getMessage(), apiEx.getErrorCode());
                            progressChart.getData().clear();
                            showProgressNotice("Unable to load progress.");
                        } else {
                            ErrorDialog.showError("Failed to load progress: " + cause.getMessage());
                            progressChart.getData().clear();
                            showProgressNotice("Unable to load progress.");
                        }
                    });
                    return null;
                });
    }

    private void render2d(StudentDashboardResponse resp) {
        progressChart.getData().clear();
        if (resp == null || resp.getRecentQuizResults() == null || resp.getRecentQuizResults().isEmpty()) {
            showProgressNotice("No quiz results yet.");
            return;
        }
        hideProgressNotice();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<QuizResultDto> recent = resp.getRecentQuizResults().stream()
                .sorted(Comparator.comparing(QuizResultDto::getCompletedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(10)
                .collect(Collectors.toList());
        for (int i = 0; i < recent.size(); i++) {
            QuizResultDto qr = recent.get(i);
            String label = "Quiz " + (i + 1);
            series.getData().add(new XYChart.Data<>(label, qr.getScore()));
        }
        progressChart.getData().add(series);
        updateTwoDMeta("Recent quizzes: " + recent.size());
    }

    private void clear3dWithMessage(String message) {
        stopRotation();
        threeDContainer.getChildren().clear();
        javafx.scene.control.Label label = new javafx.scene.control.Label(message);
        label.getStyleClass().add("muted");
        threeDContainer.getChildren().add(label);
        AnchorPane.setTopAnchor(label, 12d);
        AnchorPane.setLeftAnchor(label, 12d);
        updateThreeDMeta(message);
    }

    private boolean isInstructorOrAdmin() {
        String role = SessionStore.currentRole();
        return "INSTRUCTOR".equals(role) || "ADMIN".equals(role);
    }

    private void stopRotation() {
        if (rotateTransition != null) {
            rotateTransition.stop();
        }
    }

    private PhongMaterial materialForIndex(int i) {
        Color[] palette = new Color[] {
                Color.web("#22d3ee"), Color.web("#a855f7"), Color.web("#22c55e"),
                Color.web("#f97316"), Color.web("#e11d48"), Color.web("#fbbf24")
        };
        return new PhongMaterial(palette[i % palette.length]);
    }

    private List<Box> buildInstructorBars(List<CourseStatsDto> stats) {
        if (stats == null) return List.of();
        List<CourseStatsDto> sorted = stats.stream()
                .sorted(Comparator.comparing(CourseStatsDto::getEnrollmentCount).reversed())
                .collect(Collectors.toList());
        long maxEnroll = sorted.stream().mapToLong(CourseStatsDto::getEnrollmentCount).max().orElse(1L);
        double scale = 120.0 / Math.max(1, maxEnroll);
        return sorted.stream().map(s -> {
            double height = Math.max(20, s.getEnrollmentCount() * scale);
            Box bar = new Box(30, height, 30);
            bar.setTranslateY(-height / 2);
            bar.setMaterial(materialForIndex(sorted.indexOf(s)));
            return bar;
        }).collect(Collectors.toList());
    }

    private List<Box> buildStudentBars(List<QuizResultDto> results) {
        List<QuizResultDto> recent = results.stream()
                .sorted(Comparator.comparing(QuizResultDto::getCompletedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(10)
                .collect(Collectors.toList());
        int maxScore = recent.stream().mapToInt(QuizResultDto::getScore).max().orElse(1);
        double scale = 120.0 / Math.max(1, maxScore);
        return recent.stream().map(r -> {
            double height = Math.max(20, r.getScore() * scale);
            Box bar = new Box(30, height, 30);
            bar.setTranslateY(-height / 2);
            bar.setMaterial(materialForIndex(recent.indexOf(r)));
            return bar;
        }).collect(Collectors.toList());
    }

    private void updateThreeDMeta(String text) {
        if (threeDMetaLabel != null) threeDMetaLabel.setText(text);
    }

    private void updateTwoDMeta(String text) {
        if (twoDMetaLabel != null) twoDMetaLabel.setText(text);
    }

    private void showProgressNotice(String text) {
        if (progressNoticeLabel != null) {
            progressNoticeLabel.setText(text);
            progressNoticeLabel.setVisible(true);
            progressNoticeLabel.setManaged(true);
        }
    }

    private void hideProgressNotice() {
        if (progressNoticeLabel != null) {
            progressNoticeLabel.setVisible(false);
            progressNoticeLabel.setManaged(false);
        }
    }

    // -------- Course/quiz scoped helpers for admin/instructor ----------

    private void setupCoursePicker() {
        if (coursePicker == null) return;
        coursePicker.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(CourseDto c) { return c == null ? "" : c.getTitle(); }
            @Override public CourseDto fromString(String s) { return null; }
        });
        coursePicker.setOnAction(e -> {
            selectedCourseDetail = null;
            load3dData();
            loadStudentProgress();
        });
    }

    private void loadCourseChoices() {
        if (coursePicker == null) return;
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        if ("ADMIN".equals(SessionStore.currentRole())) {
                            var page = courseApi.listPublished(0, 200, null, null);
                            return page != null ? page.getContent() : List.<CourseDto>of();
                        } else if ("INSTRUCTOR".equals(SessionStore.currentRole()) && me != null) {
                            CourseDto[] courses = courseApi.listInstructorCourses(me.getId());
                            return courses != null ? List.of(courses) : List.<CourseDto>of();
                        }
                    } catch (Exception ignored) {}
                    return List.<CourseDto>of();
                })
                .thenAccept(list -> Platform.runLater(() -> {
                    coursePicker.getItems().setAll(list);
                    if (!list.isEmpty()) {
                        coursePicker.getSelectionModel().select(0);
                        if (coursePickerLabel != null) coursePickerLabel.setText("Course scope");
                    } else if (coursePickerLabel != null) {
                        coursePickerLabel.setText("No courses available");
                    }
                }))
                .exceptionally(ex -> null);
    }

    private Long selectedCourseId() {
        if (coursePicker == null) return null;
        CourseDto c = coursePicker.getSelectionModel().getSelectedItem();
        return c != null ? c.getId() : null;
    }

    private void fetch3dForCourse(Long courseId) {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        var enrollments = courseApi.listCourseEnrollments(courseId);
                        return enrollments != null ? enrollments.length : 0;
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .thenAccept(count -> Platform.runLater(() -> renderSingleCourse3d(courseId, count)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> clear3dWithMessage("Unable to load course data."));
                    return null;
                });
    }

    private void renderSingleCourse3d(Long courseId, int enrollments) {
        stopRotation();
        double height = Math.max(20, enrollments * 15);
        Box bar = new Box(30, height, 30);
        bar.setTranslateY(-height / 2);
        bar.setMaterial(materialForIndex(0));
        Group barsGroup = new Group(bar);

        double planeWidth = 220;
        Box ground = new Box(planeWidth, 2, 180);
        ground.setTranslateY(1);
        ground.setMaterial(new PhongMaterial(Color.web("#0b1220")));

        Group root3d = new Group(ground, barsGroup, new AmbientLight(Color.color(0.8, 0.8, 0.8)));
        SubScene subScene = new SubScene(root3d, 900, 520, true, javafx.scene.SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#0a1020"));
        subScene.widthProperty().bind(threeDContainer.widthProperty());
        subScene.heightProperty().bind(threeDContainer.heightProperty());

        javafx.scene.PerspectiveCamera camera = new javafx.scene.PerspectiveCamera(true);
        camera.setTranslateZ(-420);
        camera.setTranslateY(-120);
        camera.setNearClip(0.1);
        camera.setFarClip(2000);
        camera.setRotationAxis(new Point3D(1, 0, 0));
        camera.setRotate(-18);
        subScene.setCamera(camera);

        rotateTransition = new RotateTransition(Duration.seconds(12), barsGroup);
        rotateTransition.setAxis(Rotate.Y_AXIS);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.play();

        threeDContainer.getChildren().setAll(subScene);
        AnchorPane.setTopAnchor(subScene, 0d);
        AnchorPane.setLeftAnchor(subScene, 0d);
        AnchorPane.setRightAnchor(subScene, 0d);
        AnchorPane.setBottomAnchor(subScene, 0d);
        updateThreeDMeta("Enrollments: " + enrollments);
    }

    private void load2dForCourse(Long courseId) {
        CompletableFuture
                .supplyAsync(() -> {
                    CourseDetailDto detail = courseApi.get(courseId);
                    selectedCourseDetail = detail;
                    if (detail == null || detail.getQuizzes() == null) return List.<QuizScorePoint>of();
                    var list = new java.util.ArrayList<QuizScorePoint>();
                    for (QuizSummaryDto q : detail.getQuizzes()) {
                        try {
                            QuizResultDto[] results = quizApi.results(q.getId());
                            if (results == null || results.length == 0) continue;
                            AtomicInteger total = new AtomicInteger();
                            AtomicInteger possible = new AtomicInteger();
                            for (QuizResultDto r : results) {
                                total.addAndGet(r.getScore());
                                possible.addAndGet(Math.max(1, r.getTotalQuestions()));
                            }
                            double avg = possible.get() == 0 ? 0 : (double) total.get() / possible.get() * 100.0;
                            list.add(new QuizScorePoint(q.getName(), avg));
                        } catch (Exception ignored) {}
                    }
                    return list;
                })
                .thenAccept(points -> Platform.runLater(() -> renderCourse2d(points)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        progressChart.getData().clear();
                        showProgressNotice("Unable to load course quiz stats.");
                    });
                    return null;
                });
    }

    private void renderCourse2d(List<QuizScorePoint> points) {
        progressChart.getData().clear();
        if (points == null || points.isEmpty()) {
            showProgressNotice("No quiz stats for this course.");
            updateTwoDMeta("Quizzes: 0");
            return;
        }
        hideProgressNotice();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (QuizScorePoint p : points) {
            series.getData().add(new XYChart.Data<>(p.name(), p.averageScore()));
        }
        progressChart.getData().add(series);
        updateTwoDMeta("Quizzes: " + points.size());
    }

    private record QuizScorePoint(String name, double averageScore) {}
}
