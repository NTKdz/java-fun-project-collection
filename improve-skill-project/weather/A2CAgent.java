import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Advantage Actor-Critic (A2C) Agent
 * Simpler and faster than PPO, but less stable
 */
public class A2CAgent extends PPOAgent {

    public A2CAgent(NDManager m, int stateSize, int actionSize) {
        super(m, stateSize, actionSize);
        // A2C doesn't use clipping
        this.clipEpsilon = Float.MAX_VALUE;
    }

    @Override
    protected void updatePolicy(EpisodeBuffer buffer, float[] advantages, float[] returns) {
        try (NDManager subManager = manager.newSubManager()) {
            int batchSize = buffer.size();

            float[][] statesArray = buffer.states.toArray(new float[0][]);
            NDArray states = subManager.create(statesArray);
            NDArray advArray = subManager.create(advantages);
            NDArray returnsArray = subManager.create(returns);

            long[] actionsArray = new long[batchSize];
            for (int i = 0; i < batchSize; i++) {
                actionsArray[i] = buffer.actions.get(i);
            }
            NDArray actions = subManager.create(actionsArray);

            // Update actor (simple policy gradient)
            try (ai.djl.training.GradientCollector gc = actorTrainer.newGradientCollector()) {
                NDArray logits = actorTrainer.forward(new NDList(states)).singletonOrThrow();
                NDArray probs = logits.softmax(1);
                NDArray selectedProbs = probs.gather(actions.reshape(batchSize, 1), 1).squeeze(1);
                NDArray logProbs = selectedProbs.add(1e-8).log();

                // Policy loss: -log(π(a|s)) * A(s,a)
                NDArray policyLoss = logProbs.mul(advArray).mean().neg();

                gc.backward(policyLoss);
            }
            actorTrainer.step();

            // Update critic (same as PPO)
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

    @Override
    public String getName() {
        return "A2C";
    }
}