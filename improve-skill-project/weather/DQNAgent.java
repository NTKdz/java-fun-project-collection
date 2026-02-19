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

import java.util.List;
import java.util.Random;

/**
 * Deep Q-Network (DQN) Agent
 */
public class DQNAgent {
    protected NDManager manager;
    protected Model model;
    protected Trainer trainer;
    protected int stateSize, actionSize;
    protected Random rand = new Random();
    protected float epsilon = 1.0f;
    protected float epsilonMin = 0.01f;
    protected float epsilonDecay = 0.995f;

    public DQNAgent(NDManager m, int stateSize, int actionSize) {
        this.manager = m;
        this.stateSize = stateSize;
        this.actionSize = actionSize;
        buildNetwork();
    }

    protected void buildNetwork() {
        SequentialBlock net = new SequentialBlock()
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(128).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(actionSize).build());

        model = Model.newInstance("dqn");
        model.setBlock(net);

        DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.l2Loss())
                .optOptimizer(Adam.builder()
                        .optLearningRateTracker(Tracker.fixed(0.0005f))
                        .build());

        trainer = model.newTrainer(config);
        trainer.initialize(new ai.djl.ndarray.types.Shape(1, stateSize));
    }

    public int selectAction(float[] state) {
        if (rand.nextFloat() < epsilon) {
            return rand.nextInt(actionSize);
        }
        try (NDManager subManager = manager.newSubManager()) {
            NDArray s = subManager.create(state).reshape(1, -1);
            NDArray q = trainer.forward(new NDList(s)).singletonOrThrow();
            return (int) q.argMax(1).getLong();
        }
    }

    public void train(ReplayBuffer buffer, int batchSize, float gamma) {
        if (buffer.size() < batchSize) return;

        List<ReplayBuffer.Transition> batch = buffer.sample(batchSize);
        float[][] states = new float[batchSize][stateSize];
        float[][] nextStates = new float[batchSize][stateSize];
        long[] actions = new long[batchSize];
        float[] rewards = new float[batchSize];
        boolean[] dones = new boolean[batchSize];

        for (int i = 0; i < batchSize; i++) {
            ReplayBuffer.Transition t = batch.get(i);
            states[i] = t.state;
            nextStates[i] = t.nextState;
            actions[i] = t.action;
            rewards[i] = t.reward;
            dones[i] = t.done;
        }

        try (NDManager subManager = manager.newSubManager()) {
            NDArray statesBatch = subManager.create(states);
            NDArray actionsBatch = subManager.create(actions);
            NDArray rewardsBatch = subManager.create(rewards);
            NDArray donesBatch = subManager.create(dones);
            NDArray nextStatesBatch = subManager.create(nextStates);

            NDArray nextQ = trainer.forward(new NDList(nextStatesBatch)).singletonOrThrow();
            NDArray maxNextQ = nextQ.max(new int[]{1});
            NDArray future = donesBatch.logicalNot().mul(gamma).mul(maxNextQ);
            NDArray targetValues = rewardsBatch.add(future);

            try (ai.djl.training.GradientCollector gc = trainer.newGradientCollector()) {
                NDArray predictions = trainer.forward(new NDList(statesBatch)).singletonOrThrow();
                NDArray currentSelected = predictions.gather(
                        actionsBatch.reshape(new ai.djl.ndarray.types.Shape(batchSize, 1)), 1
                ).squeeze(1);
                NDArray lossValue = trainer.getLoss().evaluate(
                        new NDList(targetValues), new NDList(currentSelected)
                );
                gc.backward(lossValue);
            }
            trainer.step();
        }
    }

    public void decayEpsilon() {
        epsilon = Math.max(epsilonMin, epsilon * epsilonDecay);
    }

    public void close() {
        model.close();
    }

    public String getName() {
        return "DQN";
    }
}
