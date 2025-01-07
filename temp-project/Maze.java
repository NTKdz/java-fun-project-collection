import java.util.*;

public class Maze {
    public static void main(String[] args) {
        int[][] maze = {
                {2, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1},
                {1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1},
                {1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 3}, // 'E' is represented as 3
                {1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1},
                {1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1},
                {1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 0},
                {1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1}
        };

        boolean[][] visited = new boolean[maze.length][maze[0].length];
        List<List<int[]>> allPaths = new ArrayList<>();

        int[] start = findStart(maze, 2);
        int[] end = findStart(maze, 3);

        if (start != null && end != null) {
            long startTime = System.currentTimeMillis();
            findAllPaths(maze, visited, start[0], start[1], end[0], end[1], new ArrayList<>(), allPaths);
            long endTime = System.currentTimeMillis();

            System.out.println("\nAll Paths:");
            for (int i = 0; i < allPaths.size(); i++) {
                System.out.println("Path " + (i + 1) + ": " + formatPath(allPaths.get(i)));
            }
            System.out.println("Total Paths Found: " + allPaths.size());
            System.out.println("Time Taken: " + (endTime - startTime) + " ms");
        } else {
            System.out.println("Start or goal position not found in the maze.");
        }
    }

    public static boolean isFull(int[][] maze, boolean[][] visited) {
        for (int i = 0; i < maze.length; i++) {
            for (int j = 0; j < maze[0].length; j++) {
                if (maze[i][j] == 0 && !visited[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void findAllPaths(int[][] maze, boolean[][] visited, int row, int col, int goalRow, int goalCol,
                                    List<int[]> path, List<List<int[]>> allPaths) {
        path.add(new int[]{row, col});
        visited[row][col] = true;

        if (row == goalRow && col == goalCol && isFull(maze, visited)) {
            allPaths.add(new ArrayList<>(path));
        } else {
            int[][] directions = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (isValidMove(maze, visited, newRow, newCol)) {
                    findAllPaths(maze, visited, newRow, newCol, goalRow, goalCol, new ArrayList<>(path), allPaths);
                }
            }
        }
        path.removeLast();
        visited[row][col] = false;
    }

    public static boolean isValidMove(int[][] maze, boolean[][] visited, int row, int col) {
        return row >= 0 && row < maze.length &&
                col >= 0 && col < maze[0].length &&
                (maze[row][col] == 0 || maze[row][col] == 3) &&
                !visited[row][col];
    }

    public static int[] findStart(int[][] maze, int value) {
        for (int i = 0; i < maze.length; i++) {
            for (int j = 0; j < maze[0].length; j++) {
                if (maze[i][j] == value) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }

    public static String formatPath(List<int[]> path) {
        StringBuilder sb = new StringBuilder();
        for (int[] point : path) {
            sb.append(Arrays.toString(point)).append(" -> ");
        }
        return sb.substring(0, sb.length() - 4);
    }
}
