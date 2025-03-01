import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class SortingVisualizer {
    final public static int PANEL_WIDTH = 800;
    final public static int PANEL_HEIGHT = 600;
    public static int ARRAY_SIZE = 400;
    final public static int ARRAY_MIN = 0;
    public static int ARRAY_MAX = 400;
    static Random rand = new Random();
    static int[] array = new int[ARRAY_SIZE];
    static int sortType = 0;
    static long timeTaken = 0;
    static int operations = 0;
    static String complexity = "";
    static JLabel statsLabel = new JLabel("Performance: ");
    static int delay_divisor = 60;
    static boolean raw = false;
    static int[] index = new int[]{-1, -1};
    static SourceDataLine currentLine;

    public static void main(String[] args) {
        array = generateArray(ARRAY_SIZE);
        JFrame frame = new JFrame("Sorting Visualizer");

        JPanel panel = createVisualizationPanel();
        JComboBox<String> algorithmSelector = createAlgorithmSelector();
        JPanel controlPanel = createControlPanel(algorithmSelector, panel);
//        JScrollPane scrollPane = new JScrollPane(panel);
//        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//
//        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.add(statsLabel, BorderLayout.NORTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void playSound(int frequency, int duration) {
        try {
            AudioFormat format = new AudioFormat(44100, 8, 1,
                    true, true);
            if (currentLine == null) {
                currentLine = AudioSystem.getSourceDataLine(format);
                currentLine.open(format);
                currentLine.start();
            }

            byte[] buffer = new byte[duration * 44100 / 1000];

            for (int i = 0; i < buffer.length; i++) {
                double angle = 2.0 * Math.PI * i * frequency / 44100;
                buffer[i] = (byte) (Math.sin(angle) * 127);
            }

            currentLine.flush();
            currentLine.write(buffer, 0, buffer.length);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void selectionSort(JPanel panel) throws InterruptedException {
        Thread.sleep(500);
        long startTime = System.nanoTime();
        operations = 0;

        for (int i = 0; i < ARRAY_SIZE - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < ARRAY_SIZE; j++) {
                if (array[j] < array[minIdx]) minIdx = j;
                operations++;
            }
            swap(i, minIdx);
            visualize(panel, delay_divisor);
        }
        updateStats(startTime, "O(n²) Time, O(1) Space");
    }

    public static void quickSort(JPanel panel) throws InterruptedException {
        Thread.sleep(500);
        long startTime = System.nanoTime();
        operations = 0;
        quickSortHelper(panel, 0, ARRAY_SIZE - 1);
        updateStats(startTime, "O(n log n) avg, O(log n) Space");
    }

    private static void quickSortHelper(JPanel panel, int low, int high) throws InterruptedException {
        if (low < high) {
            int pi = randomizedPartition(panel, low, high);
            quickSortHelper(panel, low, pi - 1);
            quickSortHelper(panel, pi + 1, high);
        }
    }

    private static int randomizedPartition(JPanel panel, int low, int high) throws InterruptedException {
        int randomIdx = low + rand.nextInt(high - low + 1);
        swap(randomIdx, high);
        return partition(panel, low, high);
    }

    private static int partition(JPanel panel, int low, int high) throws InterruptedException {
        int pivot = array[high];
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (array[j] < pivot) {
                i++;
                swap(i, j);
                visualize(panel, delay_divisor);
            }
            operations++;
        }
        swap(i + 1, high);
        visualize(panel, delay_divisor);
        return i + 1;
    }


    public static void mergeSort(JPanel panel) throws InterruptedException {
        Thread.sleep(500);
        long startTime = System.nanoTime();
        operations = 0;
        int[] temp = new int[ARRAY_SIZE];
        mergeSortHelper(panel, 0, ARRAY_SIZE - 1, temp);
        updateStats(startTime, "O(n log n) Time, O(n) Space");
    }

    private static void mergeSortHelper(JPanel panel, int left, int right, int[] temp) throws InterruptedException {
        if (left < right) {
            int mid = (left + right) / 2;
            mergeSortHelper(panel, left, mid, temp);
            mergeSortHelper(panel, mid + 1, right, temp);
            merge(panel, left, mid, right, temp);
        }
    }

    private static void merge(JPanel panel, int left, int mid, int right, int[] temp) throws InterruptedException {
        for (int i = left; i <= right; i++) {
            temp[i] = array[i];
        }
        int i = left, j = mid + 1, k = left;
        while (i <= mid && j <= right) {
            index[0] = k;
            if (temp[i] <= temp[j]) {
                index[1] = i;
                array[k] = temp[i++];
            } else {
                index[1] = j;
                array[k] = temp[j++];
            }
            operations++;
            panel.repaint();
            if (!raw) {
                Thread.sleep(1000 / delay_divisor);
                playSound(200 + (array[i] * 5), 50);
            }
            k++;
        }
        while (i <= mid) {
            array[k++] = temp[i++];
            index[0] = k;
            index[1] = i;

            operations++;
            panel.repaint();
        }
        while (j <= right) {
            array[k++] = temp[j++];
            index[0] = k;
            index[1] = j;

            operations++;
            panel.repaint();
        }
    }


    public static void cocktailSort(JPanel panel) throws InterruptedException {
        Thread.sleep(500);
        long startTime = System.nanoTime();
        operations = 0;
        boolean swapped;
        int start = 0, end = ARRAY_SIZE - 1;

        do {
            swapped = false;
            for (int i = start; i < end; i++) {
                if (array[i] > array[i + 1]) {
                    swap(i, i + 1);
                    swapped = true;
                    visualize(panel, delay_divisor);
                }
                operations++;
            }
            if (!swapped) break;
            end--;

            swapped = false;
            for (int i = end - 1; i >= start; i--) {
                if (array[i] > array[i + 1]) {
                    swap(i, i + 1);
                    swapped = true;
                    visualize(panel, delay_divisor);
                }
                operations++;
            }
            start++;
        } while (swapped);
        updateStats(startTime, "O(n²) Time, O(1) Space");
    }


    public static void countingSort(JPanel panel) throws InterruptedException {
        Thread.sleep(500);
        long startTime = System.nanoTime();
        operations = 0;

        int[] count = new int[ARRAY_MAX + 1];
        int[] output = new int[ARRAY_SIZE];

        for (int num : array) count[num]++;
        for (int i = 1; i <= ARRAY_MAX; i++) {
            count[i] += count[i - 1];
            playSound(200 + (array[i] * 5), 50);
        }
        for (int i = ARRAY_SIZE - 1; i >= 0; i--) {
            output[count[array[i]] - 1] = array[i];
            count[array[i]]--;
            operations++;
            visualize(panel, delay_divisor);
        }
        System.arraycopy(output, 0, array, 0, ARRAY_SIZE);
        updateStats(startTime, "O(n+k) Time, O(k) Space");
    }


    public static void radixSort(JPanel panel) throws InterruptedException {
        Thread.sleep(500);
        long startTime = System.nanoTime();
        operations = 0;

        int max = Arrays.stream(array).max().getAsInt();
        for (int exp = 1; max / exp > 0; exp *= 10) {
            countSortRadix(panel, exp);
            visualize(panel, delay_divisor);
        }
        updateStats(startTime, "O(nk) Time, O(n+k) Space");
    }

    private static void countSortRadix(JPanel panel, int exp) throws InterruptedException {
        int[] output = new int[ARRAY_SIZE];
        int[] count = new int[10];

        Arrays.fill(count, 0);
        for (int num : array) count[(num / exp) % 10]++;
        for (int i = 1; i < 10; i++) count[i] += count[i - 1];
        for (int i = ARRAY_SIZE - 1; i >= 0; i--) {
            output[count[(array[i] / exp) % 10] - 1] = array[i];
            count[(array[i] / exp) % 10]--;
            operations++;
            visualize(panel, delay_divisor);
        }
        System.arraycopy(output, 0, array, 0, ARRAY_SIZE);
    }


    public static void bucketSort(JPanel panel) throws InterruptedException {
        Thread.sleep(500);
        long startTime = System.nanoTime();
        operations = 0;

        int bucketCount = 10;
        ArrayList<Integer>[] buckets = new ArrayList[bucketCount];
        for (int i = 0; i < bucketCount; i++) buckets[i] = new ArrayList<>();

        for (int num : array) {
            int bucketIdx = num * bucketCount / (ARRAY_MAX + 1);
            buckets[bucketIdx].add(num);
            operations++;
        }

        int index = 0;
        for (ArrayList<Integer> bucket : buckets) {
            Collections.sort(bucket);
            for (int num : bucket) {
                swap(index, index);
                array[index++] = num;
                visualize(panel, delay_divisor);
            }
        }
        updateStats(startTime, "O(n+k) avg, O(n) Space");
    }


    public static void gnomeSort(JPanel panel) throws InterruptedException {
        Thread.sleep(500);
        long startTime = System.nanoTime();
        operations = 0;

        int index = 0;
        while (index < ARRAY_SIZE) {
            if (index == 0 || array[index] >= array[index - 1]) {
                index++;
            } else {
                swap(index, index - 1);
                index--;
                visualize(panel, delay_divisor);
            }
            operations++;
        }
        updateStats(startTime, "O(n²) Time, O(1) Space");
    }


    private static void visualize(JPanel panel, int delayDivisor) throws InterruptedException {
        panel.repaint();
        if (!raw) Thread.sleep(1000 / delay_divisor);
    }

    private static void updateStats(long startTime, String comp) {
        timeTaken = System.nanoTime() - startTime;
        complexity = comp;
        SwingUtilities.invokeLater(() -> {
            if (raw)
                statsLabel.setText(String.format("Time: %.2f ms | Ops: %d | %s",
                        timeTaken / 1e6, operations, complexity));
            else statsLabel.setText(String.format("Ops: %d | %s",
                    operations, complexity));
        });
//        System.out.println(timeTaken / 1e6 + " " + comp);
    }

    private static JPanel createVisualizationPanel() {
        JPanel panel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(
                        (int) Math.max(Math.max(1, (double) getWidth() / array.length), PANEL_WIDTH),
                        PANEL_HEIGHT
                );
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int panelWidth = getWidth();
                int panelHeight = getHeight();

                double blockWidth = Math.max(1, (double) panelWidth / array.length);
                double blockHeight = (double) panelHeight / (ARRAY_MAX - ARRAY_MIN);

                for (int i = 0; i < array.length; i++) {
                    int height = (int) (blockHeight * (array[i] - ARRAY_MIN + 1));
                    int xPosition = (int) (i * blockWidth);

                    if (xPosition >= panelWidth) continue;

                    g.setColor((i == index[0] || i == index[1]) ? Color.GREEN : new Color(0, 150, 0));
                    g.fillRect(xPosition, panelHeight - height, (int) blockWidth, height);
                }
            }

        };
        panel.setBackground(new Color(18, 18, 18));


        panel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent evt) {
                panel.repaint();
            }
        });

        return panel;
    }


    private static JComboBox<String> createAlgorithmSelector() {
        String[] algorithms = {
                "Selection", "Bubble", "Insertion", "Merge", "Quick", "Heap",
                "Shell", "Cocktail", "Counting", "Radix", "Bucket", "Gnome",
                "Comb", "Intro", "Tree", "Library", "Block", "Smooth",
                "Patience", "Cube"
        };
        return new JComboBox<>(algorithms);
    }

    private static JPanel createControlPanel(JComboBox<String> selector, JPanel panel) {
        JPanel controlPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout());

        // Existing buttons
        JButton startButton = new JButton("Start");
        JButton resetButton = new JButton("New Array");
        JButton rawButton = new JButton("Raw");

        // Sliders
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));

        // Array Size Slider
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel sizeLabel = new JLabel("Size: " + ARRAY_SIZE);
        JSlider sizeSlider = new JSlider(10, 2000, ARRAY_SIZE);
        sizeSlider.setPreferredSize(new Dimension(150, 20));
        sizeSlider.addChangeListener(e -> {
            ARRAY_SIZE = sizeSlider.getValue();
            sizeLabel.setText("Size: " + ARRAY_SIZE);
        });
        sizePanel.add(new JLabel("Array Size:"));
        sizePanel.add(sizeSlider);
        sizePanel.add(sizeLabel);

        // Max Value Slider
        JPanel maxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel maxLabel = new JLabel("Max: " + ARRAY_MAX);
        JSlider maxSlider = new JSlider(10, 1000, ARRAY_MAX);
        maxSlider.setPreferredSize(new Dimension(150, 20));
        maxSlider.addChangeListener(e -> {
            ARRAY_MAX = maxSlider.getValue();
            maxLabel.setText("Max: " + ARRAY_MAX);
        });
        maxPanel.add(new JLabel("Max Value:"));
        maxPanel.add(maxSlider);
        maxPanel.add(maxLabel);

        sliderPanel.add(sizePanel);
        sliderPanel.add(maxPanel);

        // Add components to control panel
        controlPanel.add(buttonPanel, BorderLayout.WEST);
        controlPanel.add(sliderPanel, BorderLayout.CENTER);

        // Existing button listeners
        startButton.addActionListener(e -> {
            raw = false;
            sortType = selector.getSelectedIndex();
            new SwingWorker<Void, Void>() {
                protected Void doInBackground() throws Exception {
                    sort(panel, sortType);
                    return null;
                }
            }.execute();
        });

        resetButton.addActionListener(e -> {
            index[0] = -1;
            index[1] = -1;
            array = generateArray(ARRAY_SIZE); // Generate with current size
            statsLabel.setText("Performance: ");
            panel.revalidate();
            panel.repaint();
        });

        rawButton.addActionListener(e -> {
            raw = true;
            sortType = selector.getSelectedIndex();
            new SwingWorker<Void, Void>() {
                protected Void doInBackground() throws Exception {
                    sort(panel, sortType);
                    return null;
                }
            }.execute();
        });

        buttonPanel.add(selector);
        buttonPanel.add(startButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(rawButton);

        return controlPanel;
    }


    private static void swap(int i, int j) {
//        System.out.println(i);
        index[0] = i;
        index[1] = j;
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
        operations++;
        if (!raw) playSound(200 + (array[i] * 5), 50);
    }

    public static int[] generateArray(int size) {
        return rand.ints(size, ARRAY_MIN, ARRAY_MAX + 1).toArray();
    }

    public static void sort(JPanel panel, int sortType) {
        try {
            switch (sortType) {
                case 0 -> selectionSort(panel);
                case 1 -> bubbleSort(panel);
                case 2 -> insertionSort(panel);
                case 3 -> mergeSort(panel);
                case 4 -> quickSort(panel);
                case 5 -> heapSort(panel);
                case 6 -> shellSort(panel);
                case 7 -> cocktailSort(panel);
//                case 8 -> countingSort(panel);
//                case 9 -> radixSort(panel);
                case 10 -> bucketSort(panel);
                case 11 -> gnomeSort(panel);
                case 12 -> combSort(panel);
                case 13 -> introSort(panel);
                case 14 -> treeSort(panel);
            }

            if (currentLine != null) {
                System.out.println(currentLine);
                currentLine.flush();
                currentLine.stop();
                currentLine.close();
                currentLine = null;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void combSort(JPanel panel) throws InterruptedException {
        Thread.sleep(500);
        long startTime = System.nanoTime();
        operations = 0;

        int gap = ARRAY_SIZE;
        boolean swapped = true;

        while (gap > 1 || swapped) {
            gap = Math.max(1, (gap * 10) / 13);
            swapped = false;

            for (int i = 0; i < ARRAY_SIZE - gap; i++) {
                if (array[i] > array[i + gap]) {
                    swap(i, i + gap);
                    swapped = true;
                    visualize(panel, delay_divisor);
                }
                operations++;
            }
        }
        updateStats(startTime, "O(n²) worst, O(n log n) best");
    }

    // IntroSort (Hybrid of QuickSort, HeapSort, and InsertionSort)
    public static void introSort(JPanel panel) throws InterruptedException {
        Thread.sleep(500);
        long startTime = System.nanoTime();
        operations = 0;
        int depthLimit = (int) (2 * Math.log(ARRAY_SIZE) / Math.log(2));
        introSortHelper(panel, 0, ARRAY_SIZE - 1, depthLimit);
        updateStats(startTime, "O(n log n)");
    }

    private static void introSortHelper(JPanel panel, int low, int high, int depth) throws InterruptedException {
        if (high - low < 16) {
            insertionSortHelper(panel, low, high);
            return;
        }
        if (depth == 0) {
            heapSortHelper(panel, low, high);
            return;
        }

        int pivot = partition(panel, low, high);
        introSortHelper(panel, low, pivot - 1, depth - 1);
        introSortHelper(panel, pivot + 1, high, depth - 1);
    }

    private static void heapSortHelper(JPanel panel, int low, int high) throws InterruptedException {
        int n = high - low + 1;

        // Build heap
        for (int i = low + (n / 2 - 1); i >= low; i--) {
            heapifySubarray(panel, n, i, low);
        }

        // Extract elements
        for (int i = high; i > low; i--) {
            swap(low, i);
            visualize(panel, delay_divisor);
            heapifySubarray(panel, i - low, low, low);
        }
    }

    private static void heapifySubarray(JPanel panel, int n, int i, int offset) throws InterruptedException {
        int largest = i;
        int left = 2 * (i - offset) + 1 + offset;
        int right = 2 * (i - offset) + 2 + offset;

        if (left < offset + n && array[left] > array[largest]) {
            largest = left;
        }

        if (right < offset + n && array[right] > array[largest]) {
            largest = right;
        }

        if (largest != i) {
            swap(i, largest);
            visualize(panel, delay_divisor);
            heapifySubarray(panel, n, largest, offset);
        }
    }

    private static void insertionSortHelper(JPanel panel, int low, int high) throws InterruptedException {
        for (int i = low + 1; i <= high; i++) {
            int key = array[i];
            int j = i - 1;
            while (j >= low && array[j] > key) {
                swap(j + 1, j);
                visualize(panel, delay_divisor);
                j--;
                operations++;
            }
            array[j + 1] = key;
            operations++;
            visualize(panel, delay_divisor);
        }
    }

    // Tree Sort (using BST)
    public static void treeSort(JPanel panel) throws InterruptedException {
        Thread.sleep(500);
        long startTime = System.nanoTime();
        operations = 0;

        TreeNode root = null;
        for (int i = 0; i < ARRAY_SIZE; i++) {
            root = insertBST(root, array[i]);
        }

        final int[] index = {0};
        inOrderTraversal(root, index);
        updateStats(startTime, "O(n log n)");
    }

    private static class TreeNode {
        int value;
        TreeNode left, right;

        TreeNode(int val) {
            value = val;
        }
    }

    private static TreeNode insertBST(TreeNode root, int val) {
        if (root == null) return new TreeNode(val);
        if (val < root.value) root.left = insertBST(root.left, val);
        else root.right = insertBST(root.right, val);
        return root;
    }

    private static void inOrderTraversal(TreeNode node, int[] index) {
        if (node != null) {
            inOrderTraversal(node.left, index);
            array[index[0]++] = node.value;
            inOrderTraversal(node.right, index);
        }
    }

    public static void bubbleSort(JPanel panel) throws InterruptedException {
        long startTime = System.nanoTime();
        Thread.sleep(500);
        for (int i = 0; i < ARRAY_SIZE - 1; i++) {
            for (int j = 0; j < ARRAY_SIZE - i - 1; j++) {
                if (array[j] > array[j + 1]) {
                    swap(j, j + 1);
                    visualize(panel, delay_divisor);
                }
            }
            panel.repaint();
        }
        timeTaken = System.nanoTime() - startTime;
        complexity = "O(n²) Time, O(1) Space";
        updateStats(timeTaken, complexity);
    }

    public static void insertionSort(JPanel panel) throws InterruptedException {
        long startTime = System.nanoTime();
        Thread.sleep(500);
        for (int i = 1; i < ARRAY_SIZE; i++) {
            int key = array[i];
            int j = i - 1;
            while (j >= 0 && array[j] > key) {
                swap(j + 1, j);
                visualize(panel, delay_divisor);
                j--;
            }
            array[j + 1] = key;
            operations++;
            visualize(panel, delay_divisor);
            panel.repaint();

        }
        timeTaken = System.nanoTime() - startTime;
        complexity = "O(n²) Time, O(1) Space";
        updateStats(timeTaken, complexity);
    }


    public static void heapSort(JPanel panel) throws InterruptedException {
        long startTime = System.nanoTime();
        Thread.sleep(500);
        heapSortHelper(panel, 0, ARRAY_SIZE - 1); // Changed to use helper
        timeTaken = System.nanoTime() - startTime;
        complexity = "O(n log n) Time, O(1) Space";
        updateStats(timeTaken, complexity);
    }

    private static void heapify(JPanel panel, int n, int i) throws InterruptedException {
        int largest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;


        if (left < n && array[left] > array[largest]) {
            largest = left;
        }


        if (right < n && array[right] > array[largest]) {
            largest = right;
        }


        if (largest != i) {
            swap(i, largest);
            visualize(panel, delay_divisor);
            panel.repaint();

            heapify(panel, n, largest);
        }
    }


    public static void shellSort(JPanel panel) throws InterruptedException {
        long startTime = System.nanoTime();
        Thread.sleep(500);


        for (int gap = ARRAY_SIZE / 2; gap > 0; gap /= 2) {

            for (int i = gap; i < ARRAY_SIZE; i++) {
                int temp = array[i];
                int j;
                for (j = i; j >= gap && array[j - gap] > temp; j -= gap) {
                    swap(j, j - gap);
                    visualize(panel, delay_divisor);
                }
                array[j] = temp;
                index[0] = j;
                index[1] = j;
                operations++;
                visualize(panel, delay_divisor);
            }
        }

        timeTaken = System.nanoTime() - startTime;
        complexity = "O(n²) worst, O(n log n) best Time, O(1) Space";
        updateStats(timeTaken, complexity);
    }
}