package com.snakeai.game.aicontroller;

import com.snakeai.game.SnakeGame;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class Agent {
    private static final int MEMORY_SIZE = 100_000;
    private static final int BATCH_SIZE = 1000;
    private static final double GAMMA = 0.9; // Discount rate

    private int nGames = 0;
    private double epsilon = 0; // Randomness
    private final ReplayMemory memory = new ReplayMemory(MEMORY_SIZE);
    private final DeepQNetwork model;
    private final Random random = new Random();

    public Agent(String modelName) {
        // Input size is 11, output size is 3 (straight, right, left)
        this.model = new DeepQNetwork(modelName, SnakeGame.STATE_SIZE, 3);
    }

    public void remember(INDArray state, int[] action, int reward, INDArray nextState, boolean done) {
        memory.push(state, action, reward, nextState, done);
    }

    public void trainLongMemory() {
        if (memory.size() < BATCH_SIZE) {
            return;
        }
        List<ReplayMemory.Experience> batch = memory.sample(BATCH_SIZE);
        trainOnBatch(batch);
    }

    public void trainShortMemory(ReplayMemory.Experience experience) {
        trainOnBatch(List.of(experience));
    }

    private void trainOnBatch(List<ReplayMemory.Experience> batch) {
        INDArray states = Nd4j.vstack(batch.stream().map(ReplayMemory.Experience::state).toArray(INDArray[]::new));
        INDArray nextStates = Nd4j.vstack(batch.stream().map(ReplayMemory.Experience::nextState).toArray(INDArray[]::new));

        INDArray qValues = model.predict(states);
        INDArray nextQValues = model.predict(nextStates);

        for (int i = 0; i < batch.size(); i++) {
            ReplayMemory.Experience exp = batch.get(i);
            double targetQ = exp.reward();
            if (!exp.done()) {
                // THE FIX IS HERE: Specify dimension 1 for the max operation.
                targetQ = exp.reward() + GAMMA * Transforms.max(nextQValues.getRow(i), 1).getDouble(0);
            }

            int actionIndex = getActionIndex(exp.action());
            if (actionIndex != -1) {
                // Use explicit coordinates for clarity and safety
                qValues.putScalar(new int[]{i, actionIndex}, targetQ);
            }
        }
        model.train(states, qValues);
    }

    public int[] getAction(INDArray state, boolean isTraining) {
        // Epsilon-greedy strategy for exploration/exploitation
        if (isTraining) {
            epsilon = 80 - nGames;
        } else {
            epsilon = -1; // No randomness in testing
        }

        int[] action = new int[3];
        if (random.nextInt(200) < epsilon) {
            int move = random.nextInt(3);
            action[move] = 1;
        } else {
            INDArray prediction = model.predict(state);
            int move = Nd4j.argMax(prediction, 1).getInt(0);
            action[move] = 1;
        }
        return action;
    }

    private int getActionIndex(int[] action) {
        for (int i = 0; i < action.length; i++) {
            if (action[i] == 1) return i;
        }
        return -1; // Should not happen
    }

    public void incrementGames() {
        this.nGames++;
    }

    public int getNGames() {
        return nGames;
    }

    public void saveModel() throws IOException {
        model.saveModel();
    }

    public void loadModel() throws IOException {
        model.loadModel();
    }
}