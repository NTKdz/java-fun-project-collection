module improve.project.chess {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    opens improve.project.chess.controller to javafx.fxml;
    opens improve.project.chess to javafx.fxml;
    exports improve.project.chess;
}