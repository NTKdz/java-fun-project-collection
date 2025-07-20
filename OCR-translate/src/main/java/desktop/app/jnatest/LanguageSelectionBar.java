package desktop.app.jnatest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Properties;

public class LanguageSelectionBar extends JPanel {
    private JComboBox<String> sourceLangComboBox;
    private JComboBox<String> targetLangComboBox;
    private Properties config;
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".ocrtranslator";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.properties";

    public LanguageSelectionBar(ActionListener swapListener) {
        System.out.println("Initializing LanguageSelectionBar...");
        setLayout(new FlowLayout());
        config = new Properties();

        String[] defaultLanguages = {"English"};
        try {
            defaultLanguages = LanguageMap.getSupportedLanguages();
            System.out.println("Supported languages loaded: " + defaultLanguages.length);
        } catch (Exception e) {
            System.err.println("Error loading supported languages from LanguageMap: " + e.getMessage());
            System.out.println(e.getMessage());
        }

        sourceLangComboBox = new JComboBox<>(defaultLanguages);
        targetLangComboBox = new JComboBox<>(defaultLanguages);

        sourceLangComboBox.setLightWeightPopupEnabled(false);
        targetLangComboBox.setLightWeightPopupEnabled(false);

        try {
            loadConfig();
            String sourceLang = config.getProperty("sourceLang", "English");
            String targetLang = config.getProperty("targetLang", "Japanese");
            sourceLangComboBox.setSelectedItem(sourceLang);
            targetLangComboBox.setSelectedItem(targetLang);
            System.out.println("Initial sourceLang: " + sourceLang + ", targetLang: " + targetLang);
        } catch (Exception e) {
            System.err.println("Error during config loading: " + e.getMessage());
            System.out.println("Using default languages due to error: " + e.getMessage());
            sourceLangComboBox.setSelectedItem("English");
            targetLangComboBox.setSelectedItem("Japanese");
        }

        JButton swapButton = new JButton("Swap");
        swapButton.addActionListener(swapListener);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            System.out.println("Cancel button clicked in LanguageSelectionBar");
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
            }
        });

        add(new JLabel("Source:"));
        add(sourceLangComboBox);
        add(swapButton);
        add(new JLabel("Target:"));
        add(targetLangComboBox);
        add(cancelButton);

        sourceLangComboBox.addActionListener(e -> {
            System.out.println("Source language changed to: " + sourceLangComboBox.getSelectedItem());
            saveConfig();
        });
        targetLangComboBox.addActionListener(e -> {
            System.out.println("Target language changed to: " + targetLangComboBox.getSelectedItem());
            saveConfig();
        });

        System.out.println("LanguageSelectionBar initialized");
    }

    public String getSourceLanguage() {
        Object selected = sourceLangComboBox.getSelectedItem();
        return selected != null ? selected.toString() : "English";
    }

    public String getTargetLanguage() {
        Object selected = targetLangComboBox.getSelectedItem();
        return selected != null ? selected.toString() : "Japanese";
    }

    public void swapLanguages() {
        System.out.println("Swapping languages...");
        Object temp = sourceLangComboBox.getSelectedItem();
        sourceLangComboBox.setSelectedItem(targetLangComboBox.getSelectedItem());
        targetLangComboBox.setSelectedItem(temp);
        saveConfig();
    }

    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        System.out.println("Attempting to load config from: " + configFile.getAbsolutePath());

        // Try loading from the user directory first
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                config.load(input);
                System.out.println("Config loaded from file: sourceLang=" + config.getProperty("sourceLang") +
                        ", targetLang=" + config.getProperty("targetLang"));
                return;
            } catch (IOException e) {
                System.err.println("Error loading config from " + configFile.getAbsolutePath() + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Config file not found, trying default classpath resource");
        }

        // Fallback to classpath resource from JAR
        try (InputStream input = getClass().getResourceAsStream("/desktop/app/jnatest/config.properties")) {
            if (input != null) {
                config.load(input);
                System.out.println("Config loaded from classpath: sourceLang=" + config.getProperty("sourceLang") +
                        ", targetLang=" + config.getProperty("targetLang"));
            } else {
                System.out.println("Classpath config /desktop/app/jnatest/config.properties not found, using defaults");
                config.setProperty("sourceLang", "English");
                config.setProperty("targetLang", "Japanese");
            }
        } catch (IOException e) {
            System.err.println("Error loading config from classpath: " + e.getMessage());
            e.printStackTrace();
            config.setProperty("sourceLang", "English");
            config.setProperty("targetLang", "Japanese");
        }
    }

    private void saveConfig() {
        String sourceLang = getSourceLanguage();
        String targetLang = getTargetLanguage();
        config.setProperty("sourceLang", sourceLang);
        config.setProperty("targetLang", targetLang);
        System.out.println("Saving config: sourceLang=" + sourceLang + ", targetLang=" + targetLang);

        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            boolean created = configDir.mkdirs();
            System.out.println("Config directory created: " + created);
        }

        File configFile = new File(CONFIG_FILE);
        try (OutputStream output = new FileOutputStream(configFile)) {
            config.store(output, "OCR Translator Language Settings");
            System.out.println("Config saved to: " + configFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving config to " + configFile.getAbsolutePath() + ": " + e.getMessage());
            System.out.println(e.getMessage());
            throw new RuntimeException("Failed to save configuration", e);
        }
    }

    public Rectangle getBoundsOnScreen() {
        Point location = getLocationOnScreen();
        return new Rectangle(location.x, location.y, getWidth(), getHeight());
    }
}