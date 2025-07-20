package com.snakeai.game;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
    private static final int CELL_SIZE = 20;
    private SnakeGame game;

    public GamePanel(SnakeGame game) {
        this.game = game;
        this.setPreferredSize(new Dimension(SnakeGame.BOARD_WIDTH * CELL_SIZE, SnakeGame.BOARD_HEIGHT * CELL_SIZE));
        this.setBackground(Color.BLACK);
    }

    public void setGame(SnakeGame game) {
        this.game = game;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw snake
        g.setColor(Color.GREEN);
        for (Point p : game.getSnake()) {
            g.fillRect(p.x() * CELL_SIZE, p.y() * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }

        // Draw snake head
        g.setColor(new Color(0, 150, 0));
        Point head = game.getSnake().getFirst();
        g.fillRect(head.x() * CELL_SIZE, head.y() * CELL_SIZE, CELL_SIZE, CELL_SIZE);


        // Draw food
        g.setColor(Color.RED);
        Point food = game.getFood();
        g.fillOval(food.x() * CELL_SIZE, food.y() * CELL_SIZE, CELL_SIZE, CELL_SIZE);

        // Draw score and game over
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + game.getScore(), 10, 20);

        if (game.isGameOver()) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            FontMetrics metrics = g.getFontMetrics();
            String gameOverMsg = "GAME OVER";
            g.drawString(gameOverMsg,
                    (getWidth() - metrics.stringWidth(gameOverMsg)) / 2,
                    getHeight() / 2);
        }
    }
}