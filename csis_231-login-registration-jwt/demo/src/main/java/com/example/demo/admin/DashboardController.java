package com.example.demo.admin;

import com.example.demo.Launcher;
import com.example.demo.auth.AuthApi;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TokenStore;
import com.example.demo.common.TableUtils;
import com.example.demo.model.EnrollmentResponse;
import com.example.demo.model.MeResponse;
import com.example.demo.student.EnrollmentApi;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.concurrent.CompletableFuture;

/**
 * JavaFX controller for the main dashboard screen.
 *
 * <p>After login, this screen shows basic profile information about the
 * current user (name, e-mail and role) and provides navigation shortcuts
 * to other management screens such as users and categories.</p>
 */

public class DashboardController {
    private final AuthApi authApi = new AuthApi();

    @FXML private Label welcome;
    @FXML private Label name;
    @FXML private Label email;
    @FXML private Label role;
    @FXML private TableView<EnrollmentResponse> myEnrollmentsTable;
    @FXML private TableColumn<EnrollmentResponse, String> myCourseTitleColumn;
    @FXML private TableColumn<EnrollmentResponse, String> myCourseStatusColumn;
    @FXML private TableColumn<EnrollmentResponse, String> myCourseEnrolledAtColumn;

    private final ObservableList<EnrollmentResponse> myEnrollments = FXCollections.observableArrayList();
    private final EnrollmentApi enrollmentApi = new EnrollmentApi();

    /**
     * Initializes the dashboard after the FXML has been loaded.
     *
     * <p>Using {@link Platform#runLater(Runnable)}, this method:</p>
     * <ul>
     *   <li>Checks that a JWT is present in {@link TokenStore}; if not, the
     *       user is redirected back to the login screen</li>
     *   <li>Calls {@link AuthApi#me()} to fetch the current user's profile</li>
     *   <li>Builds a display name from first and last name (or falls back to
     *       the username) and updates the welcome labels accordingly</li>
     * </ul>
     *
     * <p>If any error occurs while loading the profile, an error dialog is
     * shown to the user.</p>
     */

    @FXML
    public void initialize() {
        welcome.setText("Welcome");
        name.setText(""); email.setText(""); role.setText("");

        if (myEnrollmentsTable != null) {
            myCourseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("courseTitle"));
            myCourseStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            myCourseEnrolledAtColumn.setCellValueFactory(new PropertyValueFactory<>("enrolledAt"));
            myEnrollmentsTable.setItems(myEnrollments);
            TableUtils.style(myEnrollmentsTable, myCourseTitleColumn, myCourseStatusColumn, myCourseEnrolledAtColumn);
        }

        Platform.runLater(() -> {
            try {
                if (!TokenStore.hasToken()) {
                    AlertUtils.warn("No token present. Please login again.");
                    Launcher.go("login.fxml", "Login");
                    return;
                }
                MeResponse me = authApi.me(); // your tolerant/profile call
                SessionStore.setMe(me);
                String full = (me.getFirstName() == null ? "" : me.getFirstName());
                if (me.getLastName() != null && !me.getLastName().isBlank()) full += (full.isBlank() ? "" : " ") + me.getLastName();
                if (full.isBlank()) full = me.getUsername();

                welcome.setText(full.isBlank() ? "Welcome" : ("Welcome, " + full));
                name.setText(full);
                email.setText(me.getEmail());
                role.setText(me.getRole());
                loadMyEnrollments(me.getId());
            } catch (ApiException ex) {
                ErrorDialog.showError(ex.getMessage(), ex.getErrorCode());
            } catch (Exception ex) {
                ErrorDialog.showError("Failed to load profile: " + ex.getMessage());
            }
        });
    }

    private void loadMyEnrollments(Long userId) {
        if (userId == null || myEnrollmentsTable == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                var resp = enrollmentApi.listByCurrentUser(userId);
                Platform.runLater(() -> myEnrollments.setAll(resp != null ? java.util.Arrays.asList(resp) : java.util.List.of()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load your enrollments: " + ex.getMessage()));
            }
        });
    }

    /**
     * Opens the user management screen.
     */

    @FXML private void openUsers()      { Launcher.go("user_Dashboard.fxml", "Users"); }

/**
 * Opens the category management screen.
 */

    @FXML private void openCategories() { Launcher.go("category.fxml", "Categories"); }
    @FXML private void openCourses()    { Launcher.go("course_catalog.fxml", "Courses"); }
    @FXML private void openEnrollments(){ Launcher.go("admin_enrollments.fxml", "Enrollments"); }

    /**
     * Logs the user out by clearing the stored JWT and returning to the login screen.
     */

    @FXML
    public void onLogout() {
        TokenStore.clear();
        SessionStore.clearAll();
        Launcher.go("login.fxml", "Login");
    }
}
