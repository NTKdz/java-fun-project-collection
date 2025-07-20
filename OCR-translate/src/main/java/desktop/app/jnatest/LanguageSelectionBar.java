package desktop.app.jnatest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class LanguageSelectionBar extends JPanel {
    private final JComboBox<String> sourceLang;
    private final JComboBox<String> targetLang;

    public LanguageSelectionBar(ActionListener swapListener) {
        setLayout(new FlowLayout());
        setOpaque(true);
        setBackground(new Color(255, 255, 255, 240)); // Semi-transparent white

        sourceLang = new JComboBox<>(LanguageMap.tessLangMap.keySet().toArray(new String[0]));
        targetLang = new JComboBox<>(LanguageMap.tessLangMap.keySet().toArray(new String[0]));

        // Force heavyweight popups
        sourceLang.setLightWeightPopupEnabled(false);
        targetLang.setLightWeightPopupEnabled(false);

        sourceLang.setSelectedItem("English");
        targetLang.setSelectedItem("Japanese");

        JButton swapBtn = new JButton("⟷");
        swapBtn.addActionListener(swapListener);

        add(new JLabel("From:"));
        add(sourceLang);
        add(swapBtn);
        add(new JLabel("To:"));
        add(targetLang);
    }

    public String getSourceLanguage() {
        return (String) sourceLang.getSelectedItem();
    }

    public String getTargetLanguage() {
        return (String) targetLang.getSelectedItem();
    }

    public void swapLanguages() {
        String temp = getSourceLanguage();
        sourceLang.setSelectedItem(getTargetLanguage());
        targetLang.setSelectedItem(temp);
    }

    public Rectangle getBoundsOnScreen() {
        Point location = getLocationOnScreen();
        return new Rectangle(location.x, location.y, getWidth(), getHeight());
    }
}