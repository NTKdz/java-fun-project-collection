import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
/**
 * Experience Replay Buffer
 * Used by off-policy algorithms (DQN, DDQN, Dueling DQN)
 */
public class ReplayBuffer {

    public static class Transition {
        public float[] state;
        public int action;
        public float reward;
        public float[] nextState;
        public boolean done;

        public Transition(float[] state, int action, float reward, float[] nextState, boolean done) {
            this.state = state;
            this.action = action;
            this.reward = reward;
            this.nextState = nextState;
            this.done = done;
        }
    }

    private ArrayDeque<Transition> buffer = new ArrayDeque<>();
    private int maxSize;
    private Random rand = new Random();

    public ReplayBuffer(int maxSize) {
        this.maxSize = maxSize;
    }

    public void add(float[] state, int action, float reward, float[] nextState, boolean done) {
        if (buffer.size() >= maxSize) {
            buffer.removeFirst();
        }
        buffer.addLast(new Transition(state, action, reward, nextState, done));
    }

    public List<Transition> sample(int batchSize) {
        List<Transition> bufferList = new ArrayList<>(buffer);
        List<Transition> batch = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            batch.add(bufferList.get(rand.nextInt(bufferList.size())));
        }
        return batch;
    }

    public int size() {
        return buffer.size();
    }

    public void clear() {
        buffer.clear();
    }
}

