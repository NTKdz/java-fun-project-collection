package com.snakeai.game;

import com.snakeai.game.aicontroller.Agent;
import com.snakeai.game.aicontroller.TrainingController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
//        if (args.length == 0) {
//            System.out.println("Usage: java -jar snake-ai.jar <train|test> <model_name>");
//            System.out.println("Available model names: model_simple, model_wide, model_deep");
//            return;
//        }

        String mode = "train";
        String modelName = args.length > 1 ? args[1] : "model_wide";

        if ("train".equalsIgnoreCase(mode)) {
            System.out.println("Starting training for " + modelName + "...");
            new TrainingController().train(modelName);
        } else if ("test".equalsIgnoreCase(mode)) {
            System.out.println("Starting testing for " + modelName + "...");
            test(modelName);
        } else {
            System.out.println("Invalid mode. Use 'train' or 'test'.");
        }
    }

    private static void test(String modelName) {
        SnakeGame game = new SnakeGame();
        Agent agent = new Agent(modelName);

        try {
            agent.loadModel();
        } catch (IOException e) {
            System.err.println("Could not load model: " + modelName + ".zip. Make sure it's trained and in the root directory.");
            e.printStackTrace();
            return;
        }

        JFrame frame = new JFrame("Snake AI - Test Mode");
        GamePanel gamePanel = new GamePanel(game);
        frame.add(gamePanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Slower timer for visualization
        Timer timer = new Timer(100, e -> {
            if (!game.isGameOver()) {
                var state = game.getState();
                int[] action = agent.getAction(state, false); // a isTraining=false
                game.playStep(action);
                gamePanel.repaint();
            } else {
                System.out.println("Game Over! Final Score: " + game.getScore());
                // Optionally restart
                // game.reset();
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
    }
}