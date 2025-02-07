import java.util.Random;

public class Food {
    private int x;
    private int y;
    private final Random random = new Random();

    public void generateFood(Env env) {
        do {
            x = random.nextInt(env.getWidth());
            y = random.nextInt(env.getHeight());
        } while (isOnSnake(env.getSnake()));
    }

    private boolean isOnSnake(Snake snake) {
        for (int[] segment : snake.getBody()) {
            if (segment[0] == x && segment[1] == y) {
                return true;
            }
        }
        return false;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
