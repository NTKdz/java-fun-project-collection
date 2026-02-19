import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;

/**
 * SARSA Agent (State-Action-Reward-State-Action)
 * On-policy TD learning - more conservative than Q-learning
 */
public class SARSAAgent extends DQNAgent {
    private int lastAction = -1;
    private float[] lastState = null;

    public SARSAAgent(NDManager m, int stateSize, int actionSize) {
        super(m, stateSize, actionSize);
    }

    public void trainStep(float[] state, int action, float reward,
                          float[] nextState, int nextAction, boolean done) {
        try (NDManager subManager = manager.newSubManager()) {
            NDArray stateBatch = subManager.create(state).reshape(1, -1);
            NDArray nextStateBatch = subManager.create(nextState).reshape(1, -1);

            // SARSA uses the actual next action taken (on-policy)
            NDArray nextQ = trainer.forward(new NDList(nextStateBatch)).singletonOrThrow();
            float nextQValue = done ? 0 : nextQ.getFloat(0, nextAction);
            float target = reward + 0.95f * nextQValue;

            try (ai.djl.training.GradientCollector gc = trainer.newGradientCollector()) {
                NDArray predictions = trainer.forward(new NDList(stateBatch)).singletonOrThrow();

                // Extract the Q-value for the taken action as NDArray
                NDArray actionIndex = subManager.create(new long[]{action});
                NDArray currentQ = predictions.get(0).get(actionIndex).reshape(1);

                // Create target as NDArray
                NDArray targetArray = subManager.create(new float[]{target});

                NDArray lossValue = trainer.getLoss().evaluate(
                        new NDList(targetArray), new NDList(currentQ)
                );
                gc.backward(lossValue);
            }
            trainer.step();
        }
    }

    public int selectActionAndTrain(float[] state, float reward, boolean done) {
        int action = selectAction(state);

        if (lastState != null && lastAction != -1) {
            trainStep(lastState, lastAction, reward, state, action, done);
        }

        lastState = state.clone();
        lastAction = action;

        return action;
    }

    public void reset() {
        lastState = null;
        lastAction = -1;
    }

    @Override
    public String getName() {
        return "SARSA";
    }
}