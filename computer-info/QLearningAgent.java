// src/QLearningAgent.java
import java.io.*;
import java.util.*;

public class QLearningAgent implements Serializable {
    private static final long serialVersionUID = 1L;

    // Hyperparameters
    private final double learningRate; // alpha
    private final double discountFactor; // gamma
    private double explorationRate; // epsilon
    private final double epsilonDecay;

    // Q-table: Map<State_String, Q_Value> - transient so it's not serialized with the agent object itself
    private Map<String, Double> qTable;
    private final Random rand;

    public QLearningAgent(double learningRate, double discountFactor, double explorationRate) {
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
        this.explorationRate = explorationRate;
        this.epsilonDecay = 0.9995;
        this.qTable = new HashMap<>();
        this.rand = new Random();
    }

    // --- Save and Load Q-Table ---

    public void saveQTable(String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(this.qTable);
            System.out.println("Q-Table successfully saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving Q-Table: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void loadQTable(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("No existing Q-Table found at " + filePath + ". Starting fresh.");
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            this.qTable = (Map<String, Double>) ois.readObject();
            System.out.println("Q-Table successfully loaded from " + filePath);
            System.out.println("Loaded Q-Table with " + this.qTable.size() + " states.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading Q-Table: " + e.getMessage());
        }
    }

    // --- State and Action Logic (Unchanged) ---

    public String getStateForCell(Board board, int r, int c) {
        List<Integer> neighbors = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr;
                int nc = c + dc;
                if (!board.isValid(nr, nc)) neighbors.add(-2); // Off-board
                else {
                    Cell neighborCell = board.getCell(nr, nc);
                    if (neighborCell.isRevealed) neighbors.add(neighborCell.adjacentMines);
                    else neighbors.add(-1); // Hidden
                }
            }
        }
        return getCanonicalState(neighbors);
    }

    private String getCanonicalState(List<Integer> neighbors) {
        String canonicalState = neighbors.toString();
        List<Integer> current = new ArrayList<>(neighbors);
        for(int i = 0; i < 4; i++) {
            current = rotate(current);
            String rotatedStr = current.toString();
            if (rotatedStr.compareTo(canonicalState) < 0) canonicalState = rotatedStr;
            String flippedStr = flip(current).toString();
            if (flippedStr.compareTo(canonicalState) < 0) canonicalState = flippedStr;
        }
        return canonicalState;
    }

    private List<Integer> rotate(List<Integer> n) { return List.of(n.get(5), n.get(3), n.get(0), n.get(6), n.get(1), n.get(7), n.get(4), n.get(2)); }
    private List<Integer> flip(List<Integer> n) { return List.of(n.get(2), n.get(1), n.get(0), n.get(4), n.get(3), n.get(7), n.get(6), n.get(5)); }

    public int[] chooseAction(Board board) {
        List<int[]> possibleActions = new ArrayList<>();
        for (int r = 0; r < board.getHeight(); r++) {
            for (int c = 0; c < board.getWidth(); c++) {
                if (!board.getCell(r, c).isRevealed) {
                    possibleActions.add(new int[]{r, c});
                }
            }
        }
        if (possibleActions.isEmpty()) return null;
        if (rand.nextDouble() < this.explorationRate) {
            return possibleActions.get(rand.nextInt(possibleActions.size()));
        } else {
            int[] bestAction = null;
            double maxQValue = Double.NEGATIVE_INFINITY;
            Collections.shuffle(possibleActions);
            for (int[] action : possibleActions) {
                String state = getStateForCell(board, action[0], action[1]);
                double qValue = qTable.getOrDefault(state, 0.0);
                if (qValue > maxQValue) {
                    maxQValue = qValue;
                    bestAction = action;
                }
            }
            return (bestAction != null) ? bestAction : possibleActions.get(0);
        }
    }

    // --- Training and Testing Logic ---

    public void updateQValue(String state, double reward, Board nextBoard) {
        double oldQValue = qTable.getOrDefault(state, 0.0);
        double maxNextQ = 0.0;
        if (nextBoard.getGameState() == GameState.IN_PROGRESS) {
            for (int r = 0; r < nextBoard.getHeight(); r++) {
                for (int c = 0; c < nextBoard.getWidth(); c++) {
                    if (!nextBoard.getCell(r, c).isRevealed) {
                        String nextState = getStateForCell(nextBoard, r, c);
                        maxNextQ = Math.max(maxNextQ, qTable.getOrDefault(nextState, 0.0));
                    }
                }
            }
        }
        double newQValue = oldQValue + learningRate * (reward + discountFactor * maxNextQ - oldQValue);
        qTable.put(state, newQValue);
    }

    public void train(int numEpisodes, int width, int height, int numMines) {
        int wins = 0;
        for (int i = 0; i < numEpisodes; i++) {
            Board board = new Board(width, height, numMines);
            while(board.getGameState() == GameState.IN_PROGRESS) {
                int[] action = chooseAction(board);
                if (action == null) break;
                String state = getStateForCell(board, action[0], action[1]);
                int revealedCount = board.revealCell(action[0], action[1]);
                double reward = 0;
                if (board.getGameState() == GameState.LOSS) reward = -100;
                else if (board.getGameState() == GameState.WIN) { reward = 200; wins++; }
                else reward = (revealedCount > 1) ? 5.0 * revealedCount : 1.0;
                updateQValue(state, reward, board);
            }
            explorationRate = Math.max(0.01, explorationRate * epsilonDecay);
            if ((i + 1) % 1000 == 0) {
                System.out.printf("Episode %d/%d - Win Rate: %.2f%% - Epsilon: %.4f - Q-Table size: %d%n",
                        i + 1, numEpisodes, (double)wins/10, explorationRate, qTable.size());
                wins = 0;
            }
        }
        System.out.println("Training finished.");
    }

    public void test(int numEpisodes, int width, int height, int numMines, BoardPanel panel, int delay) {
        System.out.println("\n--- Starting Test Phase ---");
        this.explorationRate = 0; // Exploit learned knowledge
        int wins = 0;
        int losses = 0;

        for (int i = 0; i < numEpisodes; i++) {
            Board board = new Board(width, height, numMines);
            if (panel != null) {
                panel.setBoard(board);
                panel.repaint();
            }

            while (board.getGameState() == GameState.IN_PROGRESS) {
                int[] action = chooseAction(board);
                if (action == null) break;

                board.revealCell(action[0], action[1]);

                if (panel != null) {
                    panel.repaint();
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            if (board.getGameState() == GameState.WIN) {
                wins++;
            } else {
                losses++;
            }
            System.out.printf("Test Game %d/%d: %s\n", i + 1, numEpisodes, board.getGameState());
        }
        double winRate = (double) wins / (wins + losses) * 100;
        System.out.printf("\n--- Test Results --- \nEpisodes: %d\nWins: %d\nLosses: %d\nWin Rate: %.2f%%\n",
                numEpisodes, wins, losses, winRate);
    }
}