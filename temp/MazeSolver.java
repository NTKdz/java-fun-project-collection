import java.util.*;

public class MazeSolver {

    private static final int[] dRow = {-1, 1, 0, 0};
    private static final int[] dCol = {0, 0, -1, 1};

    private static boolean isValidMove(int row, int col, int[][] maze, boolean[][] visited) {
        return (row >= 0 && row < maze.length && col >= 0 && col < maze[0].length
                && maze[row][col] == 0 && !visited[row][col]);
    }

    // BFS to find the shortest path
    public static void bfsSolve(int[][] maze, int startRow, int startCol, int endRow, int endCol) {
        int rows = maze.length;
        int cols = maze[0].length;

        // Visited array to keep track of visited cells
        boolean[][] visited = new boolean[rows][cols];
        visited[startRow][startCol] = true;

        // Parent array to track the path
        int[][] parent = new int[rows][cols];

        // Queue to hold positions (row, col) and the path length
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startRow, startCol, 0});  // {row, col, distance}

        // BFS loop
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int currRow = current[0];
            int currCol = current[1];
            int distance = current[2];

            // If we reached the destination, print the distance
            if (currRow == endRow && currCol == endCol) {
                System.out.println("Shortest path length: " + distance);
                printPath(parent, endRow, endCol);
                return;
            }

            // Check all four possible directions (up, down, left, right)
            for (int i = 0; i < 4; i++) {
                int newRow = currRow + dRow[i];
                int newCol = currCol + dCol[i];

                // If the move is valid, mark it visited and add to queue
                if (isValidMove(newRow, newCol, maze, visited)) {
                    parent[newRow][newCol] = currRow * cols + currCol; // Store parent coordinates as a linear ID
                    visited[newRow][newCol] = true;
                    queue.add(new int[]{newRow, newCol, distance + 1});
                }
            }
        }

        System.out.println("No path found.");
    }

    // Method to reconstruct and print the path from end to start
    private static void printPath(int[][] parent, int endRow, int endCol) {
        LinkedList<String> path = new LinkedList<>();
        int rows = parent.length;
        int cols = parent[0].length;
        System.out.println("fds");
        // Reconstruct the path from the destination to the start
        int current = parent[endRow][endCol];
        while (current != -1) {
            int currRow = current / cols;
            int currCol = current % cols;
            path.addFirst("(" + currRow + "," + currCol + ")");
            current = parent[currRow][currCol];
        }

        // Print the reconstructed path
        System.out.println("Path: " + String.join(" -> ", path));
    }

    public static void main(String[] args) {
        // Define the maze (0 = open, 1 = wall)
        int[][] maze = {
                {0, 1, 0, 0, 0},
                {0, 1, 0, 1, 0},
                {0, 1, 0, 0, 0},
                {0, 0, 0, 1, 1},
                {0, 0, 0, 0, 0}
        };

        // Starting point (row, col)
        int startRow = 0;
        int startCol = 0;

        // Ending point (row, col)
        int endRow = 4;
        int endCol = 4;

        // Call BFS to find the shortest path
        bfsSolve(maze, startRow, startCol, endRow, endCol);
    }
}
