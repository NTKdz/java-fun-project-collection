import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class ScreenCaptureTranslator extends JFrame {
    private JTextArea textArea;
    private JComboBox<String> languageComboBox;
    private JLabel statusLabel;
    private Rectangle captureRect = null;
    private Point startPoint = null;
    private Point endPoint = null;
    private JPanel capturePanel;

    public ScreenCaptureTranslator() {
        setTitle("Screen Capture Translator");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Language selection
        String[] languages = {"English", "Spanish", "French", "German"};
        languageComboBox = new JComboBox<>(languages);
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Target Language:"));
        topPanel.add(languageComboBox);
        JButton captureButton = new JButton("Capture Screen (Ctrl+Shift+S)");
        topPanel.add(captureButton);
        add(topPanel, BorderLayout.NORTH);

        // Text display
        textArea = new JTextArea();
        textArea.setEditable(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        // Status label
        statusLabel = new JLabel("Ready");
        add(statusLabel, BorderLayout.SOUTH);

        // Capture panel for drawing rectangle
        capturePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (startPoint != null && endPoint != null) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(new Color(0, 0, 255, 50));
                    Rectangle rect = getRectangle();
                    g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
                    g2d.setColor(Color.BLUE);
                    g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
                }
            }
        };
        capturePanel.setOpaque(false);

        // Mouse listeners for drawing capture rectangle
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                endPoint = startPoint;
                capturePanel.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                endPoint = e.getPoint();
                capturePanel.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                endPoint = e.getPoint();
                captureRect = getRectangle();
                capturePanel.repaint();
                captureAndProcess();
            }
        };
        capturePanel.addMouseListener(mouseAdapter);
        capturePanel.addMouseMotionListener(mouseAdapter);

        // Global key binding for Ctrl+Shift+S
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "capture");
        actionMap.put("capture", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startCapture();
            }
        });

        // Capture button action
        captureButton.addActionListener(e -> startCapture());
    }

    private Rectangle getRectangle() {
        if (startPoint == null || endPoint == null) return new Rectangle(0, 0, 0, 0);
        int x = Math.min(startPoint.x, endPoint.x);
        int y = Math.min(startPoint.y, endPoint.y);
        int width = Math.abs(endPoint.x - startPoint.x);
        int height = Math.abs(endPoint.y - endPoint.y);
        return new Rectangle(x, y, width, height);
    }

    private void startCapture() {
        // Make window semi-transparent for capture
        setOpacity(0.7f);
        getContentPane().add(capturePanel, BorderLayout.CENTER);
        capturePanel.setVisible(true);
        revalidate();
        statusLabel.setText("Select area to capture...");
    }

    private void captureAndProcess() {
        if (captureRect == null || captureRect.width == 0 || captureRect.height == 0) {
            statusLabel.setText("No area selected!");
            resetCapture();
            return;
        }

        try {
            // Capture screenshot
            Robot robot = new Robot();
            BufferedImage screenshot = robot.createScreenCapture(captureRect);

            // Save screenshot temporarily
            File tempFile = File.createTempFile("screenshot", ".png");
            ImageIO.write(screenshot, "png", tempFile);

            // Perform OCR
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("path/to/tessdata"); // Set path to Tesseract data
            String extractedText = tesseract.doOCR(tempFile);

            // Translate text
            String targetLanguage = (String) languageComboBox.getSelectedItem();
            String translatedText = translateText(extractedText, targetLanguage);

            // Display results
            textArea.setText("Extracted Text:\n" + extractedText + "\n\nTranslated Text:\n" + translatedText);
            statusLabel.setText("Capture and translation complete!");

            // Clean up
            tempFile.delete();
        } catch (AWTException | IOException | TesseractException ex) {
            ex.printStackTrace();
            statusLabel.setText("Error: " + ex.getMessage());
        } finally {
            resetCapture();
        }
    }

    private void resetCapture() {
        setOpacity(1.0f);
        capturePanel.setVisible(false);
        getContentPane().remove(capturePanel);
        startPoint = null;
        endPoint = null;
        captureRect = null;
        revalidate();
    }

    private String translateText(String text, String targetLanguage) {
        // Placeholder for translation logic
        // In a real app, integrate with a translation API (e.g., Google Translate)
        return "Translated to " + targetLanguage + ": " + text;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ScreenCaptureTranslator app = new ScreenCaptureTranslator();
            app.setVisible(true);
        });
    }
}