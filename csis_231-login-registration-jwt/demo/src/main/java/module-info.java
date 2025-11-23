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

    // extra
    requires java.prefs;
    requires jdk.jfr;
    requires java.desktop;

    // packages used by FXML
    opens com.example.demo.auth to javafx.fxml;
    opens com.example.demo.admin to javafx.fxml;
    opens com.example.demo.instructor to javafx.fxml;
    opens com.example.demo.student to javafx.fxml;
    opens com.example.demo.common to javafx.fxml;

    // 🔥 ADD THIS LINE:
    opens com.example.demo.course to javafx.fxml;

    // models used in TableView, etc.
    opens com.example.demo.model to com.fasterxml.jackson.databind, javafx.base;

    exports com.example.demo;
}
