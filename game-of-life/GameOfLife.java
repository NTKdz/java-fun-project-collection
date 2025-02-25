import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GameOfLife extends JPanel {
    private static final int GRID_SIZE = 50;  // Grid width/height
    private static final int CELL_SIZE = 10;  // Size of each cell
    private boolean[][] grid = new boolean[GRID_SIZE][GRID_SIZE];
    private boolean running = false;

    public GameOfLife() {
        setPreferredSize(new Dimension(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE));
        setBackground(Color.BLACK);
        JPanel panel = new JPanel();

        // Handle mouse clicks to toggle cells
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX() / CELL_SIZE;
                int y = e.getY() / CELL_SIZE;
                grid[y][x] = !grid[y][x];  // Toggle cell
                repaint();
            }
        });

        // Timer for game loop
        Timer timer = new Timer(100, e -> {
            if (running) {
                step();
                repaint();
            }
        });
        timer.start();
    }

    // Compute the next generation
    private void step() {
        boolean[][] newGrid = new boolean[GRID_SIZE][GRID_SIZE];

        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int neighbors = countLiveNeighbors(y, x);

                if (grid[y][x]) {
                    newGrid[y][x] = (neighbors == 2 || neighbors == 3); // Survival
                } else {
                    newGrid[y][x] = (neighbors == 3); // Birth
                }
            }
        }

        grid = newGrid;
    }

    // Count live neighbors around (row, col)
    private int countLiveNeighbors(int row, int col) {
        int count = 0;
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                if (y == 0 && x == 0) continue;  // Skip itself
                int ny = row + y, nx = col + x;
                if (ny >= 0 && ny < GRID_SIZE && nx >= 0 && nx < GRID_SIZE && grid[ny][nx]) {
                    count++;
                }
            }
        }
        return count;
    }

    // Draw grid and cells
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.GRAY);

        // Draw grid lines
        for (int i = 0; i < GRID_SIZE; i++) {
            g.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, getHeight());
            g.drawLine(0, i * CELL_SIZE, getWidth(), i * CELL_SIZE);
        }

        // Draw cells
        g.setColor(Color.GREEN);
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (grid[y][x]) {
                    g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }

    // Main method
    public static void main(String[] args) {
        JFrame frame = new JFrame("Game of Life");
        GameOfLife game = new GameOfLife();

        // Button panel
        JPanel panel = new JPanel();
        JButton startButton = new JButton("Start");
        JButton clearButton = new JButton("Clear");

        // Start/Stop button
        startButton.addActionListener(e -> {
            game.running = !game.running;
            startButton.setText(game.running ? "Stop" : "Start");
        });

        // Clear button
        clearButton.addActionListener(e -> {
            game.grid = new boolean[GRID_SIZE][GRID_SIZE];
            game.repaint();
        });

        panel.add(startButton);
        panel.add(clearButton);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
