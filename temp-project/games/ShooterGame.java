package games;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ShooterGame {
    private final List<Enemy> enemyList = new ArrayList<>();
    private final Player player;
    private static final int tileSize = 10;

    private static class Entity {
        int x;
        int y;

        Entity(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class Enemy extends Entity {
        Enemy(int x, int y) {
            super(x, y);
        }

        void move() {
            y += tileSize;
        }
    }

    private static class Player extends Entity {
        Player(int x, int y) {
            super(x, y);
        }

        public void move(int dx, int dy) {
            this.x += dx;
            this.y += dy;
        }
    }

    public ShooterGame() {
        player = new Player(tileSize, tileSize);

        for (int i = 0; i < 5; i++) {
            enemyList.add(new Enemy(100 + i * 100, 50));
        }
    }

    public void start() {
        JPanel panel = getJPanel();
        JFrame frame = new JFrame("Shooter Game");
        frame.setSize(new Dimension(800, 600));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                System.out.println(e.getKeyCode());
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        player.move(0, -tileSize);
                        break;
                    case KeyEvent.VK_DOWN:
                        player.move(0, tileSize);
                        break;
                    case KeyEvent.VK_LEFT:
                        player.move(-tileSize, 0);
                        break;
                    case KeyEvent.VK_RIGHT:
                        player.move(tileSize, 0);
                        break;
                }
            }
        });
        panel.setFocusable(true);
        Timer gameTimer = new Timer(16, new ActionListener() {
            final long startTime = System.currentTimeMillis();
            long lastTime = System.currentTimeMillis();

            @Override
            public void actionPerformed(ActionEvent e) {
                lastTime = System.currentTimeMillis();
                updateGame(startTime, lastTime);
                panel.repaint();
            }
        });

        gameTimer.start();
    }

    private JPanel getJPanel() {
        JPanel panel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.WHITE);

                g.fillRect(player.x, player.y, 30, 30);

                for (Enemy enemy : enemyList) {
                    g.fillOval(enemy.x, enemy.y, 20, 20);
                }
            }
        };

        panel.setBackground(Color.BLACK);
        return panel;
    }

    private void updateGame(long start, long end) {
        System.out.println((start - end) % 1000);
        if (Math.abs((start - end) % 1000) < 100)
            for (Enemy enemy : enemyList) {
                enemy.move();
            }

    }

    public static void main(String[] args) {
        ShooterGame game = new ShooterGame();
        game.start();
    }
}
