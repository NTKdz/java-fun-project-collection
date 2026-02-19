package improve.project.chess;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ChessGame extends Application {
    public static final int EMPTY = 0;

    public static final int W_PAWN = 1;
    public static final int W_ROOK = 2;
    public static final int W_KNIGHT = 3;
    public static final int W_BISHOP = 4;
    public static final int W_QUEEN = 5;
    public static final int W_KING = 6;

    public static final int B_PAWN = -1;
    public static final int B_ROOK = -2;
    public static final int B_KNIGHT = -3;
    public static final int B_BISHOP = -4;
    public static final int B_QUEEN = -5;
    public static final int B_KING = -6;

    public int[][] board = {
            // Rank 8 (Black Pieces)
            {B_ROOK, B_KNIGHT, B_BISHOP, B_QUEEN, B_KING, B_BISHOP, B_KNIGHT, B_ROOK},
            // Rank 7 (Black Pawns)
            {B_PAWN, B_PAWN, B_PAWN, B_PAWN, B_PAWN, B_PAWN, B_PAWN, B_PAWN},
            // Rank 6 (Empty)
            {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
            // Rank 5 (Empty)
            {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
            // Rank 4 (Empty)
            {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
            // Rank 3 (Empty)
            {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
            // Rank 2 (White Pawns)
            {W_PAWN, W_PAWN, W_PAWN, W_PAWN, W_PAWN, W_PAWN, W_PAWN, W_PAWN},
            // Rank 1 (White Pieces)
            {W_ROOK, W_KNIGHT, W_BISHOP, W_QUEEN, W_KING, W_BISHOP, W_KNIGHT, W_ROOK}
    };

    private static final int TILE_SIZE = 80; // Width/Height of one square
    private static final String COLOR_LIGHT = "#eeeed2";
    private static final String COLOR_DARK = "#769656";

    @Override
    public void start(Stage primaryStage) {
        // 3. Create the Grid Layout
        GridPane boardGrid = new GridPane();

        // 4. Build the board based on the Model
        refreshBoard(boardGrid);

        Scene scene = new Scene(boardGrid);
        primaryStage.setTitle("JavaFX Chess");
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * loops through the 8x8 model and draws the squares and pieces.
     */
    private void refreshBoard(GridPane boardGrid) {
        boardGrid.getChildren().clear(); // Clear existing if refreshing

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {

                // A. Create the Square
                StackPane square = new StackPane();

                // Color logic: if row+col is even, it's light. Otherwise dark.
                String colorHex = (row + col) % 2 == 0 ? COLOR_LIGHT : COLOR_DARK;

                // We use a Rectangle for background color
                Rectangle bg = new Rectangle(TILE_SIZE, TILE_SIZE);
                bg.setFill(Color.web(colorHex));
                square.getChildren().add(bg);

                // B. Get piece data from your Model
                int pieceCode = board[row][col];

                // C. If there is a piece, draw it
                if (pieceCode != EMPTY) {
                    Label pieceLabel = new Label(getUnicodeSymbol(pieceCode));
                    pieceLabel.setFont(Font.font("Arial", 50)); // Large font for chess pieces

                    // Set color: White pieces (positive) are white, Black pieces (negative) are black
                    if (pieceCode > 0) {
                        pieceLabel.setTextFill(Color.WHITE);
                        // Optional: Add a subtle stroke or shadow if white text is hard to see on light squares
                        pieceLabel.setStyle("-fx-effect: dropshadow(one-pass-box, black, 2, 1, 0, 0);");
                    } else {
                        pieceLabel.setTextFill(Color.BLACK);
                    }

                    square.getChildren().add(pieceLabel);
                }

                // D. Add to GridPane
                boardGrid.add(square, col, row);
            }
        }
    }

    /**
     * Helper to convert your int codes to standard Unicode Chess symbols.
     */
    private String getUnicodeSymbol(int code) {
        return switch (code) {
            case W_KING -> "♔";
            case W_QUEEN -> "♕";
            case W_ROOK -> "♖";
            case W_BISHOP -> "♗";
            case W_KNIGHT -> "♘";
            case W_PAWN -> "♙";
            case B_KING -> "♚";
            case B_QUEEN -> "♛";
            case B_ROOK -> "♜";
            case B_BISHOP -> "♝";
            case B_KNIGHT -> "♞";
            case B_PAWN -> "♟";
            default -> "";
        };
    }
}