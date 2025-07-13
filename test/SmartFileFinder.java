import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * SmartFileFinder: A Swing-based file search tool.
 *
 * Features:
 * - Search by name (with regex), extension, size, and content.
 * - Uses java.nio.file for efficient directory walking.
 * - Responsive GUI with live results, progress bar, and cancellation,
 *   thanks to SwingWorker.
 */
public class SmartFileFinder extends JFrame {

    // --- GUI Components ---
    private final JTextField pathField = new JTextField(System.getProperty("user.home"), 30);
    private final JButton browseButton = new JButton("Browse...");
    private final JTextField nameField = new JTextField(15);
    private final JTextField extensionField = new JTextField(5);
    private final JTextField contentField = new JTextField(15);
    private final JSpinner minSizeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    private final JSpinner maxSizeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));

    private final JButton searchButton = new JButton("Search");
    private final JButton cancelButton = new JButton("Cancel");

    private final JTable resultsTable;
    private final DefaultTableModel tableModel;
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel("Ready.");

    private SearchWorker searchWorker;

    public SmartFileFinder() {
        super("Smart File Finder");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); // Center on screen

        // --- Model for the results table ---
        String[] columnNames = {"File Name", "Path", "Size (KB)", "Last Modified"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table cells non-editable
            }
        };
        resultsTable = new JTable(tableModel);

        // --- Setup Layout and Components ---
        initLayout();
        initActions();
    }

    private void initLayout() {
        // --- Input Panel ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Search Filters"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: Path
        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(new JLabel("Search In:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; inputPanel.add(pathField, gbc);
        gbc.gridx = 3; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; inputPanel.add(browseButton, gbc);

        // Row 1: Name & Extension
        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(new JLabel("File Name (regex):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; inputPanel.add(nameField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; inputPanel.add(new JLabel("Extension:"), gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL; inputPanel.add(extensionField, gbc);

        // Row 2: Content & Size
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; inputPanel.add(new JLabel("Containing Text:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; inputPanel.add(contentField, gbc);

        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sizePanel.add(new JLabel("Size (KB): Min"));
        sizePanel.add(minSizeSpinner);
        sizePanel.add(new JLabel("Max"));
        sizePanel.add(maxSizeSpinner);
        ((JSpinner.DefaultEditor) maxSizeSpinner.getEditor()).getTextField().setColumns(6);
        ((JSpinner.DefaultEditor) minSizeSpinner.getEditor()).getTextField().setColumns(6);

        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth=2; gbc.fill = GridBagConstraints.NONE; inputPanel.add(sizePanel, gbc);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(searchButton);
        buttonPanel.add(cancelButton);
        cancelButton.setEnabled(false);

        // --- Top Panel (Inputs + Buttons) ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // --- Status Panel ---
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(progressBar, BorderLayout.EAST);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // --- Main Layout ---
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultsTable), BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void initActions() {
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(pathField.getText());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        searchButton.addActionListener(e -> startSearch());
        cancelButton.addActionListener(e -> cancelSearch());
    }

    private void startSearch() {
        // Clear previous results
        tableModel.setRowCount(0);

        // Get search parameters from GUI
        Path startPath = Paths.get(pathField.getText());
        String namePattern = nameField.getText();
        String extension = extensionField.getText();
        String contentPattern = contentField.getText();
        long minSize = (int) minSizeSpinner.getValue() * 1024L;
        long maxSize = (int) maxSizeSpinner.getValue() * 1024L;

        // Validate inputs
        if (!Files.isDirectory(startPath)) {
            JOptionPane.showMessageDialog(this, "Invalid search path.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (maxSize > 0 && minSize > maxSize) {
            JOptionPane.showMessageDialog(this, "Min size cannot be greater than max size.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Compile regex patterns once for efficiency
            Pattern nameRegex = namePattern.isEmpty() ? null : Pattern.compile(namePattern, Pattern.CASE_INSENSITIVE);
            Pattern contentRegex = contentPattern.isEmpty() ? null : Pattern.compile(contentPattern, Pattern.CASE_INSENSITIVE);

            // --- UI Updates for starting search ---
            searchButton.setEnabled(false);
            cancelButton.setEnabled(true);
            progressBar.setIndeterminate(true);
            statusLabel.setText("Searching...");

            // --- Start the background search ---
            searchWorker = new SearchWorker(startPath, nameRegex, extension, contentRegex, minSize, maxSize);
            searchWorker.execute();

        } catch (PatternSyntaxException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Regex Pattern: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelSearch() {
        if (searchWorker != null) {
            searchWorker.cancel(true);
        }
    }

    // --- SwingWorker for background searching ---
    private class SearchWorker extends SwingWorker<Void, File> {
        private final Path startPath;
        private final Pattern nameRegex;
        private final String extension;
        private final Pattern contentRegex;
        private final long minSize;
        private final long maxSize;
        private int fileCount = 0;

        public SearchWorker(Path startPath, Pattern nameRegex, String extension, Pattern contentRegex, long minSize, long maxSize) {
            this.startPath = startPath;
            this.nameRegex = nameRegex;
            this.extension = extension;
            this.contentRegex = contentRegex;
            this.minSize = minSize;
            this.maxSize = maxSize;
        }

        @Override
        protected Void doInBackground() throws Exception {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isCancelled()) {
                        return FileVisitResult.TERMINATE;
                    }

                    // Update status with current location
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Scanning: " + file.getParent().toString()));

                    if (matches(file, attrs)) {
                        publish(file.toFile()); // Send matching file to process()
                        fileCount++;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // Just skip files we can't access
                    return FileVisitResult.CONTINUE;
                }
            });
            return null;
        }

        private boolean matches(Path path, BasicFileAttributes attrs) {
            // 1. Match by size
            if (minSize > 0 && attrs.size() < minSize) return false;
            if (maxSize > 0 && attrs.size() > maxSize) return false;

            String fileName = path.getFileName().toString();

            // 2. Match by extension
            if (!extension.isEmpty() && !fileName.toLowerCase().endsWith("." + extension.toLowerCase())) {
                return false;
            }

            // 3. Match by name regex
            if (nameRegex != null) {
                Matcher matcher = nameRegex.matcher(fileName);
                if (!matcher.find()) {
                    return false;
                }
            }

            // 4. Match by content (slowest, do it last)
            if (contentRegex != null) {
                try {
                    return Files.lines(path).anyMatch(line -> contentRegex.matcher(line).find());
                } catch (MalformedInputException e) {
                    // It's a binary file or wrong encoding, can't search content.
                    return false;
                } catch (IOException e) {
                    // Other reading error.
                    return false;
                }
            }

            return true; // All checks passed
        }

        @Override
        protected void process(List<File> chunks) {
            // This method is called on the EDT
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (File file : chunks) {
                tableModel.addRow(new Object[]{
                        file.getName(),
                        file.getParent(),
                        file.length() / 1024, // Size in KB
                        sdf.format(file.lastModified())
                });
            }
            statusLabel.setText("Found " + tableModel.getRowCount() + " files...");
        }


        @Override
        protected void done() {
            // This method is called on the EDT when doInBackground is finished
            try {
                get(); // To catch any exceptions from doInBackground
                statusLabel.setText("Search complete. Found " + tableModel.getRowCount() + " files.");
            } catch (InterruptedException | CancellationException e) {
                statusLabel.setText("Search cancelled. Found " + tableModel.getRowCount() + " files.");
            } catch (ExecutionException e) {
                statusLabel.setText("Error during search: " + e.getCause().getMessage());
                e.printStackTrace();
            } finally {
                progressBar.setIndeterminate(false);
                progressBar.setValue(0);
                searchButton.setEnabled(true);
                cancelButton.setEnabled(false);
            }
        }
    }

    // --- Main Method ---
    public static void main(String[] args) {
        // Set a more modern Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Ensure GUI is created on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> new SmartFileFinder().setVisible(true));
    }
}