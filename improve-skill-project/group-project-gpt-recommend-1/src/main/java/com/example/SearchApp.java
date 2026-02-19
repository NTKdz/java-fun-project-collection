package com.example;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Base64;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Main Swing application.
 * Two tabs: Search and Settings.
 */
public class SearchApp extends JFrame {

    // ---- Colours & fonts (easy to tweak) ----
    private static final Color CLR_BG         = new Color(22, 22, 34);
    private static final Color CLR_PANEL      = new Color(30, 30, 46);
    private static final Color CLR_CARD       = new Color(40, 40, 58);
    private static final Color CLR_ACCENT     = new Color(99, 102, 241);
    private static final Color CLR_ACCENT2    = new Color(139, 92, 246);
    private static final Color CLR_TEXT       = new Color(225, 225, 235);
    private static final Color CLR_TEXT_DIM   = new Color(160, 160, 185);
    private static final Color CLR_SUCCESS    = new Color(52, 211, 153);
    private static final Color CLR_WARN       = new Color(251, 191, 36);
    private static final Color CLR_ERROR      = new Color(248, 113, 113);
    private static final Color CLR_BAR_BG     = new Color(55, 55, 78);
    private static final Color CLR_INPUT_BG   = new Color(15, 15, 25);
    private static final Color CLR_ROW_ALT    = new Color(35, 35, 52);
    private static final Color CLR_ROW_HOV    = new Color(55, 55, 80);
    private static final Color CLR_BORDER     = new Color(65, 65, 92);

    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_MONO   = new Font("Consolas", Font.PLAIN, 12);

    // App icon — magnifying glass, embedded as base64 PNG so no external file needed
    private static final String ICON_B64 =
            "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAB4UlEQVR4nO2azU3EQAxGPygAOuFKGQgoAYmikCgBEGVwpQPue4EG4BQpiuKMx+Nv/vC77mZiv7G9O5sFgiAIgiD4r5wxFn14/P5Nvef56ZJy71xcg9AkvqW1CJebWxLf0kpE0U09Et9SW8S59UJG8sx1JUy2NUHe3l98SK+9vfxcp66vVQnZNzlK/ihpiSMZNSRktYB38qnrarSDeQassSbvdX0JagHSbngFL63DroKiCvDeuRaVoBKwtwusYPfWZVaBywwYmaSA2l9MJFhxmCqA3as1Z0G0QOsAWhMCWgfQmhDQOoDWmARozvMlsNdfkxTQ+kfLBVYc0QKaN+3ZZ5Xp3rrMKiyqAG8JNXt/QS1A2gWvoKV12DPIZQaUSpCuP52+SpZVkSXgaDesElLJ39x9Uo/jXT4X2Nv599crSiuYF2X9QHFU9gwJ5hnAGE6pnme0Q3dPhzVJelZCl/8PqCmh23+I1JLQxUFHooaErg9DmuRKB2PXAgC+hO4FAFwJQwgAeBKGEQBwJAwlAPCXMJwAwFfCkAIAPwnDCgB8JAwtACiX0PVX4Ry0Pb8VNo0AIG/6LyKGb4E1loPRVAIAvYSlWqYTAORVwpQCcphWQKoKphyCFqb6GJRYfzyyHrAMyx8Dee8ZcGtymwAAAABJRU5ErkJggg==";

    // ---- State ----
    private final ConfigManager config;
    private final SearchEngine  engine;

    // ---- Search tab components ----
    private JTextField     searchField;
    private JButton        searchBtn;
    private JLabel         resultCountLabel;
    private SearchEngine.SearchMode searchMode = SearchEngine.SearchMode.ALL;
    private JTable         resultTable;
    private ResultModel    resultModel;
    private JLabel         statusLabel;

    // ---- Index tab components ----
    private JProgressBar   indexProgress;
    private JLabel         indexProgressLabel;
    private JLabel         indexStatusLabel;
    private JButton        indexBtn;
    private JButton        cancelBtn;
    private JTextArea      indexLog;
    private JLabel         docCountLabel;

    // ---- Settings tab components ----
    private JTextArea      foldersArea;
    private JTextArea      skipArea;
    private JTextField     indexDirField;
    private JTextField     maxResultsField;

    // ---- Index worker ----
    private SwingWorker<Void, String> indexWorker;
    private final AtomicBoolean       cancelFlag     = new AtomicBoolean(false);

    // ---- Index tab extra components ----
    private JList<String>    folderList;
    private DefaultListModel<String> folderListModel;
    private JLabel           indexModeLabel;

    // ========== ENTRY POINT ==========

