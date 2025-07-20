package com.snakeai.game;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class SnakeGame {

    public static final int BOARD_WIDTH = 20;
    public static final int BOARD_HEIGHT = 20;
    public static final int STATE_SIZE = 11;

    private LinkedList<Point> snake;
    private Point food;
    private Direction direction;
    private boolean gameOver;
    private int score;
    private Random random = new Random();

    public SnakeGame() {
        reset();
    }

    public void reset() {
        direction = Direction.RIGHT;
        snake = new LinkedList<>();
        Point head = new Point(BOARD_WIDTH / 2, BOARD_HEIGHT / 2);
        snake.add(head);
        snake.add(new Point(head.x() - 1, head.y()));
        snake.add(new Point(head.x() - 2, head.y()));
        score = 0;
        placeFood();
        gameOver = false;
    }

    private void placeFood() {
        while (true) {
            int x = random.nextInt(BOARD_WIDTH);
            int y = random.nextInt(BOARD_HEIGHT);
            Point newFood = new Point(x, y);
            if (!snake.contains(newFood)) {
                food = newFood;
                return;
            }
        }
    }

    /**
     * The main game step function for the AI.
     * @param action The action chosen by the agent [straight, right, left].
     * @return A reward for the action taken.
     */
    public int playStep(int[] action) {
        if (gameOver) return 0;

        // Determine new direction based on action
        updateDirection(action);

        Point head = snake.getFirst();
        Point newHead = moveHead(head);

        // Check for game over conditions
        if (isCollision(newHead)) {
            gameOver = true;
            return -10; // Negative reward for dying
        }

        snake.addFirst(newHead);

        // Check for food
        if (newHead.equals(food)) {
            score++;
            placeFood();
            return 10; // Positive reward for eating food
        } else {
            snake.removeLast();
        }
        return 0; // No reward if nothing special happens
    }

    private void updateDirection(int[] action) {
        // action = [1, 0, 0] -> straight
        // action = [0, 1, 0] -> right turn
        // action = [0, 0, 1] -> left turn
        Direction[] clockWise = {Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT};
        int idx = -1;
        for (int i = 0; i < clockWise.length; i++) {
            if (clockWise[i] == this.direction) {
                idx = i;
                break;
            }
        }

        if (action[1] == 1) { // Right turn
            int nextIdx = (idx + 1) % 4;
            this.direction = clockWise[nextIdx];
        } else if (action[2] == 1) { // Left turn
            int nextIdx = (idx - 1 + 4) % 4;
            this.direction = clockWise[nextIdx];
        }
        // If action[0] == 1, we do nothing and continue straight
    }

    private Point moveHead(Point head) {
        return switch (direction) {
            case UP -> new Point(head.x(), head.y() - 1);
            case DOWN -> new Point(head.x(), head.y() + 1);
            case LEFT -> new Point(head.x() - 1, head.y());
            case RIGHT -> new Point(head.x() + 1, head.y());
        };
    }

    private boolean isCollision(Point point) {
        // Wall collision
        if (point.x() < 0 || point.x() >= BOARD_WIDTH || point.y() < 0 || point.y() >= BOARD_HEIGHT) {
            return true;
        }
        // Self collision
        return snake.contains(point);
    }

    /**
     * Generates the state vector for the neural network.
     * State is an 11-element array representing:
     * - Dangers (wall or self) in 3 directions (straight, right, left)
     * - Current direction of snake (one-hot encoded)
     * - Food location relative to snake head
     */
    public INDArray getState() {
        Point head = snake.getFirst();

        Point point_l = new Point(head.x() - 1, head.y());
        Point point_r = new Point(head.x() + 1, head.y());
        Point point_u = new Point(head.x(), head.y() - 1);
        Point point_d = new Point(head.x(), head.y() + 1);

        boolean dir_l = direction == Direction.LEFT;
        boolean dir_r = direction == Direction.RIGHT;
        boolean dir_u = direction == Direction.UP;
        boolean dir_d = direction == Direction.DOWN;

        double[] state = new double[STATE_SIZE];

        // Danger states
        state[0] = (dir_r && isCollision(point_r)) || (dir_l && isCollision(point_l)) || (dir_u && isCollision(point_u)) || (dir_d && isCollision(point_d)) ? 1 : 0; // danger straight
        state[1] = (dir_u && isCollision(point_r)) || (dir_d && isCollision(point_l)) || (dir_l && isCollision(point_u)) || (dir_r && isCollision(point_d)) ? 1 : 0; // danger right
        state[2] = (dir_d && isCollision(point_r)) || (dir_u && isCollision(point_l)) || (dir_r && isCollision(point_u)) || (dir_l && isCollision(point_d)) ? 1 : 0; // danger left

        // Move direction
        state[3] = dir_l ? 1 : 0;
        state[4] = dir_r ? 1 : 0;
        state[5] = dir_u ? 1 : 0;
        state[6] = dir_d ? 1 : 0;

        // Food location
        state[7] = food.x() < head.x() ? 1 : 0; // food left
        state[8] = food.x() > head.x() ? 1 : 0; // food right
        state[9] = food.y() < head.y() ? 1 : 0; // food up
        state[10] = food.y() > head.y() ? 1 : 0; // food down

        return Nd4j.create(state).reshape(1, STATE_SIZE);
    }

    // Getters
    public LinkedList<Point> getSnake() { return snake; }
    public Point getFood() { return food; }
    public int getScore() { return score; }
    public boolean isGameOver() { return gameOver; }
}