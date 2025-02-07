import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Solver {
    public static boolean isValid(int[][] board, int value, int row, int col) {
        int startRow = (row / 3) * 3;
        int startCol = (col / 3) * 3;
        //check 3x3
        for (int i = startRow; i < startRow + 3; i++) {
            for (int j = startCol; j < startCol + 3; j++) {
                if (board[i][j] == value) {
                    return false;
                }
            }
        }
        //check row, col
        for (int i = 0; i < board.length; i++) {
            if (board[i][col] == value || board[row][i] == value) {
                return false;
            }
        }
        return true;
    }

    public static boolean solve(int[][] board, int currentRow, int currentCol) {
        for (int i = currentRow; i < board.length; i++) {
            for (int j = (i == currentRow ? currentCol : 0); j < board[i].length; j++) {
                if (board[i][j] == 0) {
                    for (int k = 1; k <= 9; k++) {
                        if (isValid(board, k, i, j)) {
                            board[i][j] = k;
                            if (solve(board, i, j)) {
                                return true;
                            }
                            board[i][j] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }


    public static int[][] generateBoard(int numClues) {
        int[][] board = new int[9][9];
        solve(board, 0, 0);

        Random r = new Random();
        for (int i = 0; i < 81 - numClues; i++) {
            int row = r.nextInt(9);
            int col = r.nextInt(9);
            board[row][col] = 0;
        }
        return board;
    }

    public static void main(String[] args) {
//        int[][] board = generateBoard(30);
//        System.out.println(Arrays.deepToString(board).replace("],", "],\n"));
        int[][] board = {
                {8, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 3, 6, 0, 0, 0, 0, 0, 0},
                {7, 0, 0, 9, 0, 2, 0, 0, 0},
                {0, 5, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 4, 5, 7, 0, 0, 0, 0},
                {0, 0, 0, 1, 0, 3, 0, 0, 0},
                {0, 1, 0, 0, 0, 0, 6, 8, 0},
                {0, 8, 5, 0, 0, 0, 0, 1, 0},
                {9, 0, 0, 0, 0, 0, 0, 4, 0}
        };
        solve(board, 0, 0);
        System.out.println("Solved Sudoku:");
        System.out.println(Arrays.deepToString(board).replace("],", "],\n"));
    }
}
