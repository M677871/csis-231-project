package com.example.demo;

import com.example.demo.security.IdleGuard;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;
import java.time.Duration;
import java.util.Objects;

public final class Launcher {

    private static Stage stage;
    private static Scene scene; // reuse one scene
    private static final String FXML_PREFIX = "/com/example/demo/fxml/";
    private static final String STYLES_PATH = "/com/example/demo/styles.css";
    private static final String ICON_PATH   = "/com/example/demo/icon.png"; // optional

    private static volatile boolean idleAttached; // ensure guard attaches only once

    private Launcher() {}

    public static void init(Stage primary) {
        stage = Objects.requireNonNull(primary, "primary stage is null");

        // Try to set an app icon if present (non-fatal if missing)
        URL iconUrl = HelloApplication.class.getResource(ICON_PATH);
        if (iconUrl != null) {
            try { stage.getIcons().add(new Image(iconUrl.toExternalForm())); } catch (Exception ignored) {}
        }
    }

    public static void go(String fxmlFileName, String title) {
        Runnable task = () -> {
            try {
                ensureInitialized();

                URL url = HelloApplication.class.getResource(FXML_PREFIX + fxmlFileName);
                if (url == null) {
                    throw new IllegalStateException("FXML not found: " + FXML_PREFIX + fxmlFileName);
                }

                FXMLLoader loader = new FXMLLoader(url);
                Parent root = loader.load();

                if (scene == null) {
                    scene = new Scene(root);
                    applyGlobalStyles(scene);
                    stage.setScene(scene);
                } else {
                    scene.setRoot(root);
                }

                // Attach IdleGuard once to the reusable scene
                attachIdleGuardOnce(stage.getScene());

                if (title != null && !title.isBlank()) {
                    stage.setTitle(title);
                }

                if (!stage.isShowing()) stage.show();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load " + fxmlFileName + ": " + ex.getMessage(), ex);
            }
        };

        if (Platform.isFxApplicationThread()) task.run(); else Platform.runLater(task);
    }

    private static void ensureInitialized() {
        if (stage == null) {
            throw new IllegalStateException("Launcher.init(stage) must be called before Launcher.go(...)");
        }
    }

    private static void applyGlobalStyles(Scene s) {
        URL css = HelloApplication.class.getResource(STYLES_PATH);
        if (css != null) {
            String uri = css.toExternalForm();
            if (!s.getStylesheets().contains(uri)) {
                s.getStylesheets().add(uri);
            }
        }
    }

    private static synchronized void attachIdleGuardOnce(Scene s) {
        if (!idleAttached && s != null) {
            IdleGuard.attach(s, Duration.ofMinutes(15));
            idleAttached = true;
        }
    }
}
