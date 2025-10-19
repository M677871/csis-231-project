module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;


    opens com.example.demo to javafx.fxml;
    opens com.example.demo.model to com.fasterxml.jackson.databind, javafx.base;
    exports com.example.demo;
}