import java.util.ArrayList;
import java.util.List;
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
 * Episode Buffer
 * Used by on-policy algorithms (PPO, A2C)
 */
public class EpisodeBuffer {
    public List<float[]> states = new ArrayList<>();
    public List<Integer> actions = new ArrayList<>();
    public List<Float> rewards = new ArrayList<>();
    public List<Float> values = new ArrayList<>();
    public List<Float> logProbs = new ArrayList<>();

    public void add(float[] state, int action, float reward, float value, float logProb) {
        states.add(state);
        actions.add(action);
        rewards.add(reward);
        values.add(value);
        logProbs.add(logProb);
    }

    public void clear() {
        states.clear();
        actions.clear();
        rewards.clear();
        values.clear();
        logProbs.clear();
    }

    public int size() {
        return states.size();
    }
}
