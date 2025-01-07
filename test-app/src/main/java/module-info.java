module com.personal.testapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.personal.testapp to javafx.fxml;
    exports com.personal.testapp;
}