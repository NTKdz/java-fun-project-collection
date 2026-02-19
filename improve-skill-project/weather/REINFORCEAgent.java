import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.nn.Activation;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.Tracker;
import java.util.Random;

/**
 * REINFORCE Agent (Monte Carlo Policy Gradient)
 * Classic policy gradient algorithm - simple but high variance
 */
public class REINFORCEAgent {
    private NDManager manager;
    private Model policyModel;
    private Trainer policyTrainer;
    private int stateSize, actionSize;
    private Random rand = new Random();
    private float gamma = 0.99f;

    public REINFORCEAgent(NDManager m, int stateSize, int actionSize) {
        this.manager = m;
        this.stateSize = stateSize;
        this.actionSize = actionSize;
        buildNetwork();
    }

    private void buildNetwork() {
        SequentialBlock policyNet = new SequentialBlock()
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(128).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(actionSize).build());

        policyModel = Model.newInstance("reinforce");
        policyModel.setBlock(policyNet);

        DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.l2Loss())
                .optOptimizer(Adam.builder()
                        .optLearningRateTracker(Tracker.fixed(0.001f))
                        .build());

        policyTrainer = policyModel.newTrainer(config);
        policyTrainer.initialize(new ai.djl.ndarray.types.Shape(1, stateSize));
    }

    public ActionResult selectAction(float[] state) {
        try (NDManager subManager = manager.newSubManager()) {
            NDArray s = subManager.create(state).reshape(1, -1);
            NDArray logits = policyTrainer.forward(new NDList(s)).singletonOrThrow();
            NDArray probs = logits.softmax(1);

            float[] probsArray = probs.toFloatArray();
            int action = sampleAction(probsArray);
            float logProb = (float) Math.log(probsArray[action] + 1e-8);

            return new ActionResult(action, logProb);
        }
    }

    private int sampleAction(float[] probs) {
        float r = rand.nextFloat();
        float cumSum = 0;
        for (int i = 0; i < probs.length; i++) {
            cumSum += probs[i];
            if (r < cumSum) return i;
        }
        return probs.length - 1;
    }

    public void train(EpisodeBuffer buffer) {
        if (buffer.size() == 0) return;

        // Calculate returns (Monte Carlo)
        float[] returns = new float[buffer.size()];
        float G = 0;
        for (int t = buffer.size() - 1; t >= 0; t--) {
            G = buffer.rewards.get(t) + gamma * G;
            returns[t] = G;
        }

        // Normalize returns
        float mean = 0, std = 0;
        for (float r : returns) mean += r;
        mean /= returns.length;
        for (float r : returns) std += (r - mean) * (r - mean);
        std = (float) Math.sqrt(std / returns.length + 1e-8);
        for (int i = 0; i < returns.length; i++) {
            returns[i] = (returns[i] - mean) / std;
        }

        try (NDManager subManager = manager.newSubManager()) {
            int batchSize = buffer.size();
            float[][] statesArray = buffer.states.toArray(new float[0][]);
            NDArray states = subManager.create(statesArray);
            NDArray returnsArray = subManager.create(returns);

            long[] actionsArray = new long[batchSize];
            for (int i = 0; i < batchSize; i++) {
                actionsArray[i] = buffer.actions.get(i);
            }
            NDArray actions = subManager.create(actionsArray);

            // Policy gradient update
            try (ai.djl.training.GradientCollector gc = policyTrainer.newGradientCollector()) {
                NDArray logits = policyTrainer.forward(new NDList(states)).singletonOrThrow();
                NDArray probs = logits.softmax(1);
                NDArray selectedProbs = probs.gather(actions.reshape(batchSize, 1), 1).squeeze(1);
                NDArray logProbs = selectedProbs.add(1e-8).log();

                // Loss: -log(π(a|s)) * G
                NDArray loss = logProbs.mul(returnsArray).mean().neg();
                gc.backward(loss);
            }
            policyTrainer.step();
        }
    }

    public void close() {
        policyModel.close();
    }

    public String getName() {
        return "REINFORCE";
    }

    public static class ActionResult {
        public int action;
        public float logProb;

        public ActionResult(int action, float logProb) {
            this.action = action;
            this.logProb = logProb;
        }
    }
}