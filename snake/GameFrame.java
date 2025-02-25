import javax.swing.*;

public class GameFrame extends JFrame {
    public GameFrame() {
        setTitle("A* Snake Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel panel = new GamePanel();
        add(panel);
        pack();

        setLocationRelativeTo(null);
    }
}
