import javax.swing.*;
import java.awt.*;

public class Square {
    private static final int SQUARE_WIDTH = 200;
    private static final int SQUARE_HEIGHT = 200;
    private static final int FRAME_SIZE = 500;
    private static final int SQUARE_X = (FRAME_SIZE - SQUARE_WIDTH) / 2;
    private static final int SQUARE_Y = (FRAME_SIZE - SQUARE_HEIGHT) / 2;
    private static int angle = 0;

    public static void main(String[] args) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.BLACK);

                g.setFont(new Font("Monospaced", Font.PLAIN, 12));
                FontMetrics metrics = g.getFontMetrics();

                int charWidth = metrics.charWidth('a');
                int charHeight = metrics.getHeight();

                int centerX = SQUARE_X + SQUARE_WIDTH / 2;
                int centerY = SQUARE_Y + SQUARE_HEIGHT / 2;

                for (int i = 0; i < SQUARE_HEIGHT / charHeight; i++) {
                    for (int j = 0; j < SQUARE_WIDTH / charWidth; j++) {
                        int charX = SQUARE_X + j * charWidth;
                        int charY = SQUARE_Y + i * charHeight + metrics.getAscent();

                        int translatedX = charX - centerX;
                        int translatedY = charY - centerY;

                        int rotatedX = (int) (translatedX * Math.cos(Math.toRadians(angle))
                                - translatedY * Math.sin(Math.toRadians(angle)));
                        int rotatedY = (int) (translatedX * Math.sin(Math.toRadians(angle))
                                + translatedY * Math.cos(Math.toRadians(angle)));

                        int finalX = rotatedX + centerX;
                        int finalY = rotatedY + centerY;
                        g.drawChars(new char[]{'a'}, 0, 1, finalX, finalY);
                    }
                }
            }
        };

        JFrame frame = new JFrame();
        frame.setSize(FRAME_SIZE, FRAME_SIZE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);

        Timer gameTimer = new Timer(1/120*1000, e -> {
            panel.repaint();
            angle++;
        });
        gameTimer.start();
    }
}
