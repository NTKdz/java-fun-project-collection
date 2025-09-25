import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Map;

public class ColorTextToImage {
    private static final Map<String, Color> COLOR_MAP = new HashMap<>();

    static {
        COLOR_MAP.put("red", Color.RED);
        COLOR_MAP.put("green", Color.GREEN);
        COLOR_MAP.put("blue", Color.BLUE);
        COLOR_MAP.put("yellow", Color.YELLOW);
        COLOR_MAP.put("purple", new Color(128, 0, 128));
        COLOR_MAP.put("orange", Color.ORANGE);
        COLOR_MAP.put("cyan", Color.CYAN);
        COLOR_MAP.put("mint", new Color(152, 255, 152));
        COLOR_MAP.put("teal", new Color(0, 128, 128));
        COLOR_MAP.put("coral", new Color(255, 127, 80));
        COLOR_MAP.put("navy", new Color(0, 0, 128));
        COLOR_MAP.put("beige", new Color(245, 245, 220));
        COLOR_MAP.put("grey", Color.GRAY);
        COLOR_MAP.put("lime", new Color(191, 255, 0));
        COLOR_MAP.put("magenta", Color.MAGENTA);
        COLOR_MAP.put("lavender", new Color(230, 230, 250));
        COLOR_MAP.put("maroon", new Color(128, 0, 0));
        COLOR_MAP.put("olive", new Color(128, 128, 0));
        COLOR_MAP.put("brown", new Color(139, 69, 19)); // Added brown
    }

    public static void main(String[] args) throws Exception {
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader("highlighted_code.txt"))) {
            StringBuilder content = new StringBuilder();
            String lineread;
            while ((lineread = reader.readLine()) != null) {
                content.append(lineread).append("\n");
            }

            String[] lines = content.toString().split("\n");

            int baseBlockWidth = 10; // Base width per unit length
            int blockHeight = 20;
            int gap = 1;
            int cornerRadius = (int) (blockHeight * 0.3); // 30% of height for border radius

            // Calculate max image width based on longest line
            int maxImageWidth = 0;
            for (String line : lines) {
                int lineWidth = 0;
                for (char c : line.toCharArray()) {
                    lineWidth += baseBlockWidth + gap;
                }
                if (lineWidth > maxImageWidth) maxImageWidth = lineWidth;
            }
            int height = lines.length;

            BufferedImage image = new BufferedImage(
                    maxImageWidth,
                    height * (blockHeight + gap + 5),
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g = image.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, image.getWidth(), image.getHeight()); // Background

            for (int y = 0; y < lines.length; y++) {
                String[] tokens = lines[y].split("(?<=\\s)|(?=\\s)");
                int xPos = 0;
                for (String token : tokens) {
                    if (token.trim().isEmpty()) {
                        xPos += baseBlockWidth + gap; // Same spacing for spaces
                        continue;
                    }
                    String[] parts = token.split("\\.");
                    String colorName = parts[0].toLowerCase();
                    int length = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    int blockWidth = length * baseBlockWidth; // Scale width by length

                    Color c = COLOR_MAP.getOrDefault(colorName, Color.WHITE);
                    g.setColor(c);
                    g.fillRoundRect(xPos, y * (blockHeight + gap + 5), blockWidth, blockHeight, cornerRadius, cornerRadius);
                    xPos += blockWidth + gap; // Move to next block
                }
            }

            g.dispose();

            ImageIO.write(image, "png", new File("mosaic.png"));
            System.out.println("Image saved as mosaic.png");
        }
    }
}