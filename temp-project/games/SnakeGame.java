package games;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class SnakeGame extends JPanel implements ActionListener {
    private final int TILE_SIZE = 20; // Size of each grid tile
    private final int WIDTH = 30;    // Number of tiles horizontally
    private final int HEIGHT = 20;   // Number of tiles vertically
    private final int FRAME_RATE = 60; // Rendering frame rate (60 FPS)
    private int gameSpeed = 15;       // Initial game speed (updates per second)

    private final ArrayList<Point> snake = new ArrayList<>(); // Snake body
    private Point food; // Food position
    private char direction = 'R'; // Initial direction: R = Right, L = Left, U = Up, D = Down
    private boolean running = true; // Game running status
    private int ticks = 0; // Keeps track of game updates

    private Timer timer; // Frame timer

    public SnakeGame() {
        setPreferredSize(new Dimension(WIDTH * TILE_SIZE, HEIGHT * TILE_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        if (direction != 'D') direction = 'U';
                        break;
                    case KeyEvent.VK_DOWN:
                        if (direction != 'U') direction = 'D';
                        break;
                    case KeyEvent.VK_LEFT:
                        if (direction != 'R') direction = 'L';
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (direction != 'L') direction = 'R';
                        break;
                }
            }
        });

        startGame();
    }

    private void startGame() {
        // Initialize the snake with 3 segments
        snake.clear();
        snake.add(new Point(5, 5));
        snake.add(new Point(4, 5));
        snake.add(new Point(3, 5));

        // Spawn initial food
        spawnFood();

        // Start the game loop (60 FPS)
        timer = new Timer(1000 / FRAME_RATE, this);
        timer.start();
    }

    private void spawnFood() {
        Random random = new Random();
        // Ensure food doesn't spawn on the snake
        do {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            food = new Point(x, y);
        } while (snake.contains(food));
    }

    private void moveSnake() {
        // Calculate new head position
        Point head = snake.getFirst();
        Point newHead = switch (direction) {
            case 'U' -> new Point(head.x, head.y - 1);
            case 'D' -> new Point(head.x, head.y + 1);
            case 'L' -> new Point(head.x - 1, head.y);
            case 'R' -> new Point(head.x + 1, head.y);
            default -> head;
        };

        // Check collisions
        if (newHead.x < 0 || newHead.x >= WIDTH || newHead.y < 0 || newHead.y >= HEIGHT || snake.contains(newHead)) {
            running = false;
            timer.stop();
        } else {
            // Add new head to the snake
            snake.addFirst(newHead);

            // Check if the snake eats the food
            if (newHead.equals(food)) {
                spawnFood();
                gameSpeed++; // Increase speed when food is eaten
            } else {
                snake.removeLast(); // Remove tail
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            ticks++;

            // Update game logic at the current speed
            if (ticks >= FRAME_RATE / gameSpeed) {
                moveSnake();
                ticks = 0;
            }

            repaint(); // Redraw the game
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw food
        g.setColor(Color.RED);
        g.fillRect(food.x * TILE_SIZE, food.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        // Draw snake
        g.setColor(Color.GREEN);
        for (Point segment : snake) {
            g.fillRect(segment.x * TILE_SIZE, segment.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        // Draw game over message
        if (!running) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("Game Over", WIDTH * TILE_SIZE / 4, HEIGHT * TILE_SIZE / 2);
        }

        // Draw score (snake size - 3, since the initial size is 3)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Score: " + (snake.size() - 3), 10, 20);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Snake Game");
        SnakeGame gamePanel = new SnakeGame();

        frame.add(gamePanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); // Center the window
        frame.setVisible(true);
    }
}
