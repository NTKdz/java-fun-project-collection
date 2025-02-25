package screen_capture;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ScreenCapture {
    private static final String ASCII_CHARS = "@%#*+=-:. ";
    private static final User32 user32 = User32.INSTANCE;
    private static final GDI32 gdi32 = GDI32.INSTANCE;
    private static final int SRCCOPY = 0x00CC0020; // Manually defined constant

    public interface CustomUser32 extends User32 {
        boolean PrintWindow(WinDef.HWND hwnd, WinDef.HDC hdcBlt, int nFlags);
    }

    private static final CustomUser32 CUSTOM_USER32 = Native.load("user32", CustomUser32.class);

    public static class WindowInfo {
        public WinDef.HWND hwnd;
        public String title;

        public WindowInfo(WinDef.HWND hwnd, String title) {
            this.hwnd = hwnd;
            this.title = title;
        }
    }

    public static List<WindowInfo> listWindows() {
        List<WindowInfo> windows = new ArrayList<>();
        user32.EnumWindows((hwnd, arg1) -> {
            if (user32.IsWindowVisible(hwnd)) {
                char[] titleBuffer = new char[512];
                user32.GetWindowText(hwnd, titleBuffer, 512);
                String title = Native.toString(titleBuffer);
                if (!title.isEmpty()) {
                    windows.add(new WindowInfo(hwnd, title));
                }
            }
            return true;
        }, null);
        return windows;
    }

    public static BufferedImage captureWindow(WinDef.HWND hwnd) {
        WinDef.RECT rect = new WinDef.RECT();
        user32.GetWindowRect(hwnd, rect);
        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;

        if (width <= 0 || height <= 0) {
            throw new RuntimeException("Invalid window size");
        }

        WinDef.HDC hdc = user32.GetDC(hwnd);
        WinDef.HDC memDC = gdi32.CreateCompatibleDC(hdc);
        WinGDI.BITMAPINFO bi = new WinGDI.BITMAPINFO();
        bi.bmiHeader.biSize = bi.bmiHeader.size();
        bi.bmiHeader.biWidth = width;
        bi.bmiHeader.biHeight = -height;
        bi.bmiHeader.biPlanes = 1;
        bi.bmiHeader.biBitCount = 32;
        bi.bmiHeader.biCompression = WinGDI.BI_RGB;

        // Create compatible bitmap using HGDIOBJ
        WinDef.HBITMAP hBitmap = gdi32.CreateCompatibleBitmap(hdc, width, height);
        WinNT.HANDLE old = gdi32.SelectObject(memDC, hBitmap);

        // Try PrintWindow first
        boolean pwResult = CUSTOM_USER32.PrintWindow(hwnd, memDC, 2);
        if (!pwResult) {
            // Fallback to BitBlt with manual SRCCOPY
            gdi32.BitBlt(memDC, 0, 0, width, height, hdc, 0, 0, SRCCOPY);
        }

        // Get pixel data
        int[] rawPixels = new int[width * height];
        Pointer pixelPointer = new Memory(rawPixels.length * 4); // 4 bytes per pixel (32-bit)
        gdi32.GetDIBits(memDC, hBitmap, 0, height, pixelPointer, bi, WinGDI.DIB_RGB_COLORS);

        // Convert to BufferedImage (BGR -> RGB)
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = rawPixels[y * width + x];
                image.setRGB(x, y, (0xFF << 24) | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF));
            }
        }

        // Cleanup resources
        gdi32.SelectObject(memDC, old);
        gdi32.DeleteObject(hBitmap);
        user32.ReleaseDC(hwnd, hdc);
        gdi32.DeleteDC(memDC);

        return image;
    }

    public static String imageToAscii(BufferedImage image, int cols) {
        double scale = 0.43;
        int width = image.getWidth();
        int height = image.getHeight();
        cols = Math.min(cols, width);
        int cellWidth = width / cols;
        int cellHeight = (int) (cellWidth / scale);
        int rows = height / cellHeight;

        BufferedImage resized = new BufferedImage(cols, rows, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resized.createGraphics();
        g.drawImage(image, 0, 0, cols, rows, null);
        g.dispose();

        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int pixel = resized.getRaster().getSample(x, y, 0);
                int index = (int) (pixel / 255.0 * (ASCII_CHARS.length() - 1));
                sb.append(ASCII_CHARS.charAt(index));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public static void saveAsciiAsPng(String ascii, String outputPath) throws Exception {
        Font font = new Font("Courier", Font.PLAIN, 10);
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        String[] lines = ascii.split("\n");
        int width = fm.stringWidth(lines[0]);
        int height = fm.getHeight() * lines.length;
        g2d.dispose();

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        g2d = img.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.WHITE);
        g2d.setFont(font);

        for (int i = 0; i < lines.length; i++) {
            g2d.drawString(lines[i], 0, fm.getAscent() + i * fm.getHeight());
        }
        g2d.dispose();

        ImageIO.write(img, "png", new File(outputPath));
    }

    public static void main(String[] args) {
        try {
            List<WindowInfo> windows = listWindows();
            System.out.println("Available windows:");
            for (int i = 0; i < windows.size(); i++) {
                System.out.println(i + ": " + windows.get(i).title);
            }

            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter window number: ");
            int choice = scanner.nextInt();
            scanner.close();
            WinDef.HWND hwnd = windows.get(choice).hwnd;

            long start = System.currentTimeMillis();

            // Capture and save original
            long captureStart = System.currentTimeMillis();
            BufferedImage img = captureWindow(hwnd);
            ImageIO.write(img, "png", new File("original_java.png"));
            double captureTime = (System.currentTimeMillis() - captureStart) / 1000.0;

            // ASCII conversion
            long asciiStart = System.currentTimeMillis();
            String ascii = imageToAscii(img, 100);
            double asciiTime = (System.currentTimeMillis() - asciiStart) / 1000.0;

            // Save ASCII
            long saveStart = System.currentTimeMillis();
            saveAsciiAsPng(ascii, "ascii_java.png");
            double saveTime = (System.currentTimeMillis() - saveStart) / 1000.0;

            double totalTime = (System.currentTimeMillis() - start) / 1000.0;

            System.out.println("\nPerformance Metrics:");
            System.out.printf("Capture: %.3fs\n", captureTime);
            System.out.printf("ASCII Conversion: %.3fs\n", asciiTime);
            System.out.printf("Save to PNG: %.3fs\n", saveTime);
            System.out.printf("Total Time: %.3fs\n", totalTime);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}