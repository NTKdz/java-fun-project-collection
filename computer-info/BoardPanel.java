// src/BoardPanel.java
import javax.swing.*;
import java.awt.*;

public class BoardPanel extends JPanel {
    private Board board;
    private final int CELL_SIZE;

    private final Color[] NUM_COLORS = {
            Color.BLUE, new Color(0, 128, 0), Color.RED, new Color(0, 0, 128),
            new Color(128, 0, 0), Color.CYAN, Color.BLACK, Color.GRAY
    };

    public BoardPanel(Board board, int cellSize) {
        this.board = board;
        this.CELL_SIZE = cellSize;
        int width = board.getWidth() * CELL_SIZE;
        int height = board.getHeight() * CELL_SIZE;
        setPreferredSize(new Dimension(width, height));
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (board == null) return;

        for (int r = 0; r < board.getHeight(); r++) {
            for (int c = 0; c < board.getWidth(); c++) {
                drawCell(g, r, c);
            }
        }
    }

    private void drawCell(Graphics g, int r, int c) {
        int x = c * CELL_SIZE;
        int y = r * CELL_SIZE;
        Cell cell = board.getCell(r, c);

        // Base look
        g.setFont(new Font("Arial", Font.BOLD, CELL_SIZE / 2));
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(x, y, CELL_SIZE, CELL_SIZE);
        g.setColor(Color.DARK_GRAY);
        g.drawRect(x, y, CELL_SIZE, CELL_SIZE);

        if (!cell.isRevealed) {
            if (cell.isFlagged) {
                g.setColor(Color.ORANGE);
                g.drawString("F", x + CELL_SIZE / 3, y + 2 * CELL_SIZE / 3);
            } else {
                // Draw a lid for unrevealed cells
                g.setColor(Color.GRAY);
                g.fillRect(x,y,CELL_SIZE,CELL_SIZE);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x,y,CELL_SIZE,CELL_SIZE);
            }
            return;
        }

        if (cell.isMine) {
            g.setColor(Color.RED);
            g.fillRect(x, y, CELL_SIZE, CELL_SIZE);
            g.setColor(Color.BLACK);
            g.drawString("*", x + CELL_SIZE / 3, y + 2 * CELL_SIZE / 3);
        } else if (cell.adjacentMines > 0) {
            g.setColor(NUM_COLORS[cell.adjacentMines - 1]);
            g.drawString(String.valueOf(cell.adjacentMines), x + CELL_SIZE / 3, y + 2 * CELL_SIZE / 3);
        }
    }
}