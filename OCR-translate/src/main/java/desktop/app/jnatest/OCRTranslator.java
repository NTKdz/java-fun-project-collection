package desktop.app.jnatest;

import net.sourceforge.tess4j.Tesseract;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.io.FileUtils.deleteDirectory;

public class OCRTranslator implements NativeKeyListener {
    //    private static final Object lock = new Object();
    private boolean ctrlPressed = false;
    private boolean altPressed = false;
    private TrayIcon trayIcon;
    private volatile boolean running = true;
    private final File tempTessdataDir;


    public OCRTranslator() {
        // Enable JNativeHook logging for debugging
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.ALL);

        // Extract tessdata at startup
        try {
            tempTessdataDir = extractTessdataToTemp();
            if (tempTessdataDir == null) {
                throw new RuntimeException("Failed to extract tessdata to temporary directory");
            }
            System.out.println("tessdata initialized at: " + tempTessdataDir.getAbsolutePath());

            // Register shutdown hook to clean up temporary directory
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Cleaning up temporary tessdata directory: " + tempTessdataDir.getAbsolutePath());
                try {
                    deleteDirectory(tempTessdataDir);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (IOException e) {
            System.err.println("Error initializing tessdata: " + e.getMessage());
            System.out.println(e.getMessage());
            throw new RuntimeException("Failed to initialize tessdata", e);
        }
    }

    public static void main(String[] args) {
        // Initialize the application as a background service
        SwingUtilities.invokeLater(() -> {
            try {
                OCRTranslator app = new OCRTranslator();
                app.startBackgroundService();
            } catch (Exception e) {
                System.err.println("Error starting application: " + e.getMessage());
                ////system.out.println(e.getMessage());
                System.exit(1);
            }
        });
    }

    private void startBackgroundService() throws NativeHookException {
        // Register JNativeHook for global key listening
        try {
            GlobalScreen.registerNativeHook();
            ////system.out.println("JNativeHook registered successfully");
        } catch (NativeHookException e) {
            System.err.println("Failed to register JNativeHook: " + e.getMessage());
            ////system.out.println(e.getMessage());
            throw e;
        }
        GlobalScreen.addNativeKeyListener(this);

        // Add system tray icon (fallback to default image if icon.png is missing)
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

            PopupMenu popup = new PopupMenu();
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                running = false;
                try {
                    GlobalScreen.unregisterNativeHook();
                    ////system.out.println("JNativeHook unregistered");
                } catch (NativeHookException ex) {
                    System.err.println("Error unregistering native hook: " + ex.getMessage());
                }
                SystemTray.getSystemTray().remove(trayIcon);
                System.exit(0);
            });
            popup.add(exitItem);

            trayIcon = new TrayIcon(image, "OCR Translator", popup);
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
                //system.out.println("System tray icon added");
            } catch (AWTException e) {
                System.err.println("Error adding system tray icon: " + e.getMessage());
            }
        } else {
            //system.out.println("System tray not supported");
        }

        // Keep the application running
        //system.out.println("Background service running, waiting for hotkey...");
        // Rely on system tray and JNativeHook to keep JVM alive
        // Add a daemon thread to prevent premature exit if needed
        Thread keepAlive = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.err.println("Keep-alive thread interrupted: " + e.getMessage());
                    running = false;
                }
            }
        });
        keepAlive.setDaemon(true);
        keepAlive.start();
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        //system.out.println("Key pressed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
        if (e.getKeyCode() == NativeKeyEvent.VC_CONTROL) {
            ctrlPressed = true;
        } else if (e.getKeyCode() == NativeKeyEvent.VC_ALT) {
            altPressed = true;
        } else if (e.getKeyCode() == NativeKeyEvent.VC_Z && ctrlPressed && altPressed) {
            //system.out.println("Hotkey Ctrl+Alt+T detected, running OCR Translator");
            SwingUtilities.invokeLater(() -> {
                //system.out.println("Inside invokeLater, calling runOCRTranslator");
                runOCRTranslator();
            });
        }
//        else if (e.getKeyCode() == NativeKeyEvent.VC_F1) {
//            //system.out.println("F1 pressed, running OCR Translator");
//            SwingUtilities.invokeLater(() -> {
//                //system.out.println("Inside invokeLater for F1, calling runOCRTranslator");
//                runOCRTranslator();
//            });
//        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
//        //system.out.println("Key released: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
        if (e.getKeyCode() == NativeKeyEvent.VC_CONTROL) {
            ctrlPressed = false;
//            //system.out.println("Ctrl released: " + ctrlPressed);
        } else if (e.getKeyCode() == NativeKeyEvent.VC_ALT) {
            altPressed = false;
//            //system.out.println("Alt released: " + altPressed);
        }
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Not used
    }

    private void runOCRTranslator() {
        //system.out.println("Step 0: Entered runOCRTranslator");
        try {
            //system.out.println("Step 1: Creating LanguageSelectionBar...");
            LanguageSelectionBar langBar = new LanguageSelectionBar(e -> {
                //system.out.println("Swapping languages in LanguageSelectionBar");
                ((LanguageSelectionBar) e.getSource()).swapLanguages();
            });
            //system.out.println("Step 1: LanguageSelectionBar created");

            ////system.out.println("Step 2: Selecting screen area...");
            CompletableFuture<Rectangle> future = selectScreenArea(langBar);
            future.thenAccept(selectedArea -> {
                if (selectedArea == null) {
                    ////system.out.println("Step 2: No area selected");
                    showResult("No area selected.");
                    return;
                }
                ////system.out.println("Step 2: Screen area selected: " + selectedArea);

                try {
                    ////system.out.println("Step 3: Capturing screen...");
                    BufferedImage image = captureScreen(selectedArea);
                    ////system.out.println("Step 3: Screen captured, image size: " + image.getWidth() + "x" + image.getHeight());

                    ////system.out.println("Step 4: Getting language codes...");
                    String tesseractLangCode = LanguageMap.getTesseractCode(langBar.getSourceLanguage());
                    String lingvaLangCode = LanguageMap.getLingvaCode(langBar.getTargetLanguage());
                    ////system.out.println("Step 4: Source language: " + langBar.getSourceLanguage() + " (" + tesseractLangCode + ")");
                    ////system.out.println("Step 4: Target language: " + langBar.getTargetLanguage() + " (" + lingvaLangCode + ")");

                    ////system.out.println("Step 5: Initializing Tesseract...");
                    if (tempTessdataDir == null) {
                        throw new RuntimeException("Failed to extract tessdata to temporary directory");
                    }
//                    File tessDataDir = new File("tessdata");
//                    if (!tessDataDir.exists() || !tessDataDir.isDirectory()) {
//                        throw new RuntimeException("tessdata directory not found at: " + tessDataDir.getAbsolutePath());
//                    }
                    Tesseract tesseract = new Tesseract();
                    tesseract.setDatapath(tempTessdataDir.getAbsolutePath());
                    if (tesseractLangCode == null || tesseractLangCode.isEmpty()) {
                        throw new RuntimeException("Invalid Tesseract language code for: " + langBar.getSourceLanguage());
                    }
                    tesseract.setPageSegMode(11);
                    tesseract.setLanguage(tesseractLangCode);
                    ////system.out.println("Step 5: Tesseract initialized with tessdata: " + tessDataDir.getAbsolutePath());

                    ////system.out.println("Step 6: Performing OCR...");
                    String ocrResult = tesseract.doOCR(image);
                    ////system.out.println("Step 6: OCR completed, result: " + (ocrResult != null ? ocrResult.trim() : "null"));

                    ////system.out.println("Step 7: Translating text...");
                    if (lingvaLangCode == null || lingvaLangCode.isEmpty()) {
                        throw new RuntimeException("Invalid Lingva language code for: " + langBar.getTargetLanguage());
                    }
                    String translated = TranslateHelper.translateText(ocrResult, lingvaLangCode);
                    ////system.out.println("Step 7: Translation completed: " + translated);

                    ////system.out.println("Step 8: Showing results...");
                    showResult("Translated:\n" + translated + "\n\nOCR Result:\n" + ocrResult);
                    ////system.out.println("Step 8: Results shown");
                } catch (Exception e) {
                    String errorMessage = "OCR Translator error: " + e.getMessage();
                    System.err.println(errorMessage);
                    System.out.println(e.getMessage());
                    showResult(errorMessage);
                }
            }).exceptionally(throwable -> {
                String errorMessage = "Screen selection error: " + throwable.getMessage();
                System.err.println(errorMessage);
                throwable.printStackTrace();
                showResult(errorMessage);
                return null;
            });
        } catch (Exception e) {
            String errorMessage = "OCR Translator error: " + e.getMessage();
            System.err.println(errorMessage);
            System.out.println(e.getMessage());
            showResult(errorMessage);
        }
    }

    private void showResult(String text) {
        ////system.out.println("Showing result window: " + text);
        SwingUtilities.invokeLater(() -> {
            JFrame resultFrame = new JFrame("OCR Translator Results");
            resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            resultFrame.setSize(400, 300);
            resultFrame.setLocationRelativeTo(null);

            JTextArea textArea = new JTextArea(text);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);

            JScrollPane scrollPane = new JScrollPane(textArea);
            resultFrame.add(scrollPane, BorderLayout.CENTER);

            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> resultFrame.dispose());
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(closeButton);
            resultFrame.add(buttonPanel, BorderLayout.SOUTH);

            resultFrame.setVisible(true);
            ////system.out.println("Result window made visible");
        });
    }

    public static BufferedImage captureScreen(Rectangle area) throws Exception {
        ////system.out.println("Capturing screen area: " + area);
        Robot robot = new Robot();
        return robot.createScreenCapture(area);
    }

    public static CompletableFuture<Rectangle> selectScreenArea(LanguageSelectionBar langBar) {
        ////system.out.println("Opening screen selection window...");
        CompletableFuture<Rectangle> future = new CompletableFuture<>();
        final Rectangle[] selectedRect = {null};

        JWindow overlayWindow = new JWindow();
        overlayWindow.setAlwaysOnTop(true);
        overlayWindow.setOpacity(0.3f);
        overlayWindow.setBackground(Color.black);
        overlayWindow.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        overlayWindow.setFocusableWindowState(true);

        // Add a cancel button to the LanguageSelectionBar panel
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            ////system.out.println("Cancel button clicked, closing selection window");
            overlayWindow.setVisible(false);
            langBar.setVisible(false);
            future.complete(null);
        });
        langBar.add(cancelButton);

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
                            ////system.out.println("Mouse pressed in LanguageSelectionBar, ignoring");
                            return;
                        }
                        start = e.getPoint();
                        dragging = true;
                        ////system.out.println("Mouse pressed at: " + start);
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        Point mouse = e.getLocationOnScreen();
                        Rectangle langBounds = langBar.getBoundsOnScreen();
                        if (langBounds.contains(mouse)) {
                            ////system.out.println("Mouse released in LanguageSelectionBar, ignoring");
                            return;
                        }
                        if (!dragging) return;
                        dragging = false;

                        end = e.getPoint();
                        int x = Math.min(start.x, end.x);
                        int y = Math.min(start.y, end.y);
                        int width = Math.abs(start.x - end.x);
                        int height = Math.abs(start.y - end.y);
                        selectedRect[0] = new Rectangle(x, y, width, height);
                        ////system.out.println("Mouse released, selected area: " + selectedRect[0]);

                        overlayWindow.setVisible(false);
                        langBar.setVisible(false);
                        future.complete(selectedRect[0]);
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        Point mouse = e.getLocationOnScreen();
                        Rectangle langBounds = langBar.getBoundsOnScreen();
                        if (langBounds.contains(mouse)) {
                            ////system.out.println("Mouse clicked in LanguageSelectionBar, ignoring");
                            return;
                        }
                    }
                });

                addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        Point mouse = e.getLocationOnScreen();
                        Rectangle langBounds = langBar.getBoundsOnScreen();
                        if (langBounds.contains(mouse)) {
                            ////system.out.println("Mouse dragged in LanguageSelectionBar, ignoring");
                            return;
                        }
                        if (!dragging) return;
                        end = e.getPoint();
                        ////system.out.println("Mouse dragged to: " + end);
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (start != null && end != null) {
                    int x = Math.min(start.x, end.x);
                    int y = Math.min(start.y, end.y);
                    int width = Math.abs(start.x - end.x);
                    int height = Math.abs(start.y - end.y);
                    g.setColor(Color.white);
                    g.drawRect(x, y, width, height);
                    ////system.out.println("Drawing selection rectangle: " + new Rectangle(x, y, width, height));
                }
            }
        };

        overlayWindow.setContentPane(glass);
        overlayWindow.add(langBar);
        langBar.setBounds(50, 10, 350, 40); // Adjusted width for cancel button
        ////system.out.println("Setting LanguageSelectionBar bounds: 50, 10, 350, 40");
        overlayWindow.setVisible(true);
        ////system.out.println("Screen selection window visible");
        overlayWindow.setComponentZOrder(langBar, 0);
        overlayWindow.setComponentZOrder(glass, 1);

        return future;
    }

    private File extractTessdataToTemp() throws IOException {
        System.out.println("Extracting tessdata to temporary directory...");
        // Create a temporary directory
        Path tempDir = Files.createTempDirectory("ocrtranslator-tessdata");
        File tempTessdataDir = tempDir.toFile();

        // List of expected .traineddata files (from LanguageMap)
        String[] languages = LanguageMap.getSupportedLanguages();
        for (String lang : languages) {
            String tessCode = LanguageMap.getTesseractCode(lang);
            String resourcePath = "/tessdata/" + tessCode + ".traineddata";
            URL resourceUrl = getClass().getResource(resourcePath);
            if (resourceUrl == null) {
                System.err.println("Tesseract data file not found in JAR: " + resourcePath);
                continue; // Skip missing files
            }

            // Extract the .traineddata file to the temporary directory
            File outputFile = new File(tempTessdataDir, tessCode + ".traineddata");
            try (InputStream in = resourceUrl.openStream();
                 OutputStream out = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                System.out.println("Extracted: " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error extracting " + resourcePath + ": " + e.getMessage());
                System.out.println(e.getMessage());
            }
        }

        if (tempTessdataDir.listFiles() == null || tempTessdataDir.listFiles().length == 0) {
            System.err.println("No tessdata files extracted to: " + tempTessdataDir.getAbsolutePath());
            return null;
        }
//        System.out.println("tessdata extracted to: " + tempTessdataDir.getAbsolutePath());
        return tempTessdataDir;
    }
}