import java.util.Arrays;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Env env = new Env();
        QLearning qLearning = new QLearning();
        Minimax minimax = new Minimax(2, 1, env.winCon, 3); // AI = 2, Player = 1

        int episodes = 1000;
        int[] state = env.reset();
        int actionSize = env.height * env.width;

        while (episodes > 0) {
            int action = qLearning.chooseAction(Arrays.toString(state), actionSize);
            int row = action / env.width;
            int col = action % env.width;

            Map<String, Object> res = env.step(row, col, 1);
            int[] nextState = (int[]) res.get("state");
            double reward = (double) res.get("reward");
            boolean done = (boolean) res.get("done");

            qLearning.updateQValue(Arrays.toString(state), action, reward, Arrays.toString(nextState), actionSize);
            qLearning.decayEpsilon();
            state = nextState;
            if (done) {
                printEnd(env, episodes);
                env.reset();
                episodes--;
            } else {
                int[] bestMove = minimax.findBestMove(env.board);
                if (bestMove[0] != -1 && bestMove[1] != -1) {
                    res = env.step(bestMove[0], bestMove[1], 2);
                    state = (int[]) res.get("state");
                    done = (boolean) res.get("done");
                }

                if (done) {
                    printEnd(env, episodes);
                    env.reset();
                    episodes--;

                }
            }
            env.printBoard();
        }
    }

    public static void printEnd(Env env, int episode) {
        System.out.println("episode: " + episode + ", Score: " + env.score);
        env.printBoard();
    }
}
