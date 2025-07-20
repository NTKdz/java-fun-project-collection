package com.extremevn.employee_manager.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class Point {
    double x, y;
    final double originalX, originalY;

    public Point(int x, int y) {
        this.x = this.originalX = x;
        this.y = this.originalY = y;
    }
}

class Point3d {
    double x, y, z;
    double originalX, originalY, originalZ;

    public Point3d(double x, double y, double z) {
        this.x = this.originalX = x;
        this.y = this.originalY = y;
        this.z = this.originalZ = z;
    }
}

public class Rotation {
    private static final List<List<Point>> pointsList = new ArrayList<>();
    private static final List<List<List<Point3d>>> points3d = new ArrayList<>();
    private static int angle = 0;
    private static Random random = new Random();
    static int rand = random.nextInt(1, 3);
    static boolean changed = false;
    static int framecount = 0;

    public static void initialize() {
        for (int i = 0; i < 100; i++) {
            List<Point> row = new ArrayList<>();
            for (int j = 0; j < 100; j++) {
                row.add(new Point(i - 50, j - 50));
            }
            pointsList.add(row);
        }

        for (int i = 0; i < 40; i++) {
            List<List<Point3d>> row = new ArrayList<>();
            for (int j = 0; j < 40; j++) {
                List<Point3d> col = new ArrayList<>();
                for (int k = 0; k < 40; k++) {
                    col.add(new Point3d(i - 20, j - 20, k - 20));
                }
                row.add(col);
            }
            points3d.add(row);
        }
    }

    public static void main(String[] args) {
        initialize();

        JPanel panel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(800, 800);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.RED);
                int indexY = 0;
                for (List<List<Point3d>> row : points3d) {
                    int indexX = 0; // Reset for each new row
                    for (List<Point3d> p : row) {
                        for (Point3d p3d : p) {
                            double projX = (p3d.x - p3d.z) * Math.cos(Math.toRadians(30));
                            double projY = (p3d.y + (p3d.x + p3d.z) * Math.sin(Math.toRadians(30)));
                            int screenX = (int) (projX * 5 + getWidth() / 2);
                            int screenY = (int) (projY * 5 + getHeight() / 2);
                            g.fillRect(screenX, screenY, 5, 5);
                        }
                    }
                    indexY++; // Increment per row
                }
            }
        };

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Timer timer = new Timer(1, e -> {
            angle = (angle + 3) % 360; // Keep angle in [0,360]
//            rotate();
            if (framecount++ % 20 == 0) { // Change direction every 90 degrees
                rand = random.nextInt(1, 3);
                changed = true;
            }
            rotate3d();
            frame.repaint();
        });
        timer.start();
    }

    public static void rotate() {
        double radians = Math.toRadians(angle);
        for (List<Point> row : pointsList) {
            for (Point p : row) {
                p.x = p.originalX * Math.cos(radians) - p.originalY * Math.sin(radians);
                p.y = p.originalX * Math.sin(radians) + p.originalY * Math.cos(radians);
            }
        }
    }

    public static void rotate3d() {
        double radians = Math.toRadians(angle);
        if (changed) {
            for (List<List<Point3d>> row : points3d) {
                for (List<Point3d> col : row) {
                    for (Point3d p3d : col) {
                        p3d.originalX = p3d.x;
                        p3d.originalY = p3d.y;
                        p3d.originalZ = p3d.z;
                    }
                }
            }
            angle = 0;
            changed = false;
        } else
            for (List<List<Point3d>> row : points3d) {
                for (List<Point3d> p : row) {
                    for (Point3d p3d : p) {
                        p3d.x = p3d.originalX;
                        p3d.y = p3d.originalY;
                        p3d.z = p3d.originalY;

                        switch (rand) {
                            case 1:
                                p3d.x = p3d.originalX;
                                p3d.y = p3d.originalY * Math.cos(radians) - p3d.originalZ * Math.sin(radians);
                                p3d.z = p3d.originalY * Math.sin(radians) + p3d.originalZ * Math.cos(radians);
                                break;
                            case 2:
                                p3d.x = p3d.originalX * Math.cos(radians) + p3d.originalZ * Math.sin(radians);
                                p3d.y = p3d.originalY;
                                p3d.z = -p3d.originalX * Math.sin(radians) + p3d.originalZ * Math.cos(radians);
                                break;
                            case 3:
                                p3d.x = p3d.originalX * Math.cos(radians) - p3d.originalY * Math.sin(radians);
                                p3d.y = p3d.originalX * Math.sin(radians) + p3d.originalY * Math.cos(radians);
                                p3d.z = p3d.originalZ;
                                break;
                        }


                    }
                }
            }
    }
}
