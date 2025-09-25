import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class BarVisualizer extends JPanel {
    private int[] currentLevels = new int[0]; // what is shown
    private int[] targetLevels = new int[0];  // new computed RMS
    private final float riseSpeed = 0.08f;     // how fast bars rise (0..1)
    private final float fallSpeed = 0.05f;     // how fast bars fall (0..1)

    public enum Mode { BARS, WAVEFORM, MIRROR_WAVEFORM, CIRCLE, DOTS }

    private Mode mode = Mode.BARS;

    public BarVisualizer(int width, int height) {
        setPreferredSize(new Dimension(width, height));
    }

    public void setMode(Mode m) {
        this.mode = m;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int w = getWidth();
        int h = getHeight();

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);

        if (currentLevels.length == 0) return;

        switch (mode) {
            case BARS -> drawBars(g2, w, h);
            case WAVEFORM -> drawWaveform(g2, w, h);
            case MIRROR_WAVEFORM -> drawMirrorWaveform(g2, w, h);
            case CIRCLE -> drawCircle(g2, w, h);
            case DOTS -> drawDots(g2, w, h);
        }
    }

    // --- Visualize Modes ---

    private void drawBars(Graphics2D g2, int w, int h) {
        int barWidth = Math.max(1, w / currentLevels.length);
        g2.setColor(Color.GREEN);

        for (int i = 0; i < currentLevels.length; i++) {
            int barHeight = (int) ((currentLevels[i] / 32768.0) * h);
            int x = i * barWidth;
            int y = h - barHeight;
            g2.fillRect(x, y, barWidth - 2, barHeight);
        }
    }

    private void drawWaveform(Graphics2D g2, int w, int h) {
        g2.setColor(Color.CYAN);
        int mid = h / 2;
        for (int i = 1; i < currentLevels.length; i++) {
            int x1 = (i - 1) * w / currentLevels.length;
            int y1 = mid - (currentLevels[i - 1] * mid / 32768);
            int x2 = i * w / currentLevels.length;
            int y2 = mid - (currentLevels[i] * mid / 32768);
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawMirrorWaveform(Graphics2D g2, int w, int h) {
        g2.setColor(Color.MAGENTA);
        int mid = h / 2;
        for (int i = 1; i < currentLevels.length; i++) {
            int x1 = (i - 1) * w / currentLevels.length;
            int y1 = (currentLevels[i - 1] * mid / 32768);
            int x2 = i * w / currentLevels.length;
            int y2 = (currentLevels[i] * mid / 32768);

            g2.drawLine(x1, mid - y1, x2, mid - y2); // upper
            g2.drawLine(x1, mid + y1, x2, mid + y2); // mirrored lower
        }
    }

    private void drawCircle(Graphics2D g2, int w, int h) {
        g2.setColor(Color.ORANGE);
        int cx = w / 2;
        int cy = h / 2;
        int radius = Math.min(cx, cy) / 2;

        for (int i = 0; i < currentLevels.length; i++) {
            double angle = 2 * Math.PI * i / currentLevels.length;
            int barHeight = (int) ((currentLevels[i] / 32768.0) * (h / 2));

            int x1 = cx + (int)(Math.cos(angle) * radius);
            int y1 = cy + (int)(Math.sin(angle) * radius);
            int x2 = cx + (int)(Math.cos(angle) * (radius + barHeight));
            int y2 = cy + (int)(Math.sin(angle) * (radius + barHeight));

            g2.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawDots(Graphics2D g2, int w, int h) {
        g2.setColor(Color.YELLOW);
        int dotSpacing = w / currentLevels.length;

        for (int i = 0; i < currentLevels.length; i++) {
            int barHeight = (int) ((currentLevels[i] / 32768.0) * h);
            int x = i * dotSpacing + dotSpacing / 2;
            int y = h - barHeight;

            int size = Math.max(4, barHeight / 10);
            g2.fillOval(x - size / 2, y - size / 2, size, size);
        }
    }

    // --- Audio Processing ---

    public void setLevels(int[] samples, int bars) {
        int samplesPerBar = Math.max(1, samples.length / bars);
        int[] newTargets = new int[bars];

        for (int i = 0; i < bars; i++) {
            int start = i * samplesPerBar;
            int end = Math.min(start + samplesPerBar, samples.length);

            long sum = 0;
            for (int j = start; j < end; j++) {
                sum += (long) samples[j] * samples[j];
            }

            int count = end - start;
            int rms = count > 0 ? (int) Math.sqrt(sum / (double) count) : 0;
            newTargets[i] = rms;
        }

        if (targetLevels.length != bars) {
            targetLevels = new int[bars];
            currentLevels = new int[bars];
        }

        targetLevels = newTargets;
    }

    public void smoothUpdate() {
        for (int i = 0; i < currentLevels.length; i++) {
            if (targetLevels[i] > currentLevels[i]) {
                currentLevels[i] += (int) ((targetLevels[i] - currentLevels[i]) * riseSpeed);
            } else {
                currentLevels[i] -= (int) ((currentLevels[i] - targetLevels[i]) * fallSpeed);
            }
        }
        repaint();
    }

    public static void main(String[] args) throws Exception {
        File file = new File("song.wav");
        AudioInputStream stream = AudioSystem.getAudioInputStream(file);
        AudioFormat format = stream.getFormat();

        if (format.getSampleSizeInBits() != 16 || format.isBigEndian()) {
            throw new UnsupportedAudioFileException("Only 16-bit little-endian PCM supported.");
        }

        int frameSize = format.getFrameSize();
        int sampleRate = (int) format.getSampleRate();
        int windowMs = 50;
        int bytesPerWindow = (int) (frameSize * sampleRate * (windowMs / 1000.0));

        byte[] buffer = new byte[bytesPerWindow];

        BarVisualizer vis = new BarVisualizer(800, 300);

        // === GUI with selector ===
        JFrame frame = new JFrame("Visualizer Selector");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(vis, BorderLayout.CENTER);

        JComboBox<Mode> selector = new JComboBox<>(Mode.values());
        selector.addActionListener(e -> vis.setMode((Mode) selector.getSelectedItem()));
        frame.add(selector, BorderLayout.NORTH);

        frame.pack();
        frame.setVisible(true);

        int numBars = 60;

        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        line.start();

        Timer audioTimer = new Timer(windowMs, e -> {
            try {
                int bytesRead = stream.read(buffer);
                if (bytesRead == -1) {
                    ((Timer) e.getSource()).stop();
                    line.drain();
                    line.close();
                    return;
                }
                line.write(buffer, 0, bytesRead);

                int[] samples = new int[bytesRead / 2];
                for (int i = 0, s = 0; i < bytesRead - 1; i += 2, s++) {
                    int sample = (buffer[i] & 0xFF) | (buffer[i + 1] << 8);
                    samples[s] = sample;
                }
                vis.setLevels(samples, numBars);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Timer drawTimer = new Timer(1000 / 144, e -> vis.smoothUpdate());

        audioTimer.start();
        drawTimer.start();
    }
}
