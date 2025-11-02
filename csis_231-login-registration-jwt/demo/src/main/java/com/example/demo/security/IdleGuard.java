package com.example.demo.security;

import com.example.demo.Launcher;
import com.example.demo.util.AlertUtils;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.*;
import java.time.Duration;
import java.util.concurrent.*;
import com.example.demo.security.TokenStore;


/** Forces logout after NO user interaction for a given timeout. */
public final class IdleGuard {
    private IdleGuard() {}

    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "IdleGuard");
        t.setDaemon(true);
        return t;
    });

    private static volatile ScheduledFuture<?> task;
    private static volatile long timeoutMillis;

    public static void attach(Scene scene, Duration idleTimeout) {
        if (scene == null || idleTimeout == null) return;
        timeoutMillis = idleTimeout.toMillis();

        Runnable reset = IdleGuard::resetTimer;

        scene.addEventFilter(MouseEvent.MOUSE_MOVED,   e -> reset.run());
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> reset.run());
        scene.addEventFilter(ScrollEvent.SCROLL,       e -> reset.run());
        scene.addEventFilter(KeyEvent.KEY_PRESSED,     e -> reset.run());
        scene.addEventFilter(TouchEvent.TOUCH_PRESSED, e -> reset.run());

        resetTimer();
    }

    private static synchronized void resetTimer() {
        if (task != null) task.cancel(false);
        task = EXEC.schedule(IdleGuard::fireLogout, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    private static void fireLogout() {
        try {
            com.example.demo.api.ApiClient.post("/api/auth/logout", "{}");
        } catch (Exception ignore) {}

        Platform.runLater(() -> {
            try { TokenStore.clear(); } catch (Throwable ignored) {}
            try { AlertUtils.warn("Session expired due to inactivity. Please Login again !"); } catch (Throwable ignored) {}
            try { Launcher.go("login.fxml", "Login"); } catch (Throwable ignored) {}
        });
    }

}
