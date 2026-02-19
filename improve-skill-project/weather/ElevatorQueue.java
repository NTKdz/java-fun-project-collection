import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

public class ElevatorQueue {
    public static void main(String[] args) {
        Queue<Integer> upQueue = new PriorityQueue<>();
        Queue<Integer> downQueue = new PriorityQueue<>(Comparator.reverseOrder());
        final int[] currentDirection = {1}; // 1 for up, -1 for down
        final int[] goToFloor = {1};
        final int[] currentFloor = {1};
        boolean[][] existed = new boolean[2][20];

        JFrame frame = new JFrame();
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                int numFloors = 20;
                int tileSize = 20;

                for (int i = 0; i < numFloors; i++) {
                    if (i + 1 == currentFloor[0]) {
                        g.setColor(Color.BLUE);
                    } else g.setColor(Color.RED);
                    g.fillRect(100, i * tileSize, tileSize, tileSize);
                }
            }
        };
        Random random = new Random();

        Timer timer = new Timer(500, e -> {
            // choose floor, direction
            int chooseDirection = random.nextBoolean() ? 1 : -1;
            int chooseFloor = random.nextInt(1, 20);
            int index = chooseDirection == 1 ? 0 : 1;
            if (chooseDirection == 1 && !existed[index][chooseFloor]) {
                upQueue.offer(chooseFloor);
                existed[index][chooseFloor] = true;
            }

            if (chooseDirection == -1 && !existed[index][chooseFloor]) {
                downQueue.offer(chooseFloor);
                existed[index][chooseFloor] = true;
            }
            if (upQueue.isEmpty() && downQueue.isEmpty()) {
                return; // idle
            }
            if (currentFloor[0] < goToFloor[0]) {
                currentFloor[0]++;
            } else if (currentFloor[0] > goToFloor[0]) {
                currentFloor[0]--;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                if ((currentDirection[0] == 1 && upQueue.isEmpty()) || currentFloor[0] == 1) {
                    currentDirection[0] = downQueue.isEmpty() ? 1 : -1;
                }

                if ((currentDirection[0] == -1 && downQueue.isEmpty()) || currentFloor[0] == 20) {
                    currentDirection[0] = upQueue.isEmpty() ? -1 : 1;
                }

                if (currentDirection[0] == 1) {
                    Integer next = upQueue.poll();
                    if (next != null) {
                        goToFloor[0] = next;
                        existed[0][next] = false;
                    }
                }

                if (currentDirection[0] == -1) {
                    Integer next = downQueue.poll();
                    if (next != null) {
                        goToFloor[0] = next;
                        existed[1][next] = false;
                    }
                }
            }

            panel.repaint();
        });

        timer.start();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        panel.setBackground(Color.BLACK);
        panel.setDoubleBuffered(true);

        frame.add(panel);
        frame.setVisible(true);
    }
}
