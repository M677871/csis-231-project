package com.example.demo;

import javafx.application.Application;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        // Initialize the global launcher with the primary stage
        Launcher.init(stage);

        // Optional: set minimum size so layouts don’t collapse
        stage.setMinWidth(800);
        stage.setMinHeight(520);

        // First screen
        Launcher.go("login.fxml", "Login");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
