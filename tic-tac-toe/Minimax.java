import java.util.ArrayList;
import java.util.List;

public class Minimax {
    private final int PLAYER;
    private final int OPPONENT;
    private final int EMPTY = 0;
    private final int winCon; // Number of consecutive marks needed to win
    private final int maxDepth; // Maximum search depth for Minimax

    public Minimax(int player, int opponent, int winCon, int maxDepth) {
        this.PLAYER = player;
        this.OPPONENT = opponent;
        this.winCon = winCon;
        this.maxDepth = maxDepth;
    }

    // Get available moves on the board
    public List<int[]> getAvailableMoves(int[][] board) {
        List<int[]> moves = new ArrayList<>();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] == EMPTY) {
                    moves.add(new int[]{i, j});
                }
            }
        }
        return moves;
    }

    // Check if there are moves left on the board
    public boolean isMovesLeft(int[][] board) {
        return !getAvailableMoves(board).isEmpty();
    }

    // Evaluate board (Return +10 if AI wins, -10 if opponent wins, 0 if draw)
    public int evaluate(int[][] board) {
        return checkWin(board, PLAYER) ? 10 : (checkWin(board, OPPONENT) ? -10 : 0);
    }

    // Check if a player has won
    private boolean checkWin(int[][] board, int player) {
        int rows = board.length;
        int cols = board[0].length;

        // Check rows and columns
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= cols - winCon; j++) {
                if (checkLine(board, i, j, 0, 1, player)) return true;
            }
        }
        for (int j = 0; j < cols; j++) {
            for (int i = 0; i <= rows - winCon; i++) {
                if (checkLine(board, i, j, 1, 0, player)) return true;
            }
        }

        // Check diagonals
        for (int i = 0; i <= rows - winCon; i++) {
            for (int j = 0; j <= cols - winCon; j++) {
                if (checkLine(board, i, j, 1, 1, player)) return true;
                if (checkLine(board, i, j + winCon - 1, 1, -1, player)) return true;
            }
        }

        return false;
    }

    // Check if there are `winCon` consecutive marks in a row, column, or diagonal
    private boolean checkLine(int[][] board, int r, int c, int dr, int dc, int player) {
        for (int i = 0; i < winCon; i++) {
            if (board[r + i * dr][c + i * dc] != player) return false;
        }
        return true;
    }

    // Minimax function with Alpha-Beta Pruning and Depth Limit
    public int minimax(int[][] board, int depth, boolean isMax, int alpha, int beta) {
        int score = evaluate(board);

        if (score == 10) return score - depth; // Favor quicker wins
        if (score == -10) return score + depth; // Favor delaying losses
        if (!isMovesLeft(board)) return 0; // Draw
        if (depth >= maxDepth) return heuristic(board); // Limit depth to avoid infinite recursion

        int best;
        List<int[]> moves = getAvailableMoves(board);

        if (isMax) {
            best = Integer.MIN_VALUE;
            for (int[] move : moves) {
                board[move[0]][move[1]] = PLAYER;
                best = Math.max(best, minimax(board, depth + 1, false, alpha, beta));
                board[move[0]][move[1]] = EMPTY;
                alpha = Math.max(alpha, best);
                if (beta <= alpha) break;
            }
        } else {
            best = Integer.MAX_VALUE;
            for (int[] move : moves) {
                board[move[0]][move[1]] = OPPONENT;
                best = Math.min(best, minimax(board, depth + 1, true, alpha, beta));
                board[move[0]][move[1]] = EMPTY;
                beta = Math.min(beta, best);
                if (beta <= alpha) break;
            }
        }
        return best;
    }

    // Simple heuristic for large boards (prioritizes center control)
    private int heuristic(int[][] board) {
        int score = 0;
        int midRow = board.length / 2;
        int midCol = board[0].length / 2;

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] == PLAYER) {
                    score += (midRow - Math.abs(midRow - i)) + (midCol - Math.abs(midCol - j));
                } else if (board[i][j] == OPPONENT) {
                    score -= (midRow - Math.abs(midRow - i)) + (midCol - Math.abs(midCol - j));
                }
            }
        }
        return score;
    }

    // Find the best move for AI
    public int[] findBestMove(int[][] board) {
        int bestVal = Integer.MIN_VALUE;
        int[] bestMove = {-1, -1};

        for (int[] move : getAvailableMoves(board)) {
            board[move[0]][move[1]] = PLAYER;
            int moveVal = minimax(board, 0, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
            board[move[0]][move[1]] = EMPTY;

            if (moveVal > bestVal) {
                bestMove = move.clone();
                bestVal = moveVal;
            }
        }
        return bestMove;
    }
}
