import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel implements ActionListener {
    private static final int TILE_SIZE = 20;
    private static final int GRID_WIDTH = 25;
    private static final int GRID_HEIGHT = 25;
    private static final int GAME_SPEED = 1;

    private LinkedList<Point> snake;
    private Point food;
    private Timer timer;
    private boolean gameOver;

    public GamePanel() {
        setPreferredSize(new Dimension(GRID_WIDTH * TILE_SIZE, GRID_HEIGHT * TILE_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);

        startGame();
    }

    private void startGame() {
        snake = new LinkedList<>();
        snake.add(new Point(GRID_WIDTH / 2, GRID_HEIGHT / 2));

        spawnFood();
        gameOver = false;

        timer = new Timer(GAME_SPEED, this);
        timer.start();
    }

    private void spawnFood() {
        Random rand = new Random();
        int x, y;
        do {
            x = rand.nextInt(GRID_WIDTH);
            y = rand.nextInt(GRID_HEIGHT);
        } while (snake.contains(new Point(x, y)));
        food = new Point(x, y);
    }

    private List<Point> findPath(Point start, Point goal) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.fCost));
        Map<Point, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, null, 0, getHeuristic(start, goal));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.position.equals(goal)) {
                return reconstructPath(current);
            }

            for (Point neighbor : getNeighbors(current.position)) {
                if (snake.contains(neighbor)) continue; // Avoid body

                int gCost = current.gCost + 1;
                Node neighborNode = allNodes.getOrDefault(neighbor, new Node(neighbor, null, Integer.MAX_VALUE, getHeuristic(neighbor, goal)));

                if (gCost < neighborNode.gCost) {
                    neighborNode.gCost = gCost;
                    neighborNode.fCost = gCost + neighborNode.hCost;
                    neighborNode.parent = current;

                    openSet.add(neighborNode);
                    allNodes.put(neighbor, neighborNode);
                }
            }
        }
        return Collections.emptyList();
    }

    private int getHeuristic(Point p1, Point p2) {
        return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
    }

    private List<Point> reconstructPath(Node node) {
        List<Point> path = new LinkedList<>();
        while (node != null) {
            path.add(0, node.position);
            node = node.parent;
        }
        return path;
    }

    private List<Point> getNeighbors(Point p) {
        List<Point> neighbors = new ArrayList<>();
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        for (int[] d : directions) {
            int newX = p.x + d[0], newY = p.y + d[1];
            if (newX >= 0 && newX < GRID_WIDTH && newY >= 0 && newY < GRID_HEIGHT) {
                neighbors.add(new Point(newX, newY));
            }
        }
        return neighbors;
    }

    private void moveSnake() {
        if (gameOver) return;

        List<Point> path = findPath(snake.getFirst(), food);
        if (path.isEmpty()) {
            gameOver = true;
            return;
        }

        Point nextMove = path.get(1); // Take the next step towards food
        if (nextMove.equals(food)) {
            snake.addFirst(nextMove);
            spawnFood();
        } else {
            snake.addFirst(nextMove);
            snake.removeLast(); // Move forward
        }

        if (checkCollision()) {
            gameOver = true;
        }
    }

    private boolean checkCollision() {
        Point head = snake.getFirst();
        return snake.stream().skip(1).anyMatch(segment -> segment.equals(head));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("Game Over!", 60, getHeight() / 2);
            return;
        }

        g.setColor(Color.GREEN);
        for (Point p : snake) {
            g.fillRect(p.x * TILE_SIZE, p.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        g.setColor(Color.RED);
        g.fillOval(food.x * TILE_SIZE, food.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        moveSnake();
        repaint();
    }

    private static class Node {
        Point position;
        Node parent;
        int gCost, hCost, fCost;

        Node(Point position, Node parent, int gCost, int hCost) {
            this.position = position;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }
    }
}
