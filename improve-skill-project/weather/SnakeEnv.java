import java.util.*;

/**
 * Snake Game Environment
 * Shared environment for all RL agents
 */
public class SnakeEnv {
    int gridSize = 20;
    LinkedList<int[]> snake = new LinkedList<>();
    int[] food;
    Random rand = new Random();
    boolean done = false;
    int stepsWithoutFood = 0;
    int maxStepsWithoutFood = 100;
    int score = 0;

    public SnakeEnv() {
        reset();
    }

    public void reset() {
        snake.clear();
        snake.add(new int[]{gridSize / 2, gridSize / 2});
        placeFood();
        done = false;
        stepsWithoutFood = 0;
        score = 0;
    }

    private void placeFood() {
        Set<String> snakePositions = new HashSet<>();
        for (int[] p : snake) {
            snakePositions.add(p[0] + "," + p[1]);
        }
        List<int[]> availablePositions = new ArrayList<>();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (!snakePositions.contains(i + "," + j)) {
                    availablePositions.add(new int[]{i, j});
                }
            }
        }
        if (!availablePositions.isEmpty()) {
            food = availablePositions.get(rand.nextInt(availablePositions.size()));
        }
    }

    public StepResult step(int action) {
        if (done) return new StepResult(getEnhancedState(), 0f, true);

        int[] head = snake.peekFirst().clone();
        int[] oldHead = head.clone();

        switch (action) {
            case 0 -> head[0]--; // up
            case 1 -> head[0]++; // down
            case 2 -> head[1]--; // left
            case 3 -> head[1]++; // right
        }

        // Check wall collision
        if (head[0] < 0 || head[0] >= gridSize || head[1] < 0 || head[1] >= gridSize) {
            done = true;
            return new StepResult(getEnhancedState(), -10f, true);
        }

        // Check self collision
        for (int[] p : snake) {
            if (p[0] == head[0] && p[1] == head[1]) {
                done = true;
                return new StepResult(getEnhancedState(), -10f, true);
            }
        }

        snake.addFirst(head);
        stepsWithoutFood++;
        float reward = 0f;

        // Check food consumption
        if (head[0] == food[0] && head[1] == food[1]) {
            reward = 10f;
            score++;
            stepsWithoutFood = 0;
            placeFood();
        } else {
            snake.removeLast();
            // Distance-based reward shaping
            double oldDistance = Math.sqrt(Math.pow(oldHead[0] - food[0], 2) +
                    Math.pow(oldHead[1] - food[1], 2));
            double newDistance = Math.sqrt(Math.pow(head[0] - food[0], 2) +
                    Math.pow(head[1] - food[1], 2));
            if (newDistance < oldDistance) {
                reward = 0.1f;
            } else {
                reward = -0.15f;
            }
        }

        // Timeout penalty
        if (stepsWithoutFood >= maxStepsWithoutFood) {
            done = true;
            reward = -5f;
        }

        return new StepResult(getEnhancedState(), reward, done);
    }

    public boolean isDone() {
        return done;
    }

    public float[] getEnhancedState() {
        int[] head = snake.peekFirst();
        int stateSize = 12 + (gridSize * gridSize);
        float[] state = new float[stateSize];
        int idx = 0;

        // Danger detection (4 directions)
        state[idx++] = isDanger(head[0] - 1, head[1]) ? 1f : 0f;
        state[idx++] = isDanger(head[0] + 1, head[1]) ? 1f : 0f;
        state[idx++] = isDanger(head[0], head[1] - 1) ? 1f : 0f;
        state[idx++] = isDanger(head[0], head[1] + 1) ? 1f : 0f;

        // Food direction (4 directions)
        state[idx++] = food[0] < head[0] ? 1f : 0f;
        state[idx++] = food[0] > head[0] ? 1f : 0f;
        state[idx++] = food[1] < head[1] ? 1f : 0f;
        state[idx++] = food[1] > head[1] ? 1f : 0f;

        // Current direction (4 directions)
        if (snake.size() > 1) {
            int[] neck = snake.get(1);
            state[idx++] = neck[0] < head[0] ? 1f : 0f;
            state[idx++] = neck[0] > head[0] ? 1f : 0f;
            state[idx++] = neck[1] < head[1] ? 1f : 0f;
            state[idx++] = neck[1] > head[1] ? 1f : 0f;
        } else {
            idx += 4;
        }

        // Grid state
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                boolean isSnake = false;
                for (int[] p : snake) {
                    if (p[0] == i && p[1] == j) {
                        isSnake = true;
                        break;
                    }
                }
                if (isSnake) {
                    state[idx++] = 1f;
                } else if (food[0] == i && food[1] == j) {
                    state[idx++] = 0.5f;
                } else {
                    state[idx++] = 0f;
                }
            }
        }

        return state;
    }

    private boolean isDanger(int row, int col) {
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) {
            return true;
        }
        for (int[] p : snake) {
            if (p[0] == row && p[1] == col) {
                return true;
            }
        }
        return false;
    }

    public int getScore() {
        return score;
    }

    public int getStateSize() {
        return getEnhancedState().length;
    }

    public int getActionSize() {
        return 4;
    }

    // Helper class for step results
    public static class StepResult {
        public float[] nextState;
        public float reward;
        public boolean done;

        public StepResult(float[] nextState, float reward, boolean done) {
            this.nextState = nextState;
            this.reward = reward;
            this.done = done;
        }
    }
}

