module com.example.demo {
    // JavaFX
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;

    // HTTP + Jackson
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    // ⬇️ Add this
    requires java.prefs;


    opens com.example.demo.controllers to javafx.fxml;
    opens com.example.demo.model to com.fasterxml.jackson.databind, javafx.base;

    exports com.example.demo;
}
