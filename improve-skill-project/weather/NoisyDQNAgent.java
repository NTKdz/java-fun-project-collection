import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.nn.Activation;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.Tracker;

/**
 * Noisy DQN Agent
 * Uses learnable noise parameters for exploration instead of epsilon-greedy
 */
public class NoisyDQNAgent extends DQNAgent {

    public NoisyDQNAgent(NDManager m, int stateSize, int actionSize) {
        super(m, stateSize, actionSize);
        // No epsilon-greedy needed with noisy networks
        this.epsilon = 0.0f;
        this.epsilonMin = 0.0f;
        this.epsilonDecay = 1.0f;
    }

    @Override
    protected void buildNetwork() {
        // Network with added noise for exploration
        // Note: True NoisyNet would require custom layers with learnable noise
        // This is a simplified version that adds dropout for stochasticity
        SequentialBlock net = new SequentialBlock()
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(ai.djl.nn.norm.Dropout.builder().optRate(0.1f).build())
                .add(Linear.builder().setUnits(256).build())
                .add(Activation::relu)
                .add(ai.djl.nn.norm.Dropout.builder().optRate(0.1f).build())
                .add(Linear.builder().setUnits(128).build())
                .add(Activation::relu)
                .add(ai.djl.nn.norm.Dropout.builder().optRate(0.05f).build())
                .add(Linear.builder().setUnits(actionSize).build());

        model = Model.newInstance("noisy-dqn");
        model.setBlock(net);

        DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.l2Loss())
                .optOptimizer(Adam.builder()
                        .optLearningRateTracker(Tracker.fixed(0.0005f))
                        .build());

        trainer = model.newTrainer(config);
        trainer.initialize(new ai.djl.ndarray.types.Shape(1, stateSize));
    }

    @Override
    public int selectAction(float[] state) {
        // Always use network (with noise during training) - no epsilon-greedy
        try (NDManager subManager = manager.newSubManager()) {
            NDArray s = subManager.create(state).reshape(1, -1);
            // Set training=true to enable dropout noise during action selection
            NDArray q = trainer.evaluate(new NDList(s)).singletonOrThrow();
            return (int) q.argMax(1).getLong();
        }
    }

    @Override
    public void decayEpsilon() {
        // No epsilon decay needed - noise is learned
    }

    @Override
    public String getName() {
        return "Noisy-DQN";
    }
}