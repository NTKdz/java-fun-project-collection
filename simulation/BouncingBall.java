import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;

public class BouncingBall {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;
    private static final int BALL_SIZE = 20;
    private static final int MIN_SPEED = 5;
    private static final int MAX_SPEED = 15;
    private static final List<Ball> balls = Collections.synchronizedList(new ArrayList<>());
    private static final Random rand = new Random();
    private static final int COLLISION_DISTANCE_SQUARED = BALL_SIZE * BALL_SIZE;
    private static final double DECELERATION = 1;

    public static void main(String[] args) {
        balls.add(new Ball(WIDTH / 2, HEIGHT / 3,
                rand.nextInt(MIN_SPEED, MAX_SPEED),
                rand.nextInt(MIN_SPEED, MAX_SPEED)));

        JFrame frame = new JFrame("Bouncing Ball");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (Ball ball : balls) {
                    g.fillOval(ball.x, ball.y, BALL_SIZE, BALL_SIZE);
                }
            }
        };

        panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);

        new Timer(16, e -> {
            update();
            panel.repaint();
        }).start();
    }

    private static void update() {
        balls.forEach(Ball::move);
        handleWallCollisions();
        handleCollisions();
        for (Ball ball : balls) {
            if (ball.dy > 1) ball.dy *= DECELERATION;
            if (ball.dx > 1) ball.dx *= DECELERATION;
        }
    }

    private static void handleWallCollisions() {
        List<Ball> newBalls = new ArrayList<>();

        for (Ball ball : balls) {
            boolean verticalHit = false;
            boolean horizontalHit = false;

            if (ball.x <= 0 || ball.x + BALL_SIZE >= WIDTH) {
                verticalHit = true;
//                ball.dx = ball.dx > 0 ? -rand.nextInt(1, 20) : rand.nextInt(1, 20);
                ball.dx = -ball.dx;
            }

            if (ball.y <= 0 || ball.y + BALL_SIZE >= HEIGHT) {
                horizontalHit = true;
//                ball.dy = ball.dy > 0 ? -rand.nextInt(1, 20) : rand.nextInt(1, 20);
                ball.dy = -ball.dy;
            }

            if (verticalHit || horizontalHit) {
                if (newBalls.size() + balls.size() < 30) {
                    int dx = rand.nextInt(-20, 20);
                    int x = rand.nextInt(BALL_SIZE, WIDTH - BALL_SIZE - 10);
                    int y = rand.nextInt(BALL_SIZE, HEIGHT - BALL_SIZE - 10);
                    newBalls.add(new Ball(x,
                            y, dx, dx + 5));
                }
                ensureMinimumSpeed(ball);
            }
        }

        balls.addAll(newBalls);
    }

    private static void ensureMinimumSpeed(Ball ball) {
        if (Math.abs(ball.dx) < MIN_SPEED) {
            ball.dx = MIN_SPEED * Integer.signum((int) ball.dx);
        }
        if (Math.abs(ball.dy) < MIN_SPEED) {
            ball.dy = MIN_SPEED * Integer.signum((int) ball.dy);
        }
    }

    private static void handleCollisions() {
        Set<Ball> toRemove = new HashSet<>();
        int size = balls.size();

        for (int i = 0; i < size; i++) {
            Ball b1 = balls.get(i);
            if (toRemove.contains(b1)) continue;

            for (int j = i + 1; j < size; j++) {
                Ball b2 = balls.get(j);
                if (toRemove.contains(b2)) continue;

                int centerX1 = b1.x + BALL_SIZE / 2;
                int centerY1 = b1.y + BALL_SIZE / 2;
                int centerX2 = b2.x + BALL_SIZE / 2;
                int centerY2 = b2.y + BALL_SIZE / 2;

                int dx = centerX2 - centerX1;
                int dy = centerY2 - centerY1;
                int distSq = dx * dx + dy * dy;

                if (distSq < COLLISION_DISTANCE_SQUARED) {
                    // Remove the second ball in the pair
//                    double tempX = b1.dx;
//                    double tempY = b1.dy;
                    b1.dy = b2.dy;
                    b1.dx = b2.dx;
//                    b2.dy = tempX;
//                    b2.dx = tempY;
//                    toRemove.add(b1);
                    toRemove.add(b2);
                }
            }
        }

        balls.removeAll(toRemove);
    }
}

