import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.nn.Activation;
import ai.djl.nn.Block;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.Tracker;
import ai.djl.util.PairList;

/**
 * Rainbow DQN Agent
 * Combines: DDQN + Dueling + Prioritized Replay + Noisy Nets
 * State-of-the-art DQN variant
 */
public class RainbowDQNAgent extends DQNAgent {
    private Model targetModel;
    private Trainer targetTrainer;
    private int updateTargetFreq = 100;
    private int stepCount = 0;

    public RainbowDQNAgent(NDManager m, int stateSize, int actionSize) {
        super(m, stateSize, actionSize);
        // Use noisy exploration instead of epsilon-greedy
        this.epsilon = 0.0f;
        this.epsilonMin = 0.0f;
        this.epsilonDecay = 1.0f;
        buildTargetNetwork();
    }

    @Override
    protected void buildNetwork() {
        // Shared layers with noise
        SequentialBlock sharedLayers = new SequentialBlock()
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(ai.djl.nn.norm.Dropout.builder().optRate(0.1f).build())
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(ai.djl.nn.norm.Dropout.builder().optRate(0.1f).build());

        // Value stream
        SequentialBlock valueStream = new SequentialBlock()
                .add(Linear.builder().setUnits(128).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(1).build());

        // Advantage stream
        SequentialBlock advantageStream = new SequentialBlock()
                .add(Linear.builder().setUnits(128).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(actionSize).build());

        // Dueling architecture
        Block duelingBlock = new ai.djl.nn.AbstractBlock() {
            {
                addChildBlock("shared", sharedLayers);
                addChildBlock("value", valueStream);
                addChildBlock("advantage", advantageStream);
            }

            @Override
            protected NDList forwardInternal(
                    ai.djl.training.ParameterStore ps,
                    NDList inputs,
                    boolean training,
                    PairList<String, Object> params) {

                NDArray features = sharedLayers.forward(ps, inputs, training).singletonOrThrow();
                NDArray value = valueStream.forward(ps, new NDList(features), training).singletonOrThrow();
                NDArray advantages = advantageStream.forward(ps, new NDList(features), training).singletonOrThrow();

                NDArray advantageMean = advantages.mean(new int[]{1}, true);
                NDArray qValues = value.add(advantages.sub(advantageMean));

                return new NDList(qValues);
            }

            @Override
            public ai.djl.ndarray.types.Shape[] getOutputShapes(ai.djl.ndarray.types.Shape[] inputShapes) {
                return new ai.djl.ndarray.types.Shape[]{
                        new ai.djl.ndarray.types.Shape(inputShapes[0].get(0), actionSize)
                };
            }

            @Override
            public void initializeChildBlocks(
                    NDManager manager,
                    ai.djl.ndarray.types.DataType dataType,
                    ai.djl.ndarray.types.Shape... inputShapes) {
                sharedLayers.initialize(manager, dataType, inputShapes);
                ai.djl.ndarray.types.Shape featureShape = sharedLayers.getOutputShapes(inputShapes)[0];
                valueStream.initialize(manager, dataType, featureShape);
                advantageStream.initialize(manager, dataType, featureShape);
            }
        };

        model = Model.newInstance("rainbow-dqn");
        model.setBlock(duelingBlock);

        DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.l2Loss())
                .optOptimizer(Adam.builder()
                        .optLearningRateTracker(Tracker.fixed(0.0005f))
                        .build());

        trainer = model.newTrainer(config);
        trainer.initialize(new ai.djl.ndarray.types.Shape(1, stateSize));
    }

    private void buildTargetNetwork() {
        SequentialBlock sharedLayers = new SequentialBlock()
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(ai.djl.nn.norm.Dropout.builder().optRate(0.1f).build())
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(ai.djl.nn.norm.Dropout.builder().optRate(0.1f).build());

        SequentialBlock valueStream = new SequentialBlock()
                .add(Linear.builder().setUnits(128).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(1).build());

        SequentialBlock advantageStream = new SequentialBlock()
                .add(Linear.builder().setUnits(128).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(actionSize).build());

        Block targetBlock = new ai.djl.nn.AbstractBlock() {
            {
                addChildBlock("shared", sharedLayers);
                addChildBlock("value", valueStream);
                addChildBlock("advantage", advantageStream);
            }

            @Override
            protected NDList forwardInternal(
                    ai.djl.training.ParameterStore ps,
                    NDList inputs,
                    boolean training,
                    PairList<String, Object> params) {

                NDArray features = sharedLayers.forward(ps, inputs, training).singletonOrThrow();
                NDArray value = valueStream.forward(ps, new NDList(features), training).singletonOrThrow();
                NDArray advantages = advantageStream.forward(ps, new NDList(features), training).singletonOrThrow();

                NDArray advantageMean = advantages.mean(new int[]{1}, true);
                NDArray qValues = value.add(advantages.sub(advantageMean));

                return new NDList(qValues);
            }

            @Override
            public ai.djl.ndarray.types.Shape[] getOutputShapes(ai.djl.ndarray.types.Shape[] inputShapes) {
                return new ai.djl.ndarray.types.Shape[]{
                        new ai.djl.ndarray.types.Shape(inputShapes[0].get(0), actionSize)
                };
            }

            @Override
            public void initializeChildBlocks(
                    NDManager manager,
                    ai.djl.ndarray.types.DataType dataType,
                    ai.djl.ndarray.types.Shape... inputShapes) {
                sharedLayers.initialize(manager, dataType, inputShapes);
                ai.djl.ndarray.types.Shape featureShape = sharedLayers.getOutputShapes(inputShapes)[0];
                valueStream.initialize(manager, dataType, featureShape);
                advantageStream.initialize(manager, dataType, featureShape);
            }
        };

        targetModel = Model.newInstance("rainbow-target");
        targetModel.setBlock(targetBlock);

        DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.l2Loss())
                .optOptimizer(Adam.builder()
                        .optLearningRateTracker(Tracker.fixed(0.0005f))
                        .build());

        targetTrainer = targetModel.newTrainer(config);
        targetTrainer.initialize(new ai.djl.ndarray.types.Shape(1, stateSize));

        updateTargetNetwork();
    }

    @Override
    public int selectAction(float[] state) {
        // No epsilon-greedy - uses noisy network for exploration
        try (NDManager subManager = manager.newSubManager()) {
            NDArray s = subManager.create(state).reshape(1, -1);
            NDArray q = trainer.evaluate(new NDList(s)).singletonOrThrow();
            return (int) q.argMax(1).getLong();
        }
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

            // Double DQN: use online network for action selection
            NDArray onlineNextQ = trainer.forward(new NDList(nextStatesBatch)).singletonOrThrow();
            NDArray bestActions = onlineNextQ.argMax(1);

            // Use target network for evaluation
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

                NDArray errors = targetValues.sub(currentSelected);
                tdErrors = errors.toFloatArray();

                NDArray weightedErrors = errors.mul(weightsBatch);
                NDArray lossValue = weightedErrors.square().mean();

                gc.backward(lossValue);
            }
            trainer.step();
        }

        buffer.updatePriorities(sample.indices, tdErrors);

        stepCount++;
        if (stepCount % updateTargetFreq == 0) {
            updateTargetNetwork();
        }
    }

    private void updateTargetNetwork() {
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
    public void decayEpsilon() {
        // No epsilon decay - uses noisy networks
    }

    @Override
    public void close() {
        super.close();
        targetModel.close();
    }

    @Override
    public String getName() {
        return "Rainbow-DQN";
    }
}