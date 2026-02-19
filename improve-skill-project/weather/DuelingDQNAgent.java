import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.nn.Activation;
import ai.djl.nn.Block;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.Tracker;
import ai.djl.util.PairList;

/**
 * Dueling DQN Agent
 * Separates value and advantage streams for better learning
 */
public class DuelingDQNAgent extends DQNAgent {

    public DuelingDQNAgent(NDManager m, int stateSize, int actionSize) {
        super(m, stateSize, actionSize);
    }

    @Override
    protected void buildNetwork() {
        // Shared feature extractor
        SequentialBlock sharedLayers = new SequentialBlock()
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu);

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

        // Dueling architecture: combines value and advantage
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

                NDArray value = valueStream.forward(
                        ps, new NDList(features), training
                ).singletonOrThrow();

                NDArray advantages = advantageStream.forward(
                        ps, new NDList(features), training
                ).singletonOrThrow();

                // Q(s,a) = V(s) + (A(s,a) - mean(A(s,a)))
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
                // Initialize shared layers first
                sharedLayers.initialize(manager, dataType, inputShapes);

                // Get the output shape from shared layers
                ai.djl.ndarray.types.Shape featureShape =
                        sharedLayers.getOutputShapes(inputShapes)[0];

                // Initialize value and advantage streams with feature shape
                valueStream.initialize(manager, dataType, featureShape);
                advantageStream.initialize(manager, dataType, featureShape);
            }
        };

        model = Model.newInstance("dueling-dqn");
        model.setBlock(duelingBlock);

        DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.l2Loss())
                .optOptimizer(Adam.builder()
                        .optLearningRateTracker(Tracker.fixed(0.0005f))
                        .build());

        trainer = model.newTrainer(config);
        trainer.initialize(new ai.djl.ndarray.types.Shape(1, stateSize));
    }

    @Override
    public String getName() {
        return "Dueling-DQN";
    }
}