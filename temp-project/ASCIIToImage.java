import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ASCIIToImage {
    public static void toASCII(String path) throws IOException {
        try {
            // Load the image
            File imageFile = new File(path);
            BufferedImage image = ImageIO.read(imageFile);

            // Define ASCII characters based on brightness
            String asciiChars = "@#S%?*+;:,. ";

            // Get image dimensions
            int width = image.getWidth();
            int height = image.getHeight();

            // Scale the image for better ASCII representation
            int newWidth = width / 2;
            int newHeight = height / 4;
            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            resizedImage.getGraphics().drawImage(image, 0, 0, newWidth, newHeight, null);

            // Generate ASCII art
            StringBuilder asciiArt = new StringBuilder();
            for (int y = 0; y < newHeight; y++) {
                for (int x = 0; x < newWidth; x++) {
                    int pixel = resizedImage.getRGB(x, y);
                    // Extract RGB components
                    int red = (pixel >> 16) & 0xff;
                    int green = (pixel >> 8) & 0xff;
                    int blue = pixel & 0xff;

                    // Calculate brightness
                    double brightness = 0.2126 * red + 0.7152 * green + 0.0722 * blue;

                    // Map brightness to ASCII character
                    int charIndex = (int) ((brightness / 255) * (asciiChars.length() - 1));
                    asciiArt.append(asciiChars.charAt(charIndex));
                }
                asciiArt.append("\n");
            }

            // Adjust width-to-height ratio for the output image
            int fontSize = 12; // Adjust font size as needed
            double charWidthToHeightRatio = 0.6; // Monospaced font width-to-height ratio
            int imageWidth = (int) (newWidth * fontSize * charWidthToHeightRatio);
            int imageHeight = newHeight * fontSize;

            // Create the output image
            BufferedImage asciiImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = asciiImage.createGraphics();

            // Set background and font
            g2d.setColor(Color.BLACK); // Background color
            g2d.fillRect(0, 0, asciiImage.getWidth(), asciiImage.getHeight());
            g2d.setColor(Color.WHITE); // Text color
            g2d.setFont(new Font("Monospaced", Font.PLAIN, fontSize));

            // Render ASCII art onto the image
            int yPosition = fontSize;
            for (String line : asciiArt.toString().split("\n")) {
                g2d.drawString(line, 0, yPosition);
                yPosition += fontSize;
            }

            g2d.dispose();

            // Save the ASCII art image
            File outputFile = new File("video" + "/output/" + imageFile.getName());
            ImageIO.write(asciiImage, "jpg", outputFile);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            long start = System.currentTimeMillis();
            String folderPath = "image";

            // Create a File object for the folder
            File folder = new File(folderPath);

            // Check if the folder exists and is a directory
            if (folder.exists() && folder.isDirectory()) {
                // Get the list of files and directories in the folder
                File[] files = folder.listFiles();

                if (files != null) {
                    System.out.println("Files in folder: " + folderPath);
                    for (File file : files) {
                        // Check if it is a file or directory
                        if (file.isFile()) {
                            System.out.println("File: " + file.getName());
                            // Load the image
                            File imageFile = new File(file.getAbsolutePath());
                            BufferedImage image = ImageIO.read(imageFile);

                            // Define ASCII characters based on brightness
                            String asciiChars = "@#S%?*+;:,.";

                            // Get image dimensions
                            int width = image.getWidth();
                            int height = image.getHeight();

                            // Scale the image for better ASCII representation
                            int newWidth = width / 2;
                            int newHeight = height / 4;
                            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                            resizedImage.getGraphics().drawImage(image, 0, 0, newWidth, newHeight, null);

                            // Generate ASCII art
                            StringBuilder asciiArt = new StringBuilder();
                            for (int y = 0; y < newHeight; y++) {
                                for (int x = 0; x < newWidth; x++) {
                                    int pixel = resizedImage.getRGB(x, y);
                                    // Extract RGB components
                                    int red = (pixel >> 16) & 0xff;
                                    int green = (pixel >> 8) & 0xff;
                                    int blue = pixel & 0xff;

                                    // Calculate brightness
                                    double brightness = 0.2126 * red + 0.7152 * green + 0.0722 * blue;

                                    // Map brightness to ASCII character
                                    int charIndex = (int) ((brightness / 255) * (asciiChars.length() - 1));
                                    asciiArt.append(asciiChars.charAt(charIndex));
                                }
                                asciiArt.append("\n");
                            }

                            // Adjust width-to-height ratio for the output image
                            int fontSize = 12; // Adjust font size as needed
                            double charWidthToHeightRatio = 0.6; // Monospaced font width-to-height ratio
                            int imageWidth = (int) (newWidth * fontSize * charWidthToHeightRatio);
                            int imageHeight = newHeight * fontSize;

                            // Create the output image
                            BufferedImage asciiImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
                            Graphics2D g2d = asciiImage.createGraphics();

                            // Set background and font
                            g2d.setColor(Color.BLACK); // Background color
                            g2d.fillRect(0, 0, asciiImage.getWidth(), asciiImage.getHeight());
                            g2d.setColor(Color.WHITE); // Text color
                            g2d.setFont(new Font("Monospaced", Font.PLAIN, fontSize));

                            // Render ASCII art onto the image
                            int yPosition = fontSize;
                            for (String line : asciiArt.toString().split("\n")) {
                                g2d.drawString(line, 0, yPosition);
                                yPosition += fontSize;
                            }

                            g2d.dispose();

                            // Save the ASCII art image
                            File outputFile = new File(folderPath + "/output/" + file.getName() + ".png");
                            ImageIO.write(asciiImage, "png", outputFile);

                            System.out.println("ASCII art saved as image: " + outputFile.getAbsolutePath());

                            long end = System.currentTimeMillis();
                            System.out.println("Time taken: " + (end - start) + " ms");
                        } else if (file.isDirectory()) {
                            System.out.println("Directory: " + file.getName());
                        }
                    }
                } else {
                    System.out.println("The folder is empty or cannot be accessed.");
                }
            } else {
                System.out.println("The specified path does not exist or is not a directory.");
            }

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
