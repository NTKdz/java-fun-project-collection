import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TicTacToe {
    private List<List<Character>> board;
    private int boardSize = 20;
    private int[] lastAction = new int[2];

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tic tac toe");
        TicTacToe game = new TicTacToe();
        game.init();
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                int rows = game.board.size();
                int cols = game.board.get(0).size();

                int tileW = getWidth() / cols;
                int tileH = getHeight() / rows;

                g.setFont(g.getFont().deriveFont((float) (Math.min(tileW, tileH) * 0.8)));
                FontMetrics fm = g.getFontMetrics();

                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {

                        int tileX = col * tileW;
                        int tileY = row * tileH;

                        g.setColor(Color.RED);
                        g.drawRect(tileX, tileY, tileW, tileH);

                        char cell = game.board.get(row).get(col);
                        if (cell != '.') {
                            String s = String.valueOf(cell);

                            int textWidth = fm.stringWidth(s);
                            int textHeight = fm.getAscent();

                            int textX = tileX + (tileW - textWidth) / 2;
                            int textY = tileY + (tileH + textHeight) / 2;

                            g.drawString(s, textX, textY);
                        }
                    }
                }
            }
        };

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int mouseX = e.getX();
                int mouseY = e.getY();

                int rows = game.board.size();
                int cols = game.board.get(0).size();

                int tileW = panel.getWidth() / cols;
                int tileH = panel.getHeight() / rows;

                int col = mouseX / tileW;
                int row = mouseY / tileH;

                // safety check
                if (row >= 0 && row < rows && col >= 0 && col < cols) {
                    System.out.println("Clicked row=" + row + " col=" + col);
                    game.board.get(row).set(col, 'x');
                    game.lastAction[0] = row;
                    game.lastAction[1] = col;
                }
            }
        });

        Timer timer = new Timer(1000 / 60, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.repaint();
                if (game.isGameEnd()) {
                    System.out.println("end");
                }
            }
        });
        timer.start();
        panel.setPreferredSize(new Dimension(500, 500));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setVisible(true);
        frame.setSize(new Dimension(1000, 1000));
    }

    private boolean isGameEnd() {
        int row = this.lastAction[0];
        int col = this.lastAction[1];
        char player = this.board.get(row).get(col); // usually 'x' or 'o'

        return checkDir(row, col, 1, 0, player) ||   // horizontal
                checkDir(row, col, 0, 1, player) ||   // vertical
                checkDir(row, col, 1, 1, player) ||   // diagonal \
                checkDir(row, col, 1, -1, player);    // diagonal /
    }

    private boolean checkDir(int row, int col, int dRow, int dCol, char player) {
        int count = 1;

        // forward direction
        count += countOneDir(row, col, dRow, dCol, player);

        // backward direction
        count += countOneDir(row, col, -dRow, -dCol, player);

        System.out.println(count);
        return count >= 5;
    }

    private int countOneDir(int row, int col, int dRow, int dCol, char player) {
        int r = row + dRow;
        int c = col + dCol;
        int count = 0;

        while (r >= 0 && r < board.size()
                && c >= 0 && c < board.size()
                && board.get(r).get(c) == player) {

            count++;
            r += dRow;
            c += dCol;
        }
        return count;
    }

    private void init() {
        List<List<Character>> newBoard = new ArrayList<>();
        for (int i = 0; i < boardSize; i++) {
            newBoard.add(new ArrayList<>());
            for (int j = 0; j < boardSize; j++) {
                newBoard.get(i).add('.');
            }
        }
        this.board = newBoard;
    }
}