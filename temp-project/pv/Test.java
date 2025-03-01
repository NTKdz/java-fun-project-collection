package pv;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class Test {
    public static void main(String[] args) {
        // Create a new frame (window)
        JFrame frame = new JFrame("Hello World Swing");

        // Create a label with "Hello, World!"
        JLabel label = new JLabel("Hello, World!", SwingConstants.CENTER);

        // Set the frame size
        frame.setSize(300, 200);

        // Add the label to the frame
        frame.add(label);

        // Set default close operation
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Make the frame visible
        frame.setVisible(true);
    }
}
