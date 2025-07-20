// src/Minesweeper.java

import javax.swing.*;
import java.io.File;

public class Minesweeper {

    // --- Configuration ---
    private static final int BOARD_WIDTH = 6;
    private static final int BOARD_HEIGHT = 6;
    private static final int NUM_MINES = BOARD_WIDTH * BOARD_HEIGHT / 8;

    // Training
    private static final int TRAINING_EPISODES = 1000000;

    // Testing
    private static final int TEST_EPISODES = 10;
    private static final int VISUAL_DELAY_MS = 200; // Delay between moves in visualization
    private static final int CELL_SIZE_PX = 40; // For Swing GUI

    // Agent Hyperparameters
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.9;
    private static final double INITIAL_EPSILON = 1.0;

    // File to save/load the Q-Table
    private static final String Q_TABLE_FILE = "qtable.ser";

    public static void main(String[] args) {
        runTrainMode();
        runTestMode();
    }

    private static void runTrainMode() {
        System.out.println("--- Starting TRAINING Mode ---");
        QLearningAgent agent = new QLearningAgent(LEARNING_RATE, DISCOUNT_FACTOR, INITIAL_EPSILON);

        // Attempt to load a pre-existing Q-table to continue training
        agent.loadQTable(Q_TABLE_FILE);

        System.out.printf("Training on %dx%d board with %d mines for %d episodes...%n",
                BOARD_WIDTH, BOARD_HEIGHT, NUM_MINES, TRAINING_EPISODES);

        agent.train(TRAINING_EPISODES, BOARD_WIDTH, BOARD_HEIGHT, NUM_MINES);

        // Save the learned Q-table for future use
        agent.saveQTable(Q_TABLE_FILE);
    }

    private static void runTestMode() {
        System.out.println("--- Starting TEST Mode ---");
        if (!new File(Q_TABLE_FILE).exists()) {
            System.err.println("Error: Q-Table file '" + Q_TABLE_FILE + "' not found.");
            System.err.println("Please run in training mode first to generate the file.");
            return;
        }

        QLearningAgent agent = new QLearningAgent(LEARNING_RATE, DISCOUNT_FACTOR, INITIAL_EPSILON);
        agent.loadQTable(Q_TABLE_FILE);

        // Setup Swing GUI for visualization
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Minesweeper - Agent Testing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // The panel needs an initial board to calculate its size
            Board initialBoard = new Board(BOARD_WIDTH, BOARD_HEIGHT, NUM_MINES);
            BoardPanel boardPanel = new BoardPanel(initialBoard, CELL_SIZE_PX);
            frame.add(boardPanel);

            frame.pack();
            frame.setLocationRelativeTo(null); // Center on screen
            frame.setVisible(true);

            // Run the test in a new thread to avoid blocking the Swing EDT
            new Thread(() -> {
                agent.test(TEST_EPISODES, BOARD_WIDTH, BOARD_HEIGHT, NUM_MINES, boardPanel, VISUAL_DELAY_MS);
                System.out.println("Visualization complete. You can close the window.");
            }).start();
        });
    }
}