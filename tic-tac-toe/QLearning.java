import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class QLearning {
    private double epsilon = 1.0;      // Exploration rate
    private double decay = 0.9995;     // Decay rate for exploration
    private double learningRate = 0.01; // Learning rate (alpha)
    private double discountRate = 0.9; // Discount factor (gamma)

    private final Random random = new Random();
    private final Map<String, double[]> qTable = new HashMap<>();

    public QLearning() {}

    public QLearning(double epsilon, double decay, double learningRate, double discountRate) {
        this.epsilon = epsilon;
        this.decay = decay;
        this.learningRate = learningRate;
        this.discountRate = discountRate;
    }

    // Get Q-values for a given state (initialize if not exists)
    private double[] getQValues(String state, int actionSize) {
        return qTable.computeIfAbsent(state, k -> new double[actionSize]);
    }

    // Choose action using ε-greedy strategy
    public int chooseAction(String state, int actionSize) {
        double[] qValues = getQValues(state, actionSize);

        if (random.nextDouble() < epsilon) {
            // Exploration: Choose random action
            return random.nextInt(actionSize);
        } else {
            // Exploitation: Choose best action
            int bestAction = 0;
            for (int i = 1; i < actionSize; i++) {
                if (qValues[i] > qValues[bestAction]) {
                    bestAction = i;
                }
            }
            return bestAction;
        }
    }

    // Update Q-table using the Q-learning formula
    public void updateQValue(String state, int action, double reward, String nextState, int actionSize) {
        double[] qValues = getQValues(state, actionSize);
        double[] nextQValues = getQValues(nextState, actionSize);

        // Bellman equation: Q(s, a) = Q(s, a) + α [R + γ max_a' Q(s', a') - Q(s, a)]
        double maxNextQ = Double.NEGATIVE_INFINITY;
        for (double q : nextQValues) {
            if (q > maxNextQ) maxNextQ = q;
        }

        qValues[action] += learningRate * (reward + discountRate * maxNextQ - qValues[action]);
    }

    // Decay epsilon over time to shift from exploration to exploitation
    public void decayEpsilon() {
        epsilon = Math.max(0.1, epsilon * decay);
    }
}
