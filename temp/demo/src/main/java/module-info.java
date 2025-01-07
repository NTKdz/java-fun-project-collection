module com.personal.demo {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.personal.demo to javafx.fxml;
    exports com.personal.demo;
}