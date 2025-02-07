import java.util.*;

public class Env {
    private List<List<Integer>> map = new ArrayList<>();
    private Snake snake;
    private Food food;
    private int score = 0;
    private int maxScore = 0;
    private int width = 10;
    private int height = 10;

    public Env() {
        this(50, 50);
    }

    public Env(int width, int height) {
        this.width = width;
        this.height = height;
        reset();
    }

    public Env(List<List<Integer>> map, int score, int maxScore) {
        this.map = map;
        this.score = score;
        this.maxScore = maxScore;
    }

    public void reset() {
        score = 0;
        snake = new Snake(width / 2, height / 2);
        food = new Food();
        food.generateFood(this);
    }

    public Map<String, Object> step(int action) {
        HashMap<String, Object> result = new HashMap<>();
        boolean foodEaten = false;

        // Store previous head position
        int prevX = snake.getX();
        int prevY = snake.getY();

        // Move snake
        snake.move(action, this);

        // Check collisions
        boolean gameOver = isCollision();
        result.put("gameOver", gameOver);

        // Check food consumption
        if (snake.getX() == food.getX() && snake.getY() == food.getY()) {
            score++;
            foodEaten = true;
            snake.grow();
            food.generateFood(this);
        }

        result.put("state", getState());
        result.put("score", score);
        result.put("foodEaten", foodEaten);
        return result;
    }

    private boolean isCollision() {
        // Check wall collision
        if (snake.getX() < 0 || snake.getX() >= width ||
                snake.getY() < 0 || snake.getY() >= height) {
            return true;
        }

        // Check self-collision (skip head)
        List<int[]> body = snake.getBody();
        for (int i = 1; i < body.size(); i++) {
            int[] segment = body.get(i);
            if (segment[0] == snake.getX() && segment[1] == snake.getY()) {
                return true;
            }
        }
        return false;
    }

    // Simplified state representation
    public double[] getState() {
//        // Initialize the map grid
//        int[][] mapGrid = new int[height][width];
//
//        // Mark the snake's body on the map
//        List<int[]> body = snake.getBody();
//        for (int[] segment : body) {
//            int x = segment[0];
//            int y = segment[1];
//            if (x >= 0 && x < width && y >= 0 && y < height) {
//                mapGrid[y][x] = 1; // Snake's body
//            }
//        }
//
//        // Mark the food on the map
//        int foodX = food.getX();
//        int foodY = food.getY();
//        if (foodX >= 0 && foodX < width && foodY >= 0 && foodY < height) {
//            mapGrid[foodY][foodX] = 2; // Food
//        }
//
//        // Flatten the map grid into a 1D array
//        double[] mapState = new double[height * width];
//        int index = 0;
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                mapState[index++] = mapGrid[y][x];
//            }
//        }
//
//        // Include danger signals and food direction
//        double[] additionalState = new double[]{
//                isDanger(-1, 0) ? 1 : 0,  // Left danger
//                isDanger(1, 0) ? 1 : 0,   // Right danger
//                isDanger(0, -1) ? 1 : 0,  // Up danger
//                isDanger(0, 1) ? 1 : 0,   // Down danger
//                Integer.compare(food.getX(), snake.getX()),  // Food X direction
//                Integer.compare(food.getY(), snake.getY())   // Food Y direction
//        };
//
//        // Combine map state and additional state into a single array
//        double[] state = new double[mapState.length + additionalState.length];
//        System.arraycopy(mapState, 0, state, 0, mapState.length);
//        System.arraycopy(additionalState, 0, state, mapState.length, additionalState.length);
//
//        return mapState;
        return new double[]{
                isDanger(-1, 0) ? 1 : 0,  // Left danger
                isDanger(1, 0) ? 1 : 0,   // Right danger
                isDanger(0, -1) ? 1 : 0,  // Up danger
                isDanger(0, 1) ? 1 : 0,   // Down danger
                Integer.compare(food.getX(), snake.getX()),  // Food X direction
                Integer.compare(food.getY(), snake.getY()),   // Food Y direction
                snake.getDirection(),
//                getDistanceToObstacle(-1, 0),  // Distance to left obstacle
//                getDistanceToObstacle(1, 0),   // Distance to right obstacle
//                getDistanceToObstacle(0, -1),  // Distance to up obstacle
//                getDistanceToObstacle(0, 1),   // Distance to down obstacle
//                Math.atan2(food.getY() - snake.getY(), food.getX() - snake.getX()),  // Angle to food
//                Math.sqrt(Math.pow(food.getX() - snake.getX(), 2) + Math.pow(food.getY() - snake.getY(), 2)), // Euclidean distance to food
//                snake.getBody().size()  // Snake's current length
        };
    }

    private int getDistanceToObstacle(int dx, int dy) {
        int distance = 0;
        int x = snake.getX();
        int y = snake.getY();

        while (true) {
            x += dx;
            y += dy;
            distance++;

            // Check if the new position hits a wall
            if (x < 0 || x >= width || y < 0 || y >= height) {
                break;
            }

            boolean collided = false;
            // Check if the new position hits the snake's body
            List<int[]> body = snake.getBody();
            for (int i = 1; i < body.size(); i++) {
                int[] segment = body.get(i);
                if (segment[0] == snake.getX() && segment[1] == snake.getY()) {
                    collided = true;
                    break;
                }
            }
            if (collided) {
                break;
            }
        }

        return distance;
    }


    private boolean isDanger(int dx, int dy) {
        int newX = snake.getX() + dx;
        int newY = snake.getY() + dy;

        // Check walls
        if (newX < 0 || newX >= width || newY < 0 || newY >= height) return true;

        // Check body collision (excluding head)
        List<int[]> body = snake.getBody();
        for (int i = 1; i < body.size(); i++) {
            int[] segment = body.get(i);
            if (segment[0] == newX && segment[1] == newY) return true;
        }

        return false;
    }

    // Getters & Setters
    public Snake getSnake() {
        return snake;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getScore() {
        return score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public List<List<Integer>> getMap() {
        return map;
    }

    public void setMap(List<List<Integer>> map) {
        this.map = map;
    }

    public void setSnake(Snake snake) {
        this.snake = snake;
    }

    public Food getFood() {
        return food;
    }

    public void setFood(Food food) {
        this.food = food;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
