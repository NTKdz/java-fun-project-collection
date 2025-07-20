package desktop.app.jnatest;

import net.sourceforge.tess4j.Tesseract;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class OCRTranslator {
    public static void main(String[] args) {
        try {
            final LanguageSelectionBar[] langBar = new LanguageSelectionBar[1];

            langBar[0] = new LanguageSelectionBar(e -> langBar[0].swapLanguages());

            Rectangle selectedArea = selectScreenArea(langBar[0]);
            if (selectedArea == null) {
                System.out.println("No area selected.");
                return;
            }

            BufferedImage image = captureScreen(selectedArea);

            String tesseractLangCode = LanguageMap.getTesseractCode(langBar[0].getSourceLanguage());
            String lingvaLangCode = LanguageMap.getLingvaCode(langBar[0].getTargetLanguage());

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("tessdata");
            tesseract.setLanguage(tesseractLangCode);

            String result = tesseract.doOCR(image);
            System.out.println("OCR Result: " + result);

            String translated = TranslateHelper.translateText(result, lingvaLangCode);
            System.out.println("Translated: " + translated);

            System.exit(0);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static BufferedImage captureScreen(Rectangle area) throws Exception {
        Robot robot = new Robot();
        return robot.createScreenCapture(area);
    }

    public static Rectangle selectScreenArea(LanguageSelectionBar langBar) throws Exception {
        final Rectangle[] selectedRect = {null};
        final Object lock = new Object();

        JWindow overlayWindow = new JWindow();
        overlayWindow.setAlwaysOnTop(true);
        overlayWindow.setOpacity(0.3f);
        overlayWindow.setBackground(Color.black);
        overlayWindow.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        overlayWindow.setFocusableWindowState(true);

        JPanel glass = new JPanel() {
            Point start = null;
            Point end = null;
            boolean dragging = false;

            {
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        Point mouse = e.getLocationOnScreen();
                        Rectangle langBounds = langBar.getBoundsOnScreen();
                        if (langBounds.contains(mouse)) {
                            return; // Let LanguageSelectionBar handle the event
                        }
                        start = e.getPoint();
                        dragging = true;
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        Point mouse = e.getLocationOnScreen();
                        Rectangle langBounds = langBar.getBoundsOnScreen();
                        if (langBounds.contains(mouse)) {
                            return; // Let LanguageSelectionBar handle the event
                        }
                        if (!dragging) return;
                        dragging = false;

                        end = e.getPoint();
                        int x = Math.min(start.x, end.x);
                        int y = Math.min(start.y, end.y);
                        int width = Math.abs(start.x - end.x);
                        int height = Math.abs(start.y - end.y);
                        selectedRect[0] = new Rectangle(x, y, width, height);

                        overlayWindow.setVisible(false);
                        langBar.setVisible(false);

                        synchronized (lock) {
                            lock.notify();
                        }
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        Point mouse = e.getLocationOnScreen();
                        Rectangle langBounds = langBar.getBoundsOnScreen();
                        if (langBounds.contains(mouse)) {
                            return; // Let LanguageSelectionBar handle the event
                        }
                    }
                });

                addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        Point mouse = e.getLocationOnScreen();
                        Rectangle langBounds = langBar.getBoundsOnScreen();
                        if (langBounds.contains(mouse)) {
                            return; // Let LanguageSelectionBar handle the event
                        }
                        if (!dragging) return;
                        end = e.getPoint();
                        repaint();
                    }
                });
            }

            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (start != null && end != null) {
                    int x = Math.min(start.x, end.x);
                    int y = Math.min(start.y, end.y);
                    int width = Math.abs(start.x - end.x);
                    int height = Math.abs(start.y - end.y);
                    g.setColor(Color.white);
                    g.drawRect(x, y, width, height);
                }
            }
        };

        overlayWindow.setContentPane(glass);
        overlayWindow.add(langBar);
        langBar.setBounds(50, 10, 300, 40); // Position the LanguageSelectionBar
        overlayWindow.setVisible(true);
        overlayWindow.setComponentZOrder(langBar, 0);
        overlayWindow.setComponentZOrder(glass, 1);
        synchronized (lock) {
            lock.wait();
        }

        return selectedRect[0];
    }
}