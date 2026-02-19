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

/**
 * Double Deep Q-Network (DDQN) Agent
 * Reduces overestimation bias by decoupling action selection and evaluation
 */
public class DDQNAgent extends DQNAgent {
    private Model targetModel;
    private Trainer targetTrainer;
    private int updateTargetFreq = 100;
    private int stepCount = 0;

    public DDQNAgent(NDManager m, int stateSize, int actionSize) {
        super(m, stateSize, actionSize);
        buildTargetNetwork();
    }

    private void buildTargetNetwork() {
        SequentialBlock targetNet = new SequentialBlock()
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(128).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(actionSize).build());

        targetModel = Model.newInstance("ddqn-target");
        targetModel.setBlock(targetNet);

        DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.l2Loss())
                .optOptimizer(Adam.builder()
                        .optLearningRateTracker(Tracker.fixed(0.0005f))
                        .build());

        targetTrainer = targetModel.newTrainer(config);
        targetTrainer.initialize(new ai.djl.ndarray.types.Shape(1, stateSize));

        // Initialize target network with same weights
        updateTargetNetwork();
    }

    @Override
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

            // Double DQN: Use online network to select actions
            NDArray onlineNextQ = trainer.forward(new NDList(nextStatesBatch)).singletonOrThrow();
            NDArray bestActions = onlineNextQ.argMax(1);

            // Use target network to evaluate actions
            NDArray targetNextQ = targetTrainer.forward(new NDList(nextStatesBatch)).singletonOrThrow();
            NDArray maxNextQ = targetNextQ.gather(
                    bestActions.reshape(new ai.djl.ndarray.types.Shape(batchSize, 1)), 1
            ).squeeze(1);

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

        stepCount++;
        if (stepCount % updateTargetFreq == 0) {
            updateTargetNetwork();
        }
    }

    private void updateTargetNetwork() {
        // Copy weights from online network to target network
        ai.djl.util.PairList<String, ai.djl.nn.Parameter> params =
                trainer.getModel().getBlock().getParameters();
        ai.djl.util.PairList<String, ai.djl.nn.Parameter> targetParams =
                targetTrainer.getModel().getBlock().getParameters();

        for (int i = 0; i < params.size(); i++) {
            NDArray sourceArray = params.valueAt(i).getArray();
            NDArray targetArray = targetParams.valueAt(i).getArray();
            targetArray.set(sourceArray.toFloatArray());
        }
    }

    @Override
    public void close() {
        super.close();
        targetModel.close();
    }

    @Override
    public String getName() {
        return "DDQN";
    }
}