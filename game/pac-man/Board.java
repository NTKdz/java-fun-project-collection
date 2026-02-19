public class Board {
    private int[][] grid;
    private int score = 0;
    private Movable pacman;
    private Movable[] enemies;
    private Pepple[] pepple;
    private int width;
    private int height;

    private int[][] baseGrid = {
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, // top wall
            {1, 2, 2, 1, 2, 2, 2, 1, 2, 1}, // 2 = pellet
            {1, 2, 1, 1, 2, 1, 1, 1, 2, 1},
            {1, 2, 1, 0, 0, 0, 0, 1, 2, 1}, // 0 = empty path (for power-ups or Pac-Man start)
            {1, 2, 1, 0, 1, 1, 0, 1, 2, 1},
            {1, 2, 0, 0, 2, 2, 0, 0, 2, 1},
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}  // bottom wall
    };

    public Board(int width, int height) {
        grid = new int[height][width];
        this.width = width;
        this.height = height;
    }

    public void reset() {
        for (int i = 0; i < grid.length; i++) {
            System.arraycopy(baseGrid[i], 0, grid[i], 0, grid[i].length);
        }

        score = 0;
        pacman = new Pacman(1, 3, Movable.Direction.RIGHT);
        enemies = new Enemy[]{
                new Enemy(8, 1, Movable.Direction.LEFT),
                new Enemy(8, 5, Movable.Direction.LEFT)
        };

        pepple = new Pepple[20];
        int index = 0;
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                if (grid[y][x] == 2) {
                    pepple[index++] = new Pepple(x, y);
                }
            }
        }
    }

    public void nextFrame() {
        //enemies move
        for (Movable enemy : enemies) {
            int dx = 0, dy = 0;
            switch (enemy.getDirection()) {
                case UP -> dy = -1;
                case DOWN -> dy = 1;
                case LEFT -> dx = -1;
                case RIGHT -> dx = 1;
            }

            if (grid[enemy.getY() + dy][enemy.getX() + dx] != 1) {
                enemy.move(dx, dy);
            } else {
                // Change direction randomly if hit a wall
                Movable.Direction[] directions = Movable.Direction.values();
                enemy.setDirection(directions[(int) (Math.random() * directions.length)]);
            }
        }

        //pacman move
        int dx = 0, dy = 0;
        switch (pacman.getDirection()) {
            case UP -> dy = -1;
            case DOWN -> dy = 1;
            case LEFT -> dx = -1;
            case RIGHT -> dx = 1;
        }

        if(grid[pacman.getY() + dy][pacman.getX() + dx] == 2) {
            score += 10;
            grid[pacman.getY() + dy][pacman.getX() + dx] = 0;
        }

        if(grid[pacman.getY() + dy][pacman.getX() + dx] != 1) {
            pacman.move(dx, dy);
            grid[pacman.getY()][pacman.getX()] = 0;
        }

        //check collisions
        for(Movable enemy : enemies) {
            if(enemy.getX() == pacman.getX() && enemy.getY() == pacman.getY()) {
                System.out.println("Game Over! Final Score: " + score);
                break;
            }
        }
    }

    public int[][] getGrid() {
        return grid;
    }

    public void setGrid(int[][] grid) {
        this.grid = grid;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Movable getPacman() {
        return pacman;
    }

    public void setPacman(Movable pacman) {
        this.pacman = pacman;
    }

    public Movable[] getEnemies() {
        return enemies;
    }

    public void setEnemies(Movable[] enemies) {
        this.enemies = enemies;
    }

    public Pepple[] getPepple() {
        return pepple;
    }

    public void setPepple(Pepple[] pepple) {
        this.pepple = pepple;
    }

    public int[][] getBaseGrid() {
        return baseGrid;
    }

    public void setBaseGrid(int[][] baseGrid) {
        this.baseGrid = baseGrid;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
