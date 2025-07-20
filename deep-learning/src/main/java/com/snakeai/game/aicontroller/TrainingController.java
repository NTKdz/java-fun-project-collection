package com.snakeai.game.aicontroller;

import com.snakeai.game.SnakeGame;
import java.io.IOException;

public class TrainingController {
    public void train(String modelName) {
        Agent agent = new Agent(modelName);
        SnakeGame game = new SnakeGame();
        int record = 0;

        while (true) {
            // Get old state
            var oldState = game.getState();

            // Get move
            int[] finalMove = agent.getAction(oldState, true);

            // Perform move and get new state
            int reward = game.playStep(finalMove);
            var newState = game.getState();
            boolean done = game.isGameOver();

            // Train short memory
            agent.trainShortMemory(new ReplayMemory.Experience(oldState, finalMove, reward, newState, done));

            // Remember
            agent.remember(oldState, finalMove, reward, newState, done);

            if (done) {
                // Train long memory (experience replay)
                game.reset();
                agent.incrementGames();
                agent.trainLongMemory();

                if (game.getScore() > record) {
                    record = game.getScore();
                    try {
                        agent.saveModel();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.printf("Game: %d, Score: %d, Record: %d\n", agent.getNGames(), game.getScore(), record);
            }
        }
    }
}