import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;

/**
 * Prioritized Experience Replay DQN Agent
 * Samples important experiences more frequently
 */
public class PrioritizedDQNAgent extends DQNAgent {

    public PrioritizedDQNAgent(NDManager m, int stateSize, int actionSize) {
        super(m, stateSize, actionSize);
    }

    public void train(PrioritizedReplayBuffer buffer, int batchSize, float gamma) {
        if (buffer.size() < batchSize) return;

        PrioritizedReplayBuffer.SampleResult sample = buffer.sample(batchSize);
        float[][] states = new float[batchSize][stateSize];
        float[][] nextStates = new float[batchSize][stateSize];
        long[] actions = new long[batchSize];
        float[] rewards = new float[batchSize];
        boolean[] dones = new boolean[batchSize];

        for (int i = 0; i < batchSize; i++) {
            PrioritizedReplayBuffer.PrioritizedTransition t = sample.batch.get(i);
            states[i] = t.state;
            nextStates[i] = t.nextState;
            actions[i] = t.action;
            rewards[i] = t.reward;
            dones[i] = t.done;
        }

        float[] tdErrors = new float[batchSize];

        try (NDManager subManager = manager.newSubManager()) {
            NDArray statesBatch = subManager.create(states);
            NDArray actionsBatch = subManager.create(actions);
            NDArray rewardsBatch = subManager.create(rewards);
            NDArray donesBatch = subManager.create(dones);
            NDArray nextStatesBatch = subManager.create(nextStates);
            NDArray weightsBatch = subManager.create(sample.weights);

            NDArray nextQ = trainer.forward(new NDList(nextStatesBatch)).singletonOrThrow();
            NDArray maxNextQ = nextQ.max(new int[]{1});
            NDArray future = donesBatch.logicalNot().mul(gamma).mul(maxNextQ);
            NDArray targetValues = rewardsBatch.add(future);

            try (ai.djl.training.GradientCollector gc = trainer.newGradientCollector()) {
                NDArray predictions = trainer.forward(new NDList(statesBatch)).singletonOrThrow();
                NDArray currentSelected = predictions.gather(
                        actionsBatch.reshape(new ai.djl.ndarray.types.Shape(batchSize, 1)), 1
                ).squeeze(1);

                // Calculate TD errors
                NDArray errors = targetValues.sub(currentSelected);
                tdErrors = errors.toFloatArray();

                // Weighted loss for importance sampling
                NDArray weightedErrors = errors.mul(weightsBatch);
                NDArray lossValue = weightedErrors.square().mean();

                gc.backward(lossValue);
            }
            trainer.step();
        }

        // Update priorities based on TD errors
        buffer.updatePriorities(sample.indices, tdErrors);
    }

    @Override
    public String getName() {
        return "Prioritized-DQN";
    }
}