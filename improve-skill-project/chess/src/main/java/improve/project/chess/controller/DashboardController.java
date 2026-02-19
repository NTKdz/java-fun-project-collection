package improve.project.chess.controller;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class DashboardController {

    @FXML
    private VBox reportsSubMenu;

    @FXML
    private VBox sideMenu;

    @FXML
    public void initialize() {
        // Toggle submenu on button click
        sideMenu.lookupAll(".button").forEach(node -> {
            if ("Reports".equals(((javafx.scene.control.Button) node).getText())) {
                node.setOnMouseClicked(e -> toggleSubMenu());
            }
        });
    }

    private void toggleSubMenu() {
        boolean isVisible = reportsSubMenu.isVisible();
        if (!isVisible) {
            reportsSubMenu.setVisible(true);
            reportsSubMenu.setManaged(true);
            // Animation: slide down
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), reportsSubMenu);
            tt.setFromY(reportsSubMenu.getHeight());
            tt.setToY(0);
            tt.play();
        } else {
            // Animation: slide up
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), reportsSubMenu);
            tt.setFromY(0);
            tt.setToY(-reportsSubMenu.getHeight());
            tt.setOnFinished(event -> {
                reportsSubMenu.setVisible(false);
                reportsSubMenu.setManaged(false);
            });
            tt.play();
        }
    }
}