    /** Decode the embedded base64 PNG and return it as an ImageIcon. */
    private static ImageIcon loadAppIcon(int size) {
        try {
            byte[] bytes = Base64.getDecoder().decode(ICON_B64);
            ImageIcon raw = new ImageIcon(bytes);
            // Scale to requested size with smooth interpolation
            java.awt.Image scaled = raw.getImage()
                    .getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        // FlatLaf dark theme — handles all Swing color overrides correctly on Windows
        try { UIManager.setLookAndFeel(new FlatDarkLaf()); }
        catch (Exception ignored) {}

        // Global overrides on top of FlatLaf
        UIManager.put("Panel.background",              CLR_BG);
        UIManager.put("ScrollPane.background",         CLR_BG);
        UIManager.put("Viewport.background",           CLR_BG);
        UIManager.put("TabbedPane.background",         CLR_PANEL);
        UIManager.put("TabbedPane.selectedBackground", CLR_CARD);
        UIManager.put("TabbedPane.foreground",         CLR_TEXT);
        UIManager.put("TabbedPane.selectedForeground", CLR_TEXT);
        UIManager.put("TabbedPane.focusColor",         CLR_CARD);
        UIManager.put("TabbedPane.hoverColor",         CLR_CARD);
        UIManager.put("TabbedPane.underlineColor",     CLR_ACCENT);
        UIManager.put("TableHeader.background",        CLR_PANEL);
        UIManager.put("TableHeader.foreground",        CLR_TEXT_DIM);
        UIManager.put("Table.background",              CLR_CARD);
        UIManager.put("Table.foreground",              CLR_TEXT);
        UIManager.put("Table.selectionBackground",     CLR_ROW_HOV);
        UIManager.put("Table.selectionForeground",     CLR_TEXT);
        UIManager.put("Table.gridColor",               CLR_BORDER);
        UIManager.put("TextField.background",          CLR_INPUT_BG);
        UIManager.put("TextField.foreground",          CLR_TEXT);
        UIManager.put("TextField.caretForeground",     CLR_TEXT);
        UIManager.put("TextArea.background",           CLR_INPUT_BG);
        UIManager.put("TextArea.foreground",           CLR_TEXT);
        UIManager.put("TextArea.caretForeground",      CLR_TEXT);
        UIManager.put("ScrollBar.background",          CLR_PANEL);
        UIManager.put("ScrollBar.thumb",               CLR_BORDER);
        UIManager.put("PopupMenu.background",          CLR_CARD);
        UIManager.put("PopupMenu.foreground",          CLR_TEXT);
        UIManager.put("MenuItem.background",           CLR_CARD);
        UIManager.put("MenuItem.foreground",           CLR_TEXT);
        UIManager.put("MenuItem.selectionBackground",  CLR_ACCENT);
        UIManager.put("MenuItem.selectionForeground",  Color.WHITE);
        UIManager.put("Separator.foreground",          CLR_BORDER);

        SwingUtilities.invokeLater(() -> {
            try {
                ConfigManager config = new ConfigManager();
                SearchEngine  engine = new SearchEngine(config);
                new SearchApp(config, engine).setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Failed to initialise search engine:\n" + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    // ========== CONSTRUCTOR ==========

    public SearchApp(ConfigManager config, SearchEngine engine) {
        this.config = config;
        this.engine = engine;

        setTitle("File Search Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 700);
        setMinimumSize(new Dimension(750, 500));
        setLocationRelativeTo(null);

        // Set window icon (taskbar + title bar)
        ImageIcon appIcon = loadAppIcon(64);
        if (appIcon != null) setIconImage(appIcon.getImage());

        // Dark background for the whole frame
        getContentPane().setBackground(CLR_BG);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CLR_BG);

        // ---- Header ----
        root.add(buildHeader(), BorderLayout.NORTH);

        // ---- Tabbed pane ----
        JTabbedPane tabs = buildTabs();
        root.add(tabs, BorderLayout.CENTER);

        setContentPane(root);

        // ---- Status bar ----
        JPanel statusBar = buildStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        // ---- Window close ----
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                cancelFlag.set(true);
                engine.close();
            }
        });

        refreshDocCount();
    }

    // ========== HEADER ==========

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CLR_PANEL);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, CLR_BORDER),
                new EmptyBorder(14, 20, 14, 20)));

        JLabel title = new JLabel("  File Search Engine");
        ImageIcon headerIcon = loadAppIcon(22);
        if (headerIcon != null) title.setIcon(headerIcon);
        title.setFont(FONT_TITLE);
        title.setForeground(CLR_TEXT);

        docCountLabel = new JLabel("0 files indexed");
        docCountLabel.setFont(FONT_SMALL);
        docCountLabel.setForeground(CLR_TEXT_DIM);

        header.add(title,         BorderLayout.WEST);
        header.add(docCountLabel, BorderLayout.EAST);
        return header;
    }

    // ========== TABS ==========

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        styleTabPane(tabs);

        tabs.addTab("  Search  ",  buildSearchTab());
        tabs.addTab("  Index   ",  buildIndexTab());
        tabs.addTab("  Settings ", buildSettingsTab());

        return tabs;
    }

    // ========== SEARCH TAB ==========

    private JPanel buildSearchTab() {
        JPanel panel = darkPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // ---- Search bar ----
        JPanel searchBar = darkPanel(new BorderLayout(8, 0));

        searchField = new JTextField();
        styleInput(searchField);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        searchField.putClientProperty("JTextField.placeholderText", "Search files…");
        searchField.addActionListener(e -> doSearch());

        searchBtn = accentButton("Search");
        searchBtn.addActionListener(e -> doSearch());

        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(searchBtn,   BorderLayout.EAST);

        // ---- Search mode toggle ----
        JPanel modeRow = darkPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        JLabel modeLabel = new JLabel("Search in: ");
        modeLabel.setFont(FONT_SMALL);
        modeLabel.setForeground(CLR_TEXT_DIM);
        modeRow.add(modeLabel);

        ButtonGroup modeGroup = new ButtonGroup();
        for (SearchEngine.SearchMode mode : SearchEngine.SearchMode.values()) {
            JToggleButton btn = new JToggleButton(mode.label);
            btn.setFont(FONT_SMALL);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setSelected(mode == SearchEngine.SearchMode.ALL);

            // Style: selected = accent fill, unselected = dim outline
            Runnable updateStyle = () -> {
                if (btn.isSelected()) {
                    btn.setBackground(CLR_ACCENT);
                    btn.setForeground(Color.WHITE);
                    btn.setOpaque(true);
                    btn.setBorderPainted(false);
                } else {
                    btn.setBackground(CLR_PANEL);
                    btn.setForeground(CLR_TEXT_DIM);
                    btn.setOpaque(true);
                    btn.setBorderPainted(true);
                    btn.setBorder(BorderFactory.createLineBorder(CLR_BORDER));
                }
            };
            updateStyle.run();

            btn.addItemListener(e -> {
                updateStyle.run();
                if (btn.isSelected()) {
                    searchMode = mode;
                    // Re-run search immediately if there is an active query
                    if (!searchField.getText().isBlank()) doSearch();
                }
            });

            // Left button: rounded left corners; right button: rounded right corners
            btn.setBorder(new EmptyBorder(4, 12, 4, 12));
            modeGroup.add(btn);
            modeRow.add(btn);
        }

        // ---- Results count label ----
        resultCountLabel = new JLabel("Enter a query to search your indexed files.");
        resultCountLabel.setFont(FONT_SMALL);
        resultCountLabel.setForeground(CLR_TEXT_DIM);

        JPanel topSection = darkPanel(new BorderLayout(0, 6));
        topSection.add(searchBar,        BorderLayout.NORTH);
        topSection.add(modeRow,          BorderLayout.CENTER);
        topSection.add(resultCountLabel, BorderLayout.SOUTH);

        // ---- Results table ----
        resultModel = new ResultModel();
        resultTable = new JTable(resultModel);
        styleTable(resultTable);

        // Double-click opens folder
        resultTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelectedFile();
            }
        });

        // Right-click context menu
        JPopupMenu popup = new JPopupMenu();
        stylePopup(popup);
        JMenuItem openFile   = menuItem("Open File");
        JMenuItem openFolder = menuItem("Open Containing Folder");
        JMenuItem copyPath   = menuItem("Copy Path");
        openFile.addActionListener   (e -> openSelectedFile());
        openFolder.addActionListener (e -> openSelectedFolder());
        copyPath.addActionListener   (e -> copySelectedPath());
        popup.add(openFile);
        popup.add(openFolder);
        popup.add(new JSeparator());
        popup.add(copyPath);
        resultTable.setComponentPopupMenu(popup);

        JScrollPane scroll = darkScroll(resultTable);

        // ---- Query tips ----
        JLabel tips = new JLabel(
                "Tips:  exact phrase: \"hello world\"   fuzzy: config~1   wildcard: conf*   boolean: error AND NOT debug");
        tips.setFont(FONT_SMALL);
        tips.setForeground(CLR_TEXT_DIM);
        tips.setBorder(new EmptyBorder(4, 0, 0, 0));

        panel.add(topSection,   BorderLayout.NORTH);
        panel.add(scroll,       BorderLayout.CENTER);
        panel.add(tips,         BorderLayout.SOUTH);

        return panel;
    }

    private void doSearch() {
        String q = searchField.getText().trim();
        if (q.isBlank()) return;

        SearchEngine.SearchMode mode = searchMode; // capture at call time
        searchBtn.setEnabled(false);
        resultModel.clear();
        resultCountLabel.setText("Searching…");

        SwingWorker<List<SearchEngine.SearchResult>, Void> worker =
                new SwingWorker<>() {
                    @Override protected List<SearchEngine.SearchResult> doInBackground() throws Exception {
                        return engine.search(q, mode);
                    }
                    @Override protected void done() {
                        searchBtn.setEnabled(true);
                        try {
                            List<SearchEngine.SearchResult> results = get();
                            resultModel.setResults(results);
                            String modeStr = " [" + mode.label + "]";
                            if (results.isEmpty()) {
                                resultCountLabel.setText("No results found for: " + q + modeStr);
                            } else {
                                resultCountLabel.setText(results.size() + " result(s) for: " + q + modeStr);
                            }
                        } catch (Exception ex) {
                            resultCountLabel.setText("Query error: " + ex.getMessage());
                        }
                    }
                };
        worker.execute();
    }

    private void openSelectedFile() {
        int row = resultTable.getSelectedRow();
        if (row < 0) return;
        String path = resultModel.getPath(row);
        try { Desktop.getDesktop().open(new File(path)); }
        catch (IOException e) { showError("Cannot open file:\n" + e.getMessage()); }
    }

    private void openSelectedFolder() {
        int row = resultTable.getSelectedRow();
        if (row < 0) return;
        File parent = new File(resultModel.getPath(row)).getParentFile();
        try { Desktop.getDesktop().open(parent); }
        catch (IOException e) { showError("Cannot open folder:\n" + e.getMessage()); }
    }

    private void copySelectedPath() {
        int row = resultTable.getSelectedRow();
        if (row < 0) return;
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(resultModel.getPath(row)), null);
    }

    // ========== INDEX TAB ==========

    private JPanel buildIndexTab() {
        JPanel panel = darkPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        // ══════════════════════════════════════════
        //  TOP: folder list + action buttons
        // ══════════════════════════════════════════
        JPanel topPanel = darkPanel(new BorderLayout(10, 0));

        // ---- Left: Folder list ----
        folderListModel = new DefaultListModel<>();
        config.getList(ConfigManager.KEY_ROOT_FOLDERS).forEach(folderListModel::addElement);

        folderList = new JList<>(folderListModel);
        folderList.setBackground(CLR_INPUT_BG);
        folderList.setForeground(CLR_TEXT);
        folderList.setFont(FONT_MONO);
        folderList.setSelectionBackground(CLR_ACCENT);
        folderList.setSelectionForeground(Color.WHITE);
        folderList.setBorder(new EmptyBorder(6, 8, 6, 8));
        folderList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JScrollPane listScroll = darkScroll(folderList);
        listScroll.setBorder(roundedBorder());
        listScroll.setPreferredSize(new Dimension(260, 0));

        JLabel listLabel = new JLabel("Indexed Folders");
        listLabel.setFont(FONT_BOLD);
        listLabel.setForeground(CLR_TEXT);
        listLabel.setBorder(new EmptyBorder(0, 0, 6, 0));

        JButton addFolderBtn = smallButton("+ Add");
        JButton removeFolderBtn = smallButton("- Remove from index");
        JButton refreshListBtn  = smallButton("Reload list");

        addFolderBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setMultiSelectionEnabled(true);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                for (File f : chooser.getSelectedFiles()) {
                    String path = f.getAbsolutePath();
                    if (!folderListModel.contains(path))
                        folderListModel.addElement(path);
                }
                savefolderListToConfig();
            }
        });

        removeFolderBtn.addActionListener(e -> {
            List<String> selected = folderList.getSelectedValuesList();
            if (selected.isEmpty()) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove " + selected.size() + " folder(s) from the index?" +
                    "Their documents will be deleted from search results.",
                    "Confirm Remove", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            runIndexWorker("Removing folders from index…", () -> {
                for (String folder : selected) {
                    int n = engine.removeFolder(folder,
                            msg -> SwingUtilities.invokeLater(() -> appendLog(msg + "\n")));
                    SwingUtilities.invokeLater(() -> folderListModel.removeElement(folder));
                }
                savefolderListToConfig();
            });
        });

        refreshListBtn.addActionListener(e -> {
            folderListModel.clear();
            config.getList(ConfigManager.KEY_ROOT_FOLDERS).forEach(folderListModel::addElement);
        });

        JPanel listBtnRow = darkPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        listBtnRow.add(addFolderBtn);
        listBtnRow.add(removeFolderBtn);
        listBtnRow.add(refreshListBtn);

        JPanel leftPanel = darkPanel(new BorderLayout(0, 6));
        leftPanel.add(listLabel,  BorderLayout.NORTH);
        leftPanel.add(listScroll, BorderLayout.CENTER);
        leftPanel.add(listBtnRow, BorderLayout.SOUTH);

        // ---- Right: action buttons ----
        JPanel actionPanel = darkPanel(new GridBagLayout());
        actionPanel.setPreferredSize(new Dimension(300, 300));
        GridBagConstraints ga = new GridBagConstraints();
        ga.fill = GridBagConstraints.HORIZONTAL;
        ga.insets = new Insets(4, 0, 4, 0);
        ga.gridx = 0; ga.weightx = 1;

        JLabel actLabel = new JLabel("Actions");
        actLabel.setFont(FONT_BOLD);
        actLabel.setForeground(CLR_TEXT);
        ga.gridy = 0; actionPanel.add(actLabel, ga);

        // Full re-index button
        indexBtn = accentButton("Full Re-Index All Folders");
        indexBtn.setToolTipText("Wipe and rebuild the entire index from all folders");
        indexBtn.addActionListener(e -> startFullReindex());
        ga.gridy = 1; actionPanel.add(indexBtn, ga);

        // Update selected button
        JButton updateSelBtn = accentButton("Update Selected Folders");
        updateSelBtn.setBackground(new Color(30, 140, 100));
        updateSelBtn.setToolTipText(
                "Index only the selected folder(s). Adds new files, updates changed ones.\n" +
                        "Files in other folders are untouched.");
        updateSelBtn.addActionListener(e -> startUpdateSelected());
        ga.gridy = 2; actionPanel.add(updateSelBtn, ga);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(CLR_BORDER);
        ga.gridy = 3; actionPanel.add(sep, ga);

        // Mode label
        indexModeLabel = new JLabel("—");
        indexModeLabel.setFont(FONT_SMALL);
        indexModeLabel.setForeground(CLR_TEXT_DIM);
        ga.gridy = 4; actionPanel.add(indexModeLabel, ga);

        // Cancel button
        cancelBtn = new JButton("Cancel");
        styleSecondaryButton(cancelBtn);
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(e -> {
            cancelFlag.set(true);
            cancelBtn.setEnabled(false);
            indexStatusLabel.setText("Cancelling…");
        });
        ga.gridy = 5; actionPanel.add(cancelBtn, ga);

        // Spacer
        ga.gridy = 6; ga.weighty = 1;
        actionPanel.add(Box.createVerticalGlue(), ga);
        ga.weighty = 0;

        topPanel.add(leftPanel,   BorderLayout.CENTER);
        topPanel.add(actionPanel, BorderLayout.EAST);

        // ══════════════════════════════════════════
        //  MIDDLE: progress
        // ══════════════════════════════════════════
        JPanel progressCard = darkCard();
        progressCard.setLayout(new GridBagLayout());
        GridBagConstraints gp = new GridBagConstraints();
        gp.fill = GridBagConstraints.HORIZONTAL;
        gp.insets = new Insets(4, 8, 4, 8);
        gp.gridx = 0; gp.gridwidth = 2; gp.weightx = 1;

        indexProgress = new JProgressBar(0, 100);
        indexProgress.setStringPainted(false);
        indexProgress.setForeground(CLR_ACCENT);
        indexProgress.setBackground(CLR_BAR_BG);
        indexProgress.setBorderPainted(false);
        indexProgress.setPreferredSize(new Dimension(0, 16));
        gp.gridy = 0; progressCard.add(indexProgress, gp);

        indexProgressLabel = new JLabel("Not running");
        indexProgressLabel.setFont(FONT_BOLD);
        indexProgressLabel.setForeground(CLR_ACCENT);
        gp.gridy = 1; progressCard.add(indexProgressLabel, gp);

        indexStatusLabel = new JLabel(" ");
        indexStatusLabel.setFont(FONT_SMALL);
        indexStatusLabel.setForeground(CLR_TEXT_DIM);
        gp.gridy = 2; progressCard.add(indexStatusLabel, gp);

        // ══════════════════════════════════════════
        //  BOTTOM: log
        // ══════════════════════════════════════════
        indexLog = new JTextArea();
        indexLog.setEditable(false);
        indexLog.setFont(FONT_MONO);
        indexLog.setBackground(CLR_INPUT_BG);
        indexLog.setForeground(CLR_TEXT_DIM);
        indexLog.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane logScroll = darkScroll(indexLog);
        logScroll.setBorder(roundedBorder());

        JLabel logLabel = new JLabel("Log");
        logLabel.setFont(FONT_BOLD);
        logLabel.setForeground(CLR_TEXT);

        JPanel logPanel = darkPanel(new BorderLayout(0, 4));
        logPanel.add(logLabel,  BorderLayout.NORTH);
        logPanel.add(logScroll, BorderLayout.CENTER);

        // Assemble
        JPanel northSection = darkPanel(new BorderLayout(0, 10));
        northSection.add(topPanel,      BorderLayout.CENTER);
        northSection.add(progressCard,  BorderLayout.SOUTH);

        panel.add(northSection, BorderLayout.NORTH);
        panel.add(logPanel,     BorderLayout.CENTER);

        return panel;
    }

    private void savefolderListToConfig() {
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < folderListModel.size(); i++) paths.add(folderListModel.get(i));
        config.set(ConfigManager.KEY_ROOT_FOLDERS, String.join(",", paths));
        config.save();
    }

    private void startFullReindex() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "This will wipe and rebuild the entire index from scratch.\n" +
                        "All " + folderListModel.size() + " folder(s) will be re-indexed.\nContinue?",
                "Full Re-Index", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        indexModeLabel.setText("Mode: Full Re-Index");
        indexModeLabel.setForeground(CLR_WARN);
        runIndexWorker("Starting full re-index…", () ->
                engine.indexAll(makeProgressCallback(), makeStatusCallback(), cancelFlag));
    }

    private void startUpdateSelected() {
        List<String> selected = folderList.getSelectedValuesList();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Select one or more folders from the list first.",
                    "Nothing selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        indexModeLabel.setText("Mode: Update " + selected.size() + " folder(s)");
        indexModeLabel.setForeground(CLR_SUCCESS);
        runIndexWorker("Updating " + selected.size() + " selected folder(s)…", () ->
                engine.indexFolders(selected, makeProgressCallback(), makeStatusCallback(), cancelFlag));
    }

    /** Shared worker launcher — resets UI, runs task on background thread. */
    private void runIndexWorker(String startMsg, IndexTask task) {
        if (indexWorker != null && !indexWorker.isDone()) return;

        cancelFlag.set(false);
        indexBtn.setEnabled(false);
        cancelBtn.setEnabled(true);
        indexProgress.setValue(0);
        indexProgressLabel.setText("0%");
        indexProgressLabel.setForeground(CLR_ACCENT);
        indexLog.setText("");
        appendLog(startMsg + "\n");

        indexWorker = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                task.run();
                return null;
            }
            @Override protected void done() {
                SwingUtilities.invokeLater(() -> {
                    indexBtn.setEnabled(true);
                    cancelBtn.setEnabled(false);
                    if (!cancelFlag.get()) {
                        indexProgress.setValue(100);
                        indexProgressLabel.setText("100% — Done!");
                        indexProgressLabel.setForeground(CLR_SUCCESS);
                    } else {
                        indexProgressLabel.setForeground(CLR_WARN);
                    }
                    refreshDocCount();

                    try { get(); }
                    catch (Exception e) {
                        appendLog("Error: " + e.getMessage() + "\n");
                        indexProgressLabel.setForeground(CLR_ERROR);
                    }
                });
            }
        };
        indexWorker.execute();
    }

    /** Functional interface so index tasks can throw checked exceptions. */
    @FunctionalInterface
    interface IndexTask { void run() throws Exception; }

    private BiConsumer<Long, Long> makeProgressCallback() {
        return (done, total) -> SwingUtilities.invokeLater(() -> {
            if (total <= 0) {
                indexProgress.setIndeterminate(true);
                indexProgressLabel.setText("Counting…");
            } else {
                indexProgress.setIndeterminate(false);
                int pct = (int) Math.min(100, done * 100 / total);
                indexProgress.setValue(pct);
                indexProgressLabel.setText(pct + "%  (" + done + " / " + total + " files)");
            }
        });
    }

    private Consumer<String> makeStatusCallback() {
        return msg -> SwingUtilities.invokeLater(() -> {
            indexStatusLabel.setText(msg);
            appendLog(msg + "\n");
        });
    }

    private void appendLog(String msg) {
        indexLog.append(msg);
        indexLog.setCaretPosition(indexLog.getDocument().getLength());
    }

    // ========== SETTINGS TAB ==========

    private JPanel buildSettingsTab() {
        JPanel panel = darkPanel(new BorderLayout(0, 0));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel form = darkPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(6, 4, 6, 4);
        g.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;

        // ---- Folders to index ----
        row = addSettingLabel(form, g, row,
                "Folders to Index",
                "One path per line. e.g. C:\\ or D:\\Projects");
        foldersArea = settingsTextArea(String.join("\n", config.getList(ConfigManager.KEY_ROOT_FOLDERS)));
        row = addSettingArea(form, g, row, foldersArea, 4);

        // ---- Note ----
        g.gridx = 0; g.gridy = row++; g.gridwidth = 2; g.weightx = 1; g.weighty = 0;
        JLabel tikaNote = new JLabel(
                "<html><body style='color:#7BAAFF'>ℹ Apache Tika automatically detects and indexes 1,400+ file types " +
                        "(PDF, Word, Excel, PowerPoint, EPUB, emails, code, etc.) — no extension list needed.</body></html>");
        tikaNote.setFont(FONT_SMALL);
        tikaNote.setBorder(new EmptyBorder(4, 4, 8, 4));
        form.add(tikaNote, g);

        // ---- Skip folders ----
        row = addSettingLabel(form, g, row,
                "Skip These Folder Names",
                "Comma-separated names (not full paths). e.g. Windows, node_modules");
        skipArea = settingsTextArea(config.get(ConfigManager.KEY_SKIP_FOLDERS));
        row = addSettingArea(form, g, row, skipArea, 3);

        // ---- Index dir ----
        row = addSettingLabel(form, g, row,
                "Index Storage Location",
                "Where Lucene saves the index data.");
        indexDirField = settingsTextField(config.get(ConfigManager.KEY_INDEX_DIR));
        JButton browseBtn = smallButton("Browse…");
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(indexDirField.getText());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                indexDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        JPanel dirRow = darkPanel(new BorderLayout(6, 0));
        dirRow.add(indexDirField, BorderLayout.CENTER);
        dirRow.add(browseBtn,     BorderLayout.EAST);
        g.gridx = 0; g.gridy = row++; g.gridwidth = 2; g.weightx = 1; g.weighty = 0;
        form.add(dirRow, g);

        // ---- Max results ----
        row = addSettingLabel(form, g, row, "Max Results", "How many results to show per search.");
        maxResultsField = settingsTextField(config.get(ConfigManager.KEY_MAX_RESULTS));
        maxResultsField.setPreferredSize(new Dimension(80, 28));
        g.gridx = 0; g.gridy = row++; g.gridwidth = 1; g.weightx = 0; g.weighty = 0;
        form.add(maxResultsField, g);

        // ---- Save button ----
        JButton saveBtn = accentButton("Save Settings");
        saveBtn.addActionListener(e -> saveSettings());
        g.gridx = 0; g.gridy = row; g.gridwidth = 2; g.weighty = 1;
        g.anchor = GridBagConstraints.SOUTH;
        JPanel btnRow = darkPanel(new FlowLayout(FlowLayout.LEFT));
        btnRow.add(saveBtn);
        form.add(btnRow, g);

        JScrollPane scroll = darkScroll(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void saveSettings() {
        // Normalize folders: one per line → comma-separated
        String folders = Arrays.stream(foldersArea.getText().split("\\n"))
                .map(String::trim).filter(s -> !s.isBlank())
                .reduce((a, b) -> a + "," + b).orElse("");

        config.set(ConfigManager.KEY_ROOT_FOLDERS, folders);
        config.set(ConfigManager.KEY_SKIP_FOLDERS, skipArea.getText().trim());
        config.set(ConfigManager.KEY_INDEX_DIR,    indexDirField.getText().trim());
        config.set(ConfigManager.KEY_MAX_RESULTS,  maxResultsField.getText().trim());
        config.save();

        JOptionPane.showMessageDialog(this,
                "Settings saved!\nChanges take effect on the next index run.",
                "Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    // ========== SETTINGS FORM HELPERS ==========

    private int addSettingLabel(JPanel form, GridBagConstraints g,
                                int row, String label, String hint) {
        g.gridx = 0; g.gridy = row++; g.gridwidth = 2; g.weightx = 1; g.weighty = 0;
        JPanel lp = darkPanel(new BorderLayout(0, 2));
        JLabel l = new JLabel(label);
        l.setFont(FONT_BOLD);
        l.setForeground(CLR_TEXT);
        JLabel h = new JLabel(hint);
        h.setFont(FONT_SMALL);
        h.setForeground(CLR_TEXT_DIM);
        lp.add(l, BorderLayout.NORTH);
        lp.add(h, BorderLayout.SOUTH);
        form.add(lp, g);
        return row;
    }

    private int addSettingArea(JPanel form, GridBagConstraints g,
                               int row, JTextArea area, int rows) {
        g.gridx = 0; g.gridy = row++; g.gridwidth = 2; g.weightx = 1; g.weighty = 0;
        area.setRows(rows);
        JScrollPane sp = darkScroll(area);
        sp.setBorder(roundedBorder());
        form.add(sp, g);
        return row;
    }

    // ========== STATUS BAR ==========

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CLR_PANEL);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, CLR_BORDER),
                new EmptyBorder(5, 14, 5, 14)));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(CLR_TEXT_DIM);

        JLabel hint = new JLabel("Double-click a result to open · Right-click for options");
        hint.setFont(FONT_SMALL);
        hint.setForeground(CLR_TEXT_DIM);

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(hint,        BorderLayout.EAST);
        return bar;
    }

    // ========== RESULT TABLE MODEL ==========

    private static class ResultModel extends AbstractTableModel {
        private static final String[] COLS = { "#", "Type", "Filename", "Relevance", "Size", "Path" };
        private final List<SearchEngine.SearchResult> data = new ArrayList<>();

        void setResults(List<SearchEngine.SearchResult> results) {
            data.clear(); data.addAll(results); fireTableDataChanged();
        }
        void clear() { data.clear(); fireTableDataChanged(); }
        String getPath(int row) { return data.get(row).path(); }

        @Override public int getRowCount()    { return data.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int col) { return COLS[col]; }

        @Override public Object getValueAt(int row, int col) {
            SearchEngine.SearchResult r = data.get(row);
            return switch (col) {
                case 0 -> row + 1;
                case 1 -> r.fileType();
                case 2 -> r.filename();
                case 3 -> r;           // RelevanceRenderer
                case 4 -> formatSize(r.size());
                case 5 -> r.path();
                default -> "";
            };
        }

        @Override public Class<?> getColumnClass(int col) {
            return col == 0 ? Integer.class : Object.class;
        }

        private static String formatSize(long bytes) {
            if (bytes < 1024)        return bytes + " B";
            if (bytes < 1024 * 1024) return new DecimalFormat("0.0").format(bytes / 1024.0) + " KB";
            return new DecimalFormat("0.0").format(bytes / (1024.0 * 1024)) + " MB";
        }
    }

    // ========== RELEVANCE BAR RENDERER ==========

    private static class RelevanceRenderer extends JPanel implements TableCellRenderer {
        private float score, maxScore;

        RelevanceRenderer() { setOpaque(true); }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focused, int row, int col) {
            if (value instanceof SearchEngine.SearchResult r) {
                score = r.score(); maxScore = r.maxScore();
            }
            setBackground(selected ? CLR_ROW_HOV : (row % 2 == 0 ? CLR_CARD : CLR_ROW_ALT));
            return this;
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 50, h = 10;
            int x = 8, y = (getHeight() - h) / 2;

            // Background track
            g.setColor(CLR_BAR_BG);
            g.fillRoundRect(x, y, w, h, h, h);

            // Fill
            float ratio = maxScore > 0 ? Math.min(1f, score / maxScore) : 0f;
            int filled = (int) (w * ratio);
            Color barColor = ratio >= 0.8f ? CLR_SUCCESS : ratio >= 0.5f ? CLR_WARN : CLR_ERROR;
            g.setColor(barColor);
            if (filled > 0) g.fillRoundRect(x, y, filled, h, h, h);

            // Percentage text
            g.setColor(CLR_TEXT);
            g.setFont(FONT_SMALL);
            String pct = (int)(ratio * 100) + "%";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(pct, x + w + 4, y + h - 1);
        }
    }

    // ========== TABLE STYLING ==========

    private void styleTable(JTable table) {
        table.setBackground(CLR_CARD);
        table.setForeground(CLR_TEXT);
        table.setFont(FONT_LABEL);
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 2));
        table.setSelectionBackground(CLR_ROW_HOV);
        table.setSelectionForeground(CLR_TEXT);
        table.setFillsViewportHeight(true);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setBackground(CLR_PANEL);
        header.setForeground(CLR_TEXT_DIM);
        header.setFont(FONT_BOLD);
        header.setBorder(new MatteBorder(0, 0, 1, 0, CLR_BORDER));

        // Column widths
        TableColumnModel cols = table.getColumnModel();
        cols.getColumn(0).setPreferredWidth(30);  cols.getColumn(0).setMaxWidth(40);  // #
        cols.getColumn(1).setPreferredWidth(55);  cols.getColumn(1).setMaxWidth(70);  // Type
        cols.getColumn(2).setPreferredWidth(200);                                     // Filename
        cols.getColumn(3).setPreferredWidth(155);                                     // Relevance
        cols.getColumn(4).setPreferredWidth(75);                                      // Size
        cols.getColumn(5).setPreferredWidth(380);                                     // Path

        // Relevance bar renderer on column 3
        cols.getColumn(3).setCellRenderer(new RelevanceRenderer());

        // Type badge renderer — coloured pill based on file type
        cols.getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String type = val != null ? val.toString() : "";
                setText(type);
                setHorizontalAlignment(CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 10));
                setOpaque(true);
                setBackground(sel ? CLR_ROW_HOV : typeBadgeColor(type));
                setForeground(Color.WHITE);
                setBorder(new EmptyBorder(4, 4, 4, 4));
                return this;
            }
            private Color typeBadgeColor(String type) {
                return switch (type) {
                    case "PDF"  -> new Color(220, 50, 50);
                    case "DOCX","DOC","ODT","RTF","WPD" -> new Color(30, 100, 200);
                    case "XLSX","XLS","ODS","CSV"        -> new Color(30, 150, 80);
                    case "PPTX","PPT","ODP"              -> new Color(200, 100, 30);
                    case "EPUB","FB2"                    -> new Color(130, 60, 200);
                    case "EML","MSG"                     -> new Color(80, 150, 200);
                    case "HTML","XML"                    -> new Color(180, 120, 30);
                    case "JSON","YAML"                   -> new Color(100, 160, 60);
                    case "JAVA","PY","JS","TS","CS",
                         "GO","RS","CPP","C","KT","PHP" -> new Color(80, 80, 180);
                    default -> new Color(90, 90, 110);
                };
            }
        });

        // Default renderer for other columns
        DefaultTableCellRenderer baseRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(sel ? CLR_ROW_HOV : (row % 2 == 0 ? CLR_CARD : CLR_ROW_ALT));
                setForeground(col == 5 ? CLR_TEXT_DIM : CLR_TEXT);
                setFont(col == 5 ? FONT_SMALL : FONT_LABEL);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        };
        for (int i : new int[]{0, 2, 4, 5}) {
            cols.getColumn(i).setCellRenderer(baseRenderer);
        }
    }

    // ========== WIDGET FACTORIES ==========

    private JPanel darkPanel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(CLR_BG);
        return p;
    }

    private JPanel darkPanel() { return darkPanel(new FlowLayout()); }

    private JPanel darkCard() {
        JPanel p = new JPanel();
        p.setBackground(CLR_CARD);
        p.setBorder(new CompoundBorder(roundedBorder(), new EmptyBorder(12, 14, 12, 14)));
        return p;
    }

    private JScrollPane darkScroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBackground(CLR_BG);
        sp.getViewport().setBackground(c.getBackground());
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setBackground(CLR_PANEL);
        return sp;
    }

    private JButton accentButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(CLR_ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(CLR_ACCENT2); btn.repaint(); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(CLR_ACCENT);  btn.repaint(); }
        });
        return btn;
    }

    private void styleSecondaryButton(JButton btn) {
        btn.setFont(FONT_BOLD);
        btn.setBackground(CLR_PANEL);
        btn.setForeground(CLR_TEXT);
        btn.setOpaque(true);
        btn.setBorderPainted(true);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(CLR_BORDER),
                new EmptyBorder(7, 16, 7, 16)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JButton smallButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_SMALL);
        btn.setBackground(CLR_PANEL);
        btn.setForeground(CLR_TEXT);
        btn.setOpaque(true);
        btn.setBorderPainted(true);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(CLR_BORDER),
                new EmptyBorder(5, 10, 5, 10)));
        return btn;
    }

    private void styleInput(JTextField field) {
        field.setBackground(CLR_INPUT_BG);
        field.setForeground(CLR_TEXT);
        field.setCaretColor(CLR_TEXT);
        field.setFont(FONT_LABEL);
        field.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(CLR_BORDER),
                new EmptyBorder(8, 12, 8, 12)));
    }

    private JTextField settingsTextField(String value) {
        JTextField f = new JTextField(value);
        styleInput(f);
        return f;
    }

    private JTextArea settingsTextArea(String value) {
        JTextArea a = new JTextArea(value);
        a.setFont(FONT_MONO);
        a.setBackground(CLR_INPUT_BG);
        a.setForeground(CLR_TEXT);
        a.setCaretColor(CLR_TEXT);
        a.setBorder(new EmptyBorder(8, 10, 8, 10));
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return a;
    }

    private void styleTabPane(JTabbedPane tabs) {
        tabs.setBackground(CLR_PANEL);
        tabs.setForeground(CLR_TEXT);
        tabs.setFont(FONT_BOLD);
        tabs.setBorder(BorderFactory.createEmptyBorder());
    }

    private void stylePopup(JPopupMenu popup) {
        popup.setBackground(CLR_CARD);
        popup.setBorder(BorderFactory.createLineBorder(CLR_BORDER));
    }

    private JMenuItem menuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setBackground(CLR_CARD);
        item.setForeground(CLR_TEXT);
        item.setFont(FONT_LABEL);
        return item;
    }

    private Border roundedBorder() {
        return BorderFactory.createLineBorder(CLR_BORDER, 1, true);
    }

    private void refreshDocCount() {
        int count = engine.getIndexedDocCount();
        docCountLabel.setText(count == 0
                ? "No index — go to the Index tab to build one"
                : count + " files indexed");
        docCountLabel.setForeground(count == 0 ? CLR_WARN : CLR_TEXT_DIM);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}