import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

public class Test {
    public static JPanel panel;
    public static int[][] maze;

    // ==== SETTINGS ====
    public static boolean visualize = true;   // default ON
    public static int visualizeDelay = 0;     // ms per draw

    // ==== MAZE GENERATION ====
    public static int[][] generateMaze(int rows, int cols) {
        Random rand = new Random();
        int[][] maze = new int[rows][cols];

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                maze[r][c] = rand.nextDouble() < 0.3 ? 1 : 0;

        return maze;
    }

    public static int[] getRandomOpenCell(int[][] maze) {
        Random rand = new Random();
        List<int[]> list = new ArrayList<>();

        for (int r = 0; r < maze.length; r++)
            for (int c = 0; c < maze[0].length; c++)
                if (maze[r][c] == 0) list.add(new int[]{r, c});

        return list.get(rand.nextInt(list.size()));
    }

    // ==== MAIN ====
    public static void main(String[] args) throws Exception {
        // UI
        JFrame frame = new JFrame();
        panel = new MazePanel();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 1000);
        frame.add(panel);
//        frame.setVisible(true);

        // Generate maze
        int[][] originalMaze = generateMaze(300, 300);
        int[] start = getRandomOpenCell(originalMaze);

        maze = deepCopy(originalMaze);
        maze[start[0]][start[1]] = 2;

        panel.repaint();

        // Visualization example (default)
//        bfs(originalMaze, start, true);

        // Benchmark example
        benchmark();
    }

    // ==== BENCHMARK ====
    public static void benchmark() {
        int runs = 100;

        int[] sizes = IntStream.iterate(50, n -> n <= 1000, n -> n + 50)
                .toArray();

        for (int size : sizes) {
            long totalBFS = 0;
            long totalDFS = 0;

            for (int i = 0; i < runs; i++) {

                // 1. Generate fresh maze for this run
                int[][] maze = generateMaze(size, size);

                // 2. Get random start and end for *this* maze
                int[] start = getRandomOpenCell(maze);
                int[] end = getRandomOpenCell(maze);

                while (start[0] == end[0] && start[1] == end[1])
                    end = getRandomOpenCell(maze);

                // 3. Create two identical copies for BFS/DFS fairness
                int[][] m1 = deepCopy(maze);
                int[][] m2 = deepCopy(maze);

                // 4. BFS timing
                long t1 = System.nanoTime();
                bfs(m1, start, false);   // no visualization
                long t2 = System.nanoTime();

                // 5. DFS timing
                long t3 = System.nanoTime();
                dfs(m2, start, false);   // no visualization
                long t4 = System.nanoTime();

                totalBFS += (t2 - t1);
                totalDFS += (t4 - t3);
            }

            System.out.println("=== Maze size: " + size + "x" + size + " ===");
            System.out.println("BFS: " + totalBFS / runs / 1_000_000.0 + " ms");
            System.out.println("DFS: " + totalDFS / runs / 1_000_000.0 + " ms");
            System.out.println();
        }
    }

    // ==== BFS (with visualize optional) ====
    public static void bfs(int[][] mazeInput, int[] start, boolean viz) {
        int[][] mazeLocal = mazeInput;
        boolean[][] visited = new boolean[mazeLocal.length][mazeLocal[0].length];

        Queue<int[]> q = new ArrayDeque<>();
        q.add(start);
        visited[start[0]][start[1]] = true;

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int x = cur[0], y = cur[1];

            if (viz) {
                maze = mazeLocal;      // update global for drawing
                maze[x][y] = 2;
                panel.repaint();
                sleep(visualizeDelay);
            }

            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];

                if (inRange(mazeLocal, nx, ny) &&
                        mazeLocal[nx][ny] == 0 &&
                        !visited[nx][ny]) {

                    visited[nx][ny] = true;
                    q.add(new int[]{nx, ny});
                }
            }
        }
    }

    // ==== DFS (with visualize optional) ====
    public static void dfs(int[][] mazeInput, int[] start, boolean viz) {
        int[][] mazeLocal = mazeInput;
        boolean[][] visited = new boolean[mazeLocal.length][mazeLocal[0].length];

        Deque<int[]> st = new ArrayDeque<>();
        st.push(start);
        visited[start[0]][start[1]] = true;

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!st.isEmpty()) {
            int[] cur = st.pop();
            int x = cur[0], y = cur[1];

            if (viz) {
                maze = mazeLocal;
                maze[x][y] = 2;
                panel.repaint();
                sleep(visualizeDelay);
            }

            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];

                if (inRange(mazeLocal, nx, ny) &&
                        mazeLocal[nx][ny] == 0 &&
                        !visited[nx][ny]) {

                    visited[nx][ny] = true;
                    st.push(new int[]{nx, ny});
                }
            }
        }
    }

    // ==== UTIL ====
    public static boolean inRange(int[][] m, int x, int y) {
        return x >= 0 && x < m.length && y >= 0 && y < m[0].length;
    }

    public static int[][] deepCopy(int[][] arr) {
        int[][] copy = new int[arr.length][arr[0].length];
        for (int i = 0; i < arr.length; i++)
            System.arraycopy(arr[i], 0, copy[i], 0, arr[0].length);
        return copy;
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    // Panel for drawing
    static class MazePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (maze == null) return;

            int rows = maze.length;
            int cols = maze[0].length;

            int cw = getWidth() / cols;
            int ch = getHeight() / rows;

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (maze[r][c] == 1) g.setColor(Color.BLUE);
                    else if (maze[r][c] == 2) g.setColor(Color.RED);
                    else g.setColor(Color.WHITE);

                    g.fillRect(c * cw, r * ch, cw, ch);
                    g.setColor(Color.BLACK);
                    g.drawRect(c * cw, r * ch, cw, ch);
                }
            }
        }
    }
}
