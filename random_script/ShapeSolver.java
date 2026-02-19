import java.util.*;

public class ShapeSolver {

    // -----------------------------
    // Helper: clone board
    // -----------------------------
    static int[][] cloneBoard(int[][] board) {
        int[][] copy = new int[board.length][board[0].length];
        for (int i = 0; i < board.length; i++) {
            copy[i] = board[i].clone();
        }
        return copy;
    }

    // -----------------------------
    // Placement check
    // -----------------------------
    static boolean canPlace(int[][] board, int[][] shape, int row, int col) {
        int N = board.length;
        int M = board[0].length;
        for (int[] cell : shape) {
            int r = row + cell[0];
            int c = col + cell[1];
            if (r < 0 || r >= N || c < 0 || c >= M) return false;
            if (board[r][c] != 0) return false; // unavailable or filled
        }
        return true;
    }

    static void applyShape(int[][] board, int[][] shape, int row, int col, int id) {
        for (int[] cell : shape) {
            board[row + cell[0]][col + cell[1]] = id;
        }
    }

    // -----------------------------
    // Count filled cells
    // -----------------------------
    static int countFilled(int[][] board) {
        int count = 0;
        for (int[] row : board) {
            for (int cell : row) {
                if (cell > 0) count++;
            }
        }
        return count;
    }

    // -----------------------------
    // Result container
    // -----------------------------
    static class Result {
        int score;
        int[][] board;
        List<Integer> used;

        Result(int score, int[][] board, List<Integer> used) {
            this.score = score;
            this.board = board;
            this.used = used;
        }
    }

    // -----------------------------
    // Recursive solver (backtracking)
    // -----------------------------
    static Result placeShapes(int[][] board, List<int[][]> shapes, int idx,
                              List<Integer> used, Result best) {
        if (idx == shapes.size()) {
            int score = countFilled(board);
            if (score > best.score) {
                best = new Result(score, cloneBoard(board), new ArrayList<>(used));
            }
            return best;
        }

        int[][] shape = shapes.get(idx);

        boolean placedSomewhere = false;
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[0].length; c++) {
                if (canPlace(board, shape, r, c)) {
                    int[][] newBoard = cloneBoard(board);
                    applyShape(newBoard, shape, r, c, idx + 1);
                    List<Integer> newUsed = new ArrayList<>(used);
                    newUsed.add(idx);
                    placedSomewhere = true;
                    best = placeShapes(newBoard, shapes, idx + 1, newUsed, best);
                }
            }
        }

        // skip this shape if it doesn’t fit
        best = placeShapes(board, shapes, idx + 1, used, best);

        return best;
    }

    // -----------------------------
    // Pretty print
    // -----------------------------
    static void printBoard(int[][] board) {
        for (int[] row : board) {
            for (int cell : row) {
                if (cell == -1) System.out.print("# ");
                else if (cell == 0) System.out.print(". ");
                else System.out.print(cell + " ");
            }
            System.out.println();
        }
    }

    // -----------------------------
    // Main example
    // -----------------------------
    public static void main(String[] args) {
        int N = 9, M = 9;
        int[][] board = new int[N][M];

        // mark unavailable cells (col=0, row=0..2)
        board[0][0] = -1;
        board[1][0] = -1;
        board[2][0] = -1;

        // define shapes (NO rotation allowed)
        int[][] shapeL = { {0,0}, {1,0}, {2,0}, {2,1} };
        int[][] shapeSquare2 = { {0,0}, {0,1}, {1,0}, {1,1} };
        int[][] shape1x1 = { {0,0} };

        int numberOfL = 2;
        int numberOfSquare2 = 2;
        int numberOf1x1 = 0;

        List<int[][]> shapes = new ArrayList<>();
        for (int i = 0; i < numberOfL; i++) shapes.add(shapeL);
        for (int i = 0; i < numberOfSquare2; i++) shapes.add(shapeSquare2);

        Result best = new Result(0, board, new ArrayList<>());
        best = placeShapes(board, shapes, 0, new ArrayList<>(), best);

        System.out.println("Best score: " + best.score);
        System.out.println("Shapes placed: " + best.used.size() + " / " + shapes.size());
        System.out.println("All placed? " + (best.used.size() == shapes.size()));
        System.out.println("Final board:");
        printBoard(best.board);
    }
}
