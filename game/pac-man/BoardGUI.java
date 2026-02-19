import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class BoardGUI extends JPanel implements KeyListener, ActionListener {

    private Board board;
    private final int TILE_SIZE = 40; // size of each cell in pixels
    private Timer timer;

    public BoardGUI(Board board) {
        this.board = board;
        setPreferredSize(new Dimension(board.getWidth() * TILE_SIZE, board.getHeight() * TILE_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(300, this); // update every 300ms
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int[][] grid = board.getGrid();

        // Draw the grid
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                int value = grid[y][x];
                if (value == 1) { // wall
                    g.setColor(Color.BLUE);
                    g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                } else if (value == 2) { // pellet
                    g.setColor(Color.YELLOW);
                    g.fillOval(x * TILE_SIZE + TILE_SIZE / 3, y * TILE_SIZE + TILE_SIZE / 3,
                            TILE_SIZE / 3, TILE_SIZE / 3);
                }
            }
        }

        // Draw Pacman
        Movable pacman = board.getPacman();
        g.setColor(Color.ORANGE);
        g.fillOval(pacman.getX() * TILE_SIZE, pacman.getY() * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        // Draw Enemies
        for (Movable enemy : board.getEnemies()) {
            g.setColor(Color.RED);
            g.fillOval(enemy.getX() * TILE_SIZE, enemy.getY() * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        Movable.Direction dir = board.getPacman().getDirection();
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> dir = Movable.Direction.UP;
            case KeyEvent.VK_DOWN -> dir = Movable.Direction.DOWN;
            case KeyEvent.VK_LEFT -> dir = Movable.Direction.LEFT;
            case KeyEvent.VK_RIGHT -> dir = Movable.Direction.RIGHT;
        }
        board.getPacman().setDirection(dir);
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void actionPerformed(ActionEvent e) {
        board.nextFrame();
        repaint();
    }

    public static void main(String[] args) {
        Board board = new Board(10, 7);
        board.reset();

        JFrame frame = new JFrame("Pac-Man");
        BoardGUI panel = new BoardGUI(board);

        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
