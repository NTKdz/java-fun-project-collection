import java.util.*;

/**
 * Enhanced Snake Environment with Better Reward Shaping
 * Optimized for faster learning and higher scores
 */
public class EnhancedSnakeEnv {
    int gridSize = 20;
    LinkedList<int[]> snake = new LinkedList<>();
    int[] food;
    Random rand = new Random();
    boolean done = false;
    int stepsWithoutFood = 0;
    int maxStepsWithoutFood = 200;  // Increased from 100
    int score = 0;
    int totalSteps = 0;

    public EnhancedSnakeEnv() {
        reset();
    }

    public void reset() {
        snake.clear();
        snake.add(new int[]{gridSize / 2, gridSize / 2});
        placeFood();
        done = false;
        stepsWithoutFood = 0;
        score = 0;
        totalSteps = 0;
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
        if (done) return new StepResult(getCompactState(), 0f, true);

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
            return new StepResult(getCompactState(), -24f, true);  // Bigger penalty
        }

        // Check self collision
        for (int[] p : snake) {
            if (p[0] == head[0] && p[1] == head[1]) {
                done = true;
                return new StepResult(getCompactState(), -24f, true);  // Bigger penalty
            }
        }

        snake.addFirst(head);
        stepsWithoutFood++;
        totalSteps++;
        float reward = 0f;

        // Calculate distances
        double oldDistance = manhattanDistance(oldHead, food);
        double newDistance = manhattanDistance(head, food);

        // Check food consumption
        if (head[0] == food[0] && head[1] == food[1]) {
            reward = 20f;  // Much bigger reward for eating
            score++;
            stepsWithoutFood = 0;
            placeFood();

//            // Bonus for longer snake
//            if (score > 5) {
//                reward += score * 0.5f;  // Progressive bonus
//            }
        } else {
            snake.removeLast();

            // Better distance-based shaping
            if (newDistance < oldDistance) {
                reward = 0.3f;  // Stronger positive feedback
            } else {
                reward = -0.5f;  // Stronger negative feedback
            }

            // Small penalty for each step to encourage efficiency
            reward -= 0.01f;
        }

        // Timeout penalty
        if (stepsWithoutFood >= maxStepsWithoutFood) {
            done = true;
            reward = -10f;
        }

        // Survival bonus for long games
        if (totalSteps % 50 == 0 && score > 0) {
            reward += 0.5f;
        }

        return new StepResult(getCompactState(), reward, done);
    }

    private double manhattanDistance(int[] p1, int[] p2) {
        return Math.abs(p1[0] - p2[0]) + Math.abs(p1[1] - p2[1]);
    }

    public boolean isDone() {
        return done;
    }

    /**
     * Compact state representation - focus on what matters
     * 52 features instead of 412:
     * - 8 danger indicators (adjacent + diagonal)
     * - 8 food direction indicators
     * - 4 current direction
     * - 8 distance features
     * - 4 wall distances
     * - 20 vision rays (4 directions x 5 types: see_body, see_food, dis_body, dis_food, dis_wall)
     */
    public float[] getCompactState() {
        int[] head = snake.peekFirst();
        float[] state = new float[52];
        int idx = 0;

        // Danger detection (8 directions: N, S, E, W, NE, NW, SE, SW)
        state[idx++] = isDanger(head[0] - 1, head[1]) ? 1f : 0f;  // N
        state[idx++] = isDanger(head[0] + 1, head[1]) ? 1f : 0f;  // S
        state[idx++] = isDanger(head[0], head[1] - 1) ? 1f : 0f;  // W
        state[idx++] = isDanger(head[0], head[1] + 1) ? 1f : 0f;  // E
        state[idx++] = isDanger(head[0] - 1, head[1] - 1) ? 1f : 0f;  // NW
        state[idx++] = isDanger(head[0] - 1, head[1] + 1) ? 1f : 0f;  // NE
        state[idx++] = isDanger(head[0] + 1, head[1] - 1) ? 1f : 0f;  // SW
        state[idx++] = isDanger(head[0] + 1, head[1] + 1) ? 1f : 0f;  // SE

        // Food direction (8 directions)
        state[idx++] = food[0] < head[0] ? 1f : 0f;  // N
        state[idx++] = food[0] > head[0] ? 1f : 0f;  // S
        state[idx++] = food[1] < head[1] ? 1f : 0f;  // W
        state[idx++] = food[1] > head[1] ? 1f : 0f;  // E
        state[idx++] = (food[0] < head[0] && food[1] < head[1]) ? 1f : 0f;  // NW
        state[idx++] = (food[0] < head[0] && food[1] > head[1]) ? 1f : 0f;  // NE
        state[idx++] = (food[0] > head[0] && food[1] < head[1]) ? 1f : 0f;  // SW
        state[idx++] = (food[0] > head[0] && food[1] > head[1]) ? 1f : 0f;  // SE

        // Current direction (4)
        if (snake.size() > 1) {
            int[] neck = snake.get(1);
            state[idx++] = neck[0] < head[0] ? 1f : 0f;  // Moving down
            state[idx++] = neck[0] > head[0] ? 1f : 0f;  // Moving up
            state[idx++] = neck[1] < head[1] ? 1f : 0f;  // Moving right
            state[idx++] = neck[1] > head[1] ? 1f : 0f;  // Moving left
        } else {
            idx += 4;
        }

        // Distance features (8) - normalized
        double distToFood = manhattanDistance(head, food) / (gridSize * 2);
        state[idx++] = (float) distToFood;
        state[idx++] = (float) (Math.abs(food[0] - head[0])) / gridSize;
        state[idx++] = (float) (Math.abs(food[1] - head[1])) / gridSize;
        state[idx++] = (float) snake.size() / (gridSize * gridSize);  // Snake length
        state[idx++] = (float) stepsWithoutFood / maxStepsWithoutFood;  // Hunger
        state[idx++] = (float) head[0] / gridSize;  // Position X
        state[idx++] = (float) head[1] / gridSize;  // Position Y
        state[idx++] = (float) score / 50;  // Score normalized

        // Wall distances (4 directions) - normalized
        state[idx++] = (float) head[0] / gridSize;  // Distance to top
        state[idx++] = (float) (gridSize - head[0]) / gridSize;  // Distance to bottom
        state[idx++] = (float) head[1] / gridSize;  // Distance to left
        state[idx++] = (float) (gridSize - head[1]) / gridSize;  // Distance to right

        // Vision rays (20 = 4 directions x 5 features)
        // For each direction: see_body, see_food, dis_body, dis_food, dis_wall
        addVisionRay(state, idx, head, -1, 0);  // Up
        addVisionRay(state, idx + 5, head, 1, 0);  // Down
        addVisionRay(state, idx + 10, head, 0, -1);  // Left
        addVisionRay(state, idx + 15, head, 0, 1);  // Right

        return state;
    }

    private void addVisionRay(float[] state, int idx, int[] head, int dr, int dc) {
        int wallDist = 0;
        int bodyDist = gridSize;
        int foodDist = gridSize;
        boolean hasFood = false;

        int r = head[0] + dr;
        int c = head[1] + dc;
        int dist = 1;

        while (r >= 0 && r < gridSize && c >= 0 && c < gridSize) {
            if (food[0] == r && food[1] == c) {
                hasFood = true;
                foodDist = dist;
            }

            for (int[] p : snake) {
                if (p[0] == r && p[1] == c) {
                    bodyDist = Math.min(bodyDist, dist);
                    break;
                }
            }

            r += dr;
            c += dc;
            dist++;
        }

        wallDist = dist - 1;

        float see_body = (bodyDist < gridSize) ? 1f : 0f;
        float see_food = hasFood ? 1f : 0f;

        state[idx] = see_body;
        state[idx + 1] = see_food;
        state[idx + 2] = (float) bodyDist / gridSize;
        state[idx + 3] = (float) foodDist / gridSize;
        state[idx + 4] = (float) wallDist / gridSize;
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
        return getCompactState().length;
    }

    public int getActionSize() {
        return 4;
    }

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