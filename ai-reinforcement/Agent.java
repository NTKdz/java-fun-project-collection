import java.io.*;
import java.util.*;

public class Agent {
    private Env env;
    private Random random;
    private Map<String, Double> qTable = new HashMap<>();
    private double alpha = 0.1; // Learning rate
    private double gamma = 0.9; // Discount factor
    private double epsilon = 1.0; // Exploration rate
    private double decay = 0.9999; // Epsilon decay rate
    private double minEpsilon = 0.001; // Minimum exploration rate
    private int maxScore = 0;

    public Agent() {
        env = new Env();
        random = new Random();
        run();
    }

    public void run() {
        int episode = 0;
        double totalScore = 0;

        long startTime = System.currentTimeMillis();
        while (maxScore < env.getWidth() * env.getHeight() - 1) {
            env.reset();
            double[] state = env.getState();
            boolean gameOver = false;
            int episodeScore = 0;

            while (!gameOver) {
                // Choose action
                int action = chooseAction(state);

                // Take action
                Map<String, Object> result = env.step(action);
                gameOver = (boolean) result.get("gameOver");
                double[] nextState = (double[]) result.get("state");
                episodeScore = (int) result.get("score");

                if (episodeScore >= env.getWidth() * env.getHeight() - 1) {
                    System.out.println("Episode " + episode + " won!, Score: "+ episodeScore);
                    break;
                }
                // Calculate reward
                double reward = calculateReward(result, gameOver, env.getSnake(), env.getFood());

                // Q-learning update
                updateQTable(state, action, reward, nextState);

                state = nextState;
            }

            // Update statistics
            totalScore += episodeScore;
            if (episodeScore > maxScore) maxScore = episodeScore;

            // Decay epsilon
            epsilon = Math.max(epsilon * decay, minEpsilon);

            // Print progress
            if (episode % 1000 == 0 && episode != 0) {
                double avgScore = totalScore / 1000;
                System.out.printf("Episode: %d, Avg: %.2f, Max: %d, Epsilon: %.4f%n",
                        episode, avgScore, maxScore, epsilon);
                totalScore = 0;
            }

            episode++;
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Total time: " + (endTime - startTime) / 1000 + " seconds");

//        saveQTable("q-table.txt");
    }

    private double calculateReward(Map<String, Object> result, boolean gameOver, Snake snake, Food food) {
        if (gameOver) {
            return -10.0; // Large penalty for losing the game
        }

        double reward = 0.0;

        if ((boolean) result.get("foodEaten")) {
            return 10.0; // Large reward for eating food
        }

        // Calculate distance to food before and after the move
        double prevDistance = Math.sqrt(
                Math.pow(food.getX() - snake.getX(), 2) +
                        Math.pow(food.getY() - snake.getY(), 2)
        );

        double newDistance = Math.sqrt(
                Math.pow(food.getX() - snake.getX(), 2) +
                        Math.pow(food.getY() - snake.getY(), 2)
        );

        // Reward for moving closer to the food
        double distanceReward = 0.0;
        if (newDistance < prevDistance) {
            distanceReward = 1.0; // Reward for moving closer
        } else if (newDistance > prevDistance) {
            distanceReward = -0.5; // Penalty for moving away
        }

        // Small penalty for each step to encourage efficiency
        double stepPenalty = -0.1;

        // Total reward
        return distanceReward + stepPenalty;
    }

    private boolean isNearWall(Snake snake) {
        int x = snake.getX();
        int y = snake.getY();
        return x <= 1 || x >= env.getWidth() - 2 || y <= 1 || y >= env.getHeight() - 2;
    }

    private boolean isNearBody(Snake snake) {
        int x = snake.getX();
        int y = snake.getY();
        for (int[] segment : snake.getBody()) {
            if (segment != snake.getBody().get(0) && // Exclude head
                    Math.abs(segment[0] - x) <= 1 && Math.abs(segment[1] - y) <= 1) {
                return true;
            }
        }
        return false;
    }

    private int chooseAction(double[] state) {
        if (random.nextDouble() < epsilon) {
            return random.nextInt(4); // Explore: choose a random action
        }
        return getBestAction(state); // Exploit: choose the best action
    }

    private int getBestAction(double[] state) {
        int bestAction = 0;
        double maxQ = Double.NEGATIVE_INFINITY;

        for (int action = 0; action < 4; action++) {
            double qValue = qTable.getOrDefault(stateToKey(state, action), 0.0);
            if (qValue > maxQ) {
                maxQ = qValue;
                bestAction = action;
            }
        }
        return bestAction;
    }

    private void updateQTable(double[] state, int action, double reward, double[] nextState) {
        String currentKey = stateToKey(state, action);
        double currentQ = qTable.getOrDefault(currentKey, 0.0);

        // Calculate max Q for next state
        double maxNextQ = Double.NEGATIVE_INFINITY;
        for (int a = 0; a < 4; a++) {
            double q = qTable.getOrDefault(stateToKey(nextState, a), 0.0);
            maxNextQ = Math.max(maxNextQ, q);
        }

        // Q-learning update rule
        double newQ = currentQ + alpha * (reward + gamma * maxNextQ - currentQ);
        qTable.put(currentKey, newQ);
    }

    private String stateToKey(double[] state, int action) {
        // Create unique key from state features and action
        StringBuilder key = new StringBuilder();
        for (double d : state) {
            key.append((int) d);
        }
        key.append(action);
        return key.toString();
    }

    public void saveQTable(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (Map.Entry<String, Double> entry : qTable.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
            System.out.println("Q-table saved to " + filename);
        } catch (IOException e) {
            System.err.println("Error saving Q-table: " + e.getMessage());
        }
    }

    public void loadQTable(String filename) {
        qTable.clear(); // Clear existing Q-table
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String key = parts[0];
                    double value = Double.parseDouble(parts[1]);
                    qTable.put(key, value);
                }
            }
            System.out.println("Q-table loaded from " + filename);
        } catch (IOException e) {
            System.err.println("Error loading Q-table: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Agent();
    }
}