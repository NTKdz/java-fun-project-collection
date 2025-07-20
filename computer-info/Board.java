// src/Board.java
import java.util.Random;

enum GameState { IN_PROGRESS, WIN, LOSS }

public class Board {
    private final int width;
    private final int height;
    private final int numMines;
    private final Cell[][] grid;
    private GameState gameState;
    private int revealedCells;
    private boolean firstClick = true;

    public Board(int width, int height, int numMines) {
        this.width = width;
        this.height = height;
        this.numMines = numMines;
        this.grid = new Cell[height][width];
        this.gameState = GameState.IN_PROGRESS;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                grid[r][c] = new Cell();
            }
        }
    }

    // Initialize board after the first click to guarantee it's not a mine
    private void initializeBoard(int firstClickRow, int firstClickCol) {
        Random rand = new Random();
        int minesPlaced = 0;
        while (minesPlaced < numMines) {
            int r = rand.nextInt(height);
            int c = rand.nextInt(width);
            // Don't place a mine on the first click location or its direct neighbors
            if (!grid[r][c].isMine && Math.abs(r - firstClickRow) > 1 && Math.abs(c - firstClickCol) > 1) {
                grid[r][c].isMine = true;
                minesPlaced++;
            }
        }

        // Calculate adjacent mines for all cells
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (!grid[r][c].isMine) {
                    int count = 0;
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            if (dr == 0 && dc == 0) continue;
                            int nr = r + dr;
                            int nc = c + dc;
                            if (isValid(nr, nc) && grid[nr][nc].isMine) {
                                count++;
                            }
                        }
                    }
                    grid[r][c].adjacentMines = count;
                }
            }
        }
    }

    public int revealCell(int r, int c) {
        if (firstClick) {
            initializeBoard(r, c);
            firstClick = false;
        }

        if (!isValid(r, c) || grid[r][c].isRevealed || grid[r][c].isFlagged) {
            return 0;
        }

        grid[r][c].isRevealed = true;

        if (grid[r][c].isMine) {
            gameState = GameState.LOSS;
            return 1;
        }

        revealedCells++;
        int newlyRevealed = 1;

        if (grid[r][c].adjacentMines == 0) {
            // Cascade reveal for empty cells
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    newlyRevealed += revealCell(r + dr, c + dc);
                }
            }
        }

        checkWinCondition();
        return newlyRevealed;
    }

    private void checkWinCondition() {
        if (gameState != GameState.LOSS) {
            if (revealedCells == (width * height) - numMines) {
                gameState = GameState.WIN;
            }
        }
    }

    public boolean isValid(int r, int c) {
        return r >= 0 && r < height && c >= 0 && c < width;
    }

    public void printBoard(boolean showMines) {
        System.out.println("Board:");
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                Cell cell = grid[r][c];
                if (showMines && cell.isMine) {
                    System.out.print("* ");
                } else {
                    System.out.print(cell + " ");
                }
            }
            System.out.println();
        }
    }

    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Cell getCell(int r, int c) { return grid[r][c]; }
    public GameState getGameState() { return gameState; }
}