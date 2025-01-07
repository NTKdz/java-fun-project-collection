import java.io.*;
import java.util.*;

public class TicTacToeQLearning {
    private static final int BOARD_SIZE = 3;
    private static final int WIN_CONDITION = 3;
    private static final double LEARNING_RATE = 0.3;
    private static final double DISCOUNT_FACTOR = 0.95;
    private static final double EPSILON = 0.4;

    private final Map<String, Double> qTable = new HashMap<>();
    private final Random random = new Random();

    public static void main(String[] args) {
        TicTacToeQLearning agent = new TicTacToeQLearning();

        // Load Q-table if exists
        agent.loadQTable("qtable1.dat");

        agent.train(1000000); // Train for 10,000 episodes

        // Save Q-table after training
        agent.saveQTable("qtable1.dat");

        agent.playGame(); // Play a game
    }

    private void train(int episodes) {
        for (int episode = 0; episode < episodes; episode++) {
            System.out.println(episode + " ");
            char[][] board = new char[BOARD_SIZE][BOARD_SIZE];
            for (char[] row : board) Arrays.fill(row, '-');

            boolean isPlayerTurn = random.nextBoolean();
            while (true) {
                if (isPlayerTurn) {
                    makeRandomMove(board, 'O');
                } else {
                    makeQLearningMove(board, 'X');
                }

                if (checkWin(board, isPlayerTurn ? 'O' : 'X') || isBoardFull(board)) {
                    updateQValues(board, isPlayerTurn ? 'O' : 'X');
                    break;
                }

                isPlayerTurn = !isPlayerTurn;
            }
        }
    }

    private void playGame() {
        char[][] board = new char[BOARD_SIZE][BOARD_SIZE];
        for (char[] row : board) Arrays.fill(row, '-');

        boolean isPlayerTurn = true;
        while (true) {
            printBoard(board);
            if (isPlayerTurn) {
                makeHumanMove(board, 'O');
            } else {
                makeQLearningMove(board, 'X');
            }

            if (checkWin(board, isPlayerTurn ? 'O' : 'X')) {
                printBoard(board);
                System.out.println((isPlayerTurn ? "Player" : "AI") + " wins!");
                break;
            }

            if (isBoardFull(board)) {
                printBoard(board);
                System.out.println("It's a draw!");
                break;
            }

            isPlayerTurn = !isPlayerTurn;
        }
    }

    private void makeQLearningMove(char[][] board, char symbol) {
        String state = encodeState(board);
        List<int[]> availableMoves = getAvailableMoves(board);

        int[] bestMove = null;
        double maxQValue = Double.NEGATIVE_INFINITY;

        for (int[] move : availableMoves) {
            char[][] newBoard = copyBoard(board);
            newBoard[move[0]][move[1]] = symbol;
            String newState = encodeState(newBoard);
            double qValue = qTable.getOrDefault(newState, 0.0);

            if (qValue > maxQValue) {
                maxQValue = qValue;
                bestMove = move;
            }
        }

        if (bestMove == null || random.nextDouble() < EPSILON) {
            bestMove = availableMoves.get(random.nextInt(availableMoves.size()));
        }

        board[bestMove[0]][bestMove[1]] = symbol;
    }

    private void makeRandomMove(char[][] board, char symbol) {
        List<int[]> availableMoves = getAvailableMoves(board);
        int[] move = availableMoves.get(random.nextInt(availableMoves.size()));
        board[move[0]][move[1]] = symbol;
    }

    private void makeHumanMove(char[][] board, char symbol) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter your move (row and column): ");
            int row = scanner.nextInt();
            int col = scanner.nextInt();

            if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE && board[row][col] == '-') {
                board[row][col] = symbol;
                break;
            } else {
                System.out.println("Invalid move. Try again.");
            }
        }
    }

    private void updateQValues(char[][] board, char winner) {
        String state = encodeState(board);
        double reward = winner == 'X' ? 1.0 : (winner == 'O' ? -1.0 : 0.5);
        qTable.put(state, reward);

        for (Map.Entry<String, Double> entry : qTable.entrySet()) {
            String prevState = entry.getKey();
            double oldQValue = entry.getValue();
            double newQValue = oldQValue + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxQValue(state) - oldQValue);
            qTable.put(prevState, newQValue);
        }
    }

    private double maxQValue(String state) {
        return qTable.getOrDefault(state, 0.0);
    }

    private boolean checkWin(char[][] board, char symbol) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (checkDirection(board, symbol, i, j, 1, 0) ||
                        checkDirection(board, symbol, i, j, 0, 1) ||
                        checkDirection(board, symbol, i, j, 1, 1) ||
                        checkDirection(board, symbol, i, j, 1, -1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkDirection(char[][] board, char symbol, int x, int y, int dx, int dy) {
        int count = 0;
        for (int i = 0; i < WIN_CONDITION; i++) {
            int nx = x + i * dx;
            int ny = y + i * dy;
            if (nx >= 0 && nx < BOARD_SIZE && ny >= 0 && ny < BOARD_SIZE && board[nx][ny] == symbol) {
                count++;
            } else {
                break;
            }
        }
        return count == WIN_CONDITION;
    }

    private boolean isBoardFull(char[][] board) {
        for (char[] row : board) {
            for (char cell : row) {
                if (cell == '-') return false;
            }
        }
        return true;
    }

    private String encodeState(char[][] board) {
        StringBuilder sb = new StringBuilder();
        for (char[] row : board) {
            for (char cell : row) {
                sb.append(cell);
            }
        }
        return sb.toString();
    }

    private char[][] copyBoard(char[][] board) {
        char[][] newBoard = new char[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(board[i], 0, newBoard[i], 0, BOARD_SIZE);
        }
        return newBoard;
    }

    private List<int[]> getAvailableMoves(char[][] board) {
        List<int[]> moves = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == '-') {
                    moves.add(new int[]{i, j});
                }
            }
        }
        return moves;
    }

    private void printBoard(char[][] board) {
        for (char[] row : board) {
            for (char cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
    }

    private void saveQTable(String fileName) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(qTable);
            System.out.println("Q-table saved to " + fileName);
        } catch (IOException e) {
            System.err.println("Error saving Q-table: " + e.getMessage());
        }
    }

    private void loadQTable(String fileName) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            Map<String, Double> loadedQTable = (Map<String, Double>) ois.readObject();
            qTable.putAll(loadedQTable);
            System.out.println("Q-table loaded from " + fileName);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading Q-table: " + e.getMessage());
        }
    }
}
