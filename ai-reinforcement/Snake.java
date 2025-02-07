import java.util.*;

public class Snake {
    private int x;
    private int y;
    private int direction;
    private List<int[]> body = new ArrayList<>();
    private boolean growing = false;

    public Snake(int x, int y, int direction, List<int[]> body) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.body = new ArrayList<>(body);
    }

    public Snake(int x, int y, int direction) {
        this(x, y);
        this.direction = direction;
    }

    public Snake(int x, int y) {
        this.x = x;
        this.y = y;
        this.direction = 3; // Default moving down
        body.add(new int[]{x, y});
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public List<int[]> getBody() {
        return body;
    }

    /**
     * Moves the snake in the given direction.
     *
     * @param action 0=Left, 1=Right, 2=Up, 3=Down
     */
    public void move(int action, Env env) {
        int dx = 0, dy = 0;
        switch (action) {
            case 0:
                dx = -1;
                break; // Left
            case 1:
                dx = 1;
                break;  // Right
            case 2:
                dy = -1;
                break; // Up
            case 3:
                dy = 1;
                break;  // Down
        }

        // Update head position
        x += dx;
        y += dy;

        // Add new head
        body.add(0, new int[]{x, y});

        // Remove tail if not growing
        if (!growing) {
            body.remove(body.size() - 1);
        } else {
            growing = false;
        }
    }

    public void grow() {
        growing = true;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public void setBody(List<int[]> body) {
        this.body = body;
    }
}
