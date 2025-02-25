import java.util.*;

public class Env {
    public int height = 7;
    public int width = 7;
    public int winCon = 4;
    public int[][] board = new int[height][width];
    public int score = 0;

    public Env() {
    }

    public Env(int height, int width, int winCon) {
        this.height = height;
        this.width = width;
        this.winCon = winCon;
    }

    public int[] reset() {
        board = new int[height][width];
        return getState();
    }

    public void printBoard() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
    }

    public boolean checkWin(int row, int col, int player, int winCon) {
        int countRow = 0, countCol = 0, countMainDiag = 0, countAntiDiag = 0;

        // Check horizontal (row-wise)
        for (int i = -winCon + 1; i < winCon; i++) {
            int checkCol = col + i;
            if (checkCol >= 0 && checkCol < board[0].length && board[row][checkCol] == player) {
                countRow++;
                if (countRow == winCon) return true;
            } else {
                countRow = 0;
            }
        }

        // Check vertical (column-wise)
        for (int i = -winCon + 1; i < winCon; i++) {
            int checkRow = row + i;
            if (checkRow >= 0 && checkRow < board.length && board[checkRow][col] == player) {
                countCol++;
                if (countCol == winCon) return true;
            } else {
                countCol = 0;
            }
        }

        // Check main diagonal (\)
        for (int i = -winCon + 1; i < winCon; i++) {
            int checkRow = row + i;
            int checkCol = col + i;
            if (checkRow >= 0 && checkRow < board.length && checkCol >= 0 && checkCol < board[0].length
                    && board[checkRow][checkCol] == player) {
                countMainDiag++;
                if (countMainDiag == winCon) return true;
            } else {
                countMainDiag = 0;
            }
        }

        // Check anti-diagonal (/)
        for (int i = -winCon + 1; i < winCon; i++) {
            int checkRow = row + i;
            int checkCol = col - i;
            if (checkRow >= 0 && checkRow < board.length && checkCol >= 0 && checkCol < board[0].length
                    && board[checkRow][checkCol] == player) {
                countAntiDiag++;
                if (countAntiDiag == winCon) return true;
            } else {
                countAntiDiag = 0;
            }
        }

        return false;
    }


    public int[] getState() {
        int[] state = new int[height * width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                state[i * width + j] = board[i][j];
            }
        }
        return state;
    }

    public Map<String, Object> step(int row, int col, int player) {
        board[row][col] = player;
        HashMap<String, Object> result = new HashMap<>();
        int opponent = player == 1 ? 2 : 1;
        if (checkWin(row, col, opponent, winCon)) {
            result.put("score", score);
            result.put("state", getState());
            result.put("reward", -10);
            result.put("done", true);
            return result;
        }

        boolean done = false;
        double reward = 0;
        if (checkWin(row, col, player, winCon)) {
            reward = 10;
            score++;
            done = true;
        }

        result.put("score", score);
        result.put("state", getState());
        result.put("reward", reward);
        result.put("done", done);
        return result;
    }
}
