import java.util.*;

/**
 * Prioritized Experience Replay Buffer
 * Samples transitions based on their TD error priority
 */
public class PrioritizedReplayBuffer {

    public static class PrioritizedTransition {
        public float[] state;
        public int action;
        public float reward;
        public float[] nextState;
        public boolean done;
        public float priority;
        public int index;

        public PrioritizedTransition(float[] state, int action, float reward,
                                     float[] nextState, boolean done, float priority, int index) {
            this.state = state;
            this.action = action;
            this.reward = reward;
            this.nextState = nextState;
            this.done = done;
            this.priority = priority;
            this.index = index;
        }
    }

    private List<PrioritizedTransition> buffer = new ArrayList<>();
    private int maxSize;
    private Random rand = new Random();
    private float alpha = 0.6f; // Priority exponent
    private float beta = 0.4f; // Importance sampling exponent
    private float betaIncrement = 0.001f;
    private float epsilon = 1e-6f; // Small constant to avoid zero priority

    public PrioritizedReplayBuffer(int maxSize) {
        this.maxSize = maxSize;
    }

    public void add(float[] state, int action, float reward, float[] nextState, boolean done) {
        float maxPriority = buffer.isEmpty() ? 1.0f :
                buffer.stream().map(t -> t.priority).max(Float::compare).orElse(1.0f);

        if (buffer.size() >= maxSize) {
            buffer.remove(0);
            // Update indices
            for (int i = 0; i < buffer.size(); i++) {
                buffer.get(i).index = i;
            }
        }

        PrioritizedTransition t = new PrioritizedTransition(
                state, action, reward, nextState, done, maxPriority, buffer.size()
        );
        buffer.add(t);
    }

    public SampleResult sample(int batchSize) {
        List<PrioritizedTransition> batch = new ArrayList<>(batchSize);
        float[] weights = new float[batchSize];
        int[] indices = new int[batchSize];

        // Calculate priorities
        float[] priorities = new float[buffer.size()];
        float sumPriorities = 0;
        for (int i = 0; i < buffer.size(); i++) {
            priorities[i] = (float) Math.pow(buffer.get(i).priority + epsilon, alpha);
            sumPriorities += priorities[i];
        }

        // Normalize priorities
        for (int i = 0; i < priorities.length; i++) {
            priorities[i] /= sumPriorities;
        }

        // Sample based on priorities
        for (int i = 0; i < batchSize; i++) {
            float r = rand.nextFloat();
            float cumSum = 0;
            int selectedIdx = 0;

            for (int j = 0; j < priorities.length; j++) {
                cumSum += priorities[j];
                if (r < cumSum) {
                    selectedIdx = j;
                    break;
                }
            }

            PrioritizedTransition t = buffer.get(selectedIdx);
            batch.add(t);
            indices[i] = selectedIdx;

            // Calculate importance sampling weight
            float prob = priorities[selectedIdx];
            weights[i] = (float) Math.pow(buffer.size() * prob, -beta);
        }

        // Normalize weights
        float maxWeight = 0;
        for (float w : weights) {
            maxWeight = Math.max(maxWeight, w);
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= maxWeight;
        }

        // Anneal beta
        beta = Math.min(1.0f, beta + betaIncrement);

        return new SampleResult(batch, weights, indices);
    }

    public void updatePriorities(int[] indices, float[] tdErrors) {
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < buffer.size()) {
                buffer.get(indices[i]).priority = Math.abs(tdErrors[i]);
            }
        }
    }

    public int size() {
        return buffer.size();
    }

    public void clear() {
        buffer.clear();
    }

    public static class SampleResult {
        public List<PrioritizedTransition> batch;
        public float[] weights;
        public int[] indices;

        public SampleResult(List<PrioritizedTransition> batch, float[] weights, int[] indices) {
            this.batch = batch;
            this.weights = weights;
            this.indices = indices;
        }
    }
}