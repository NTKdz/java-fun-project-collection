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

import java.util.Random; /**
 * Proximal Policy Optimization (PPO) Agent
 */
public class PPOAgent {
    protected NDManager manager;
    protected Model actorModel, criticModel;
    protected Trainer actorTrainer, criticTrainer;
    protected int stateSize, actionSize;
    protected Random rand = new Random();
    protected float clipEpsilon = 0.2f;
    protected float gamma = 0.95f;
    protected float lambda = 0.95f;

    public PPOAgent(NDManager m, int stateSize, int actionSize) {
        this.manager = m;
        this.stateSize = stateSize;
        this.actionSize = actionSize;
        buildNetworks();
    }

    protected void buildNetworks() {
        // Actor network
        SequentialBlock actorNet = new SequentialBlock()
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(128).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(actionSize).build());

        actorModel = Model.newInstance("ppo-actor");
        actorModel.setBlock(actorNet);

        DefaultTrainingConfig actorConfig = new DefaultTrainingConfig(Loss.l2Loss())
                .optOptimizer(Adam.builder()
                        .optLearningRateTracker(Tracker.fixed(0.0003f))
                        .build());

        actorTrainer = actorModel.newTrainer(actorConfig);
        actorTrainer.initialize(new ai.djl.ndarray.types.Shape(1, stateSize));

        // Critic network
        SequentialBlock criticNet = new SequentialBlock()
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(128).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(1).build());

        criticModel = Model.newInstance("ppo-critic");
        criticModel.setBlock(criticNet);

        DefaultTrainingConfig criticConfig = new DefaultTrainingConfig(Loss.l2Loss())
                .optOptimizer(Adam.builder()
                        .optLearningRateTracker(Tracker.fixed(0.001f))
                        .build());

        criticTrainer = criticModel.newTrainer(criticConfig);
        criticTrainer.initialize(new ai.djl.ndarray.types.Shape(1, stateSize));
    }

    public ActionResult selectAction(float[] state) {
        try (NDManager subManager = manager.newSubManager()) {
            NDArray s = subManager.create(state).reshape(1, -1);
            NDArray logits = actorTrainer.forward(new NDList(s)).singletonOrThrow();
            NDArray probs = logits.softmax(1);

            float[] probsArray = probs.toFloatArray();
            int action = sampleAction(probsArray);
            float logProb = (float) Math.log(probsArray[action] + 1e-8);

            return new ActionResult(action, logProb);
        }
    }

    protected int sampleAction(float[] probs) {
        float r = rand.nextFloat();
        float cumSum = 0;
        for (int i = 0; i < probs.length; i++) {
            cumSum += probs[i];
            if (r < cumSum) return i;
        }
        return probs.length - 1;
    }

    public float getValue(float[] state) {
        try (NDManager subManager = manager.newSubManager()) {
            NDArray s = subManager.create(state).reshape(1, -1);
            NDArray value = criticTrainer.forward(new NDList(s)).singletonOrThrow();
            return value.getFloat();
        }
    }

    public void train(EpisodeBuffer buffer, int epochs) {
        if (buffer.size() == 0) return;

        float[] advantages = calculateGAE(buffer);
        float[] returns = new float[buffer.size()];
        for (int i = 0; i < buffer.size(); i++) {
            returns[i] = advantages[i] + buffer.values.get(i);
        }

        // Normalize advantages
        float mean = 0, std = 0;
        for (float a : advantages) mean += a;
        mean /= advantages.length;
        for (float a : advantages) std += (a - mean) * (a - mean);
        std = (float) Math.sqrt(std / advantages.length + 1e-8);
        for (int i = 0; i < advantages.length; i++) {
            advantages[i] = (advantages[i] - mean) / std;
        }

        for (int epoch = 0; epoch < epochs; epoch++) {
            updatePolicy(buffer, advantages, returns);
        }
    }

    protected float[] calculateGAE(EpisodeBuffer buffer) {
        int T = buffer.size();
        float[] advantages = new float[T];
        float gae = 0;

        for (int t = T - 1; t >= 0; t--) {
            float delta = (t == T - 1)
                    ? buffer.rewards.get(t) - buffer.values.get(t)
                    : buffer.rewards.get(t) + gamma * buffer.values.get(t + 1) - buffer.values.get(t);
            gae = delta + gamma * lambda * gae;
            advantages[t] = gae;
        }

        return advantages;
    }

    protected void updatePolicy(EpisodeBuffer buffer, float[] advantages, float[] returns) {
        try (NDManager subManager = manager.newSubManager()) {
            int batchSize = buffer.size();

            float[][] statesArray = buffer.states.toArray(new float[0][]);
            NDArray states = subManager.create(statesArray);
            NDArray advArray = subManager.create(advantages);
            NDArray returnsArray = subManager.create(returns);

            long[] actionsArray = new long[batchSize];
            float[] oldLogProbs = new float[batchSize];
            for (int i = 0; i < batchSize; i++) {
                actionsArray[i] = buffer.actions.get(i);
                oldLogProbs[i] = buffer.logProbs.get(i);
            }
            NDArray actions = subManager.create(actionsArray);
            NDArray oldLogProbsArray = subManager.create(oldLogProbs);

            // Update actor
            try (ai.djl.training.GradientCollector gc = actorTrainer.newGradientCollector()) {
                NDArray logits = actorTrainer.forward(new NDList(states)).singletonOrThrow();
                NDArray probs = logits.softmax(1);
                NDArray selectedProbs = probs.gather(actions.reshape(batchSize, 1), 1).squeeze(1);
                NDArray newLogProbs = selectedProbs.add(1e-8).log();

                NDArray ratio = (newLogProbs.sub(oldLogProbsArray)).exp();
                NDArray clipRatio = ratio.clip(1 - clipEpsilon, 1 + clipEpsilon);

                NDArray loss1 = ratio.mul(advArray);
                NDArray loss2 = clipRatio.mul(advArray);
                NDArray policyLoss = loss1.minimum(loss2).mean().neg();

                gc.backward(policyLoss);
            }
            actorTrainer.step();

            // Update critic
            try (ai.djl.training.GradientCollector gc = criticTrainer.newGradientCollector()) {
                NDArray predictedValues = criticTrainer.forward(new NDList(states)).singletonOrThrow().squeeze();
                NDArray valueLoss = criticTrainer.getLoss().evaluate(
                        new NDList(returnsArray), new NDList(predictedValues)
                );
                gc.backward(valueLoss);
            }
            criticTrainer.step();
        }
    }

    public void close() {
        actorModel.close();
        criticModel.close();
    }

    public String getName() {
        return "PPO";
    }

    // Helper class
    public static class ActionResult {
        public int action;
        public float logProb;

        public ActionResult(int action, float logProb) {
            this.action = action;
            this.logProb = logProb;
        }
    }
}
