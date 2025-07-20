module desktop.app.desktopassistant {
    requires javafx.controls;
    requires javafx.fxml;
    requires sikulixapi;
    requires java.sql;
    requires java.net.http;
    requires com.sun.jna.platform;
    requires com.sun.jna;

    opens desktop.app.desktopassistant to javafx.fxml;
    exports desktop.app.desktopassistant;
}