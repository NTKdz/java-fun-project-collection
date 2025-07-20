package desktop.app.desktopassistant.automation;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import org.sikuli.script.*;
import java.io.File;

public class CalculatorAutomation {
    public static void main(String[] args) {
        // Initialize SikuliX
        Screen screen = new Screen();
        ImagePath.setBundlePath("images");

        // Print environment details
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
        System.out.println("SikuliX ImagePath: " + ImagePath.getBundlePath());

        // Validate image files
        String[] imageFiles = {"5.png", "plus.png", "3.png", "equals.png"};
        for (String img : imageFiles) {
            File file = new File(ImagePath.getBundlePath() + "\\" + img);
            if (!file.exists()) {
                System.err.println("Image file not found: " + file.getAbsolutePath());
                return;
            }
        }

        try {
            // Open Calculator based on OS
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
//                Runtime.getRuntime().exec("cmd /c start \"\" \"C:\\Users\\ADMIN\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Steam\\CounterSide.url\"");
                Runtime.getRuntime().exec("calc");
            } else {
                System.err.println("Unsupported OS: " + os);
                return;
            }
            Thread.sleep(2000); // Wait for Calculator to open
            String targetProcess = "calculatorapp.exe";
            boolean running = isProcessRunning(targetProcess);
            System.out.println(targetProcess + " running? " + running);
            // Wait for Calculator to appear
//            System.out.println("Waiting for Calculator...");
//            screen.wait("calculator_window.png", 10);
            // Perform calculation: 5 + 3
//            System.out.println("Clicking button 5...");
//            screen.click(new Pattern("5.png").similar(0.8f));
//            System.out.println("Clicking plus button...");
//            screen.click(new Pattern("plus.png").similar(0.8f));
//            System.out.println("Clicking button 3...");
//            screen.click(new Pattern("3.png").similar(0.8f));
//            System.out.println("Clicking equals button...");
//            screen.click(new Pattern("equals.png").similar(0.8f));
//            System.out.println("Calculation complete.");

        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public static boolean isProcessRunning(String processName) {
        Tlhelp32.PROCESSENTRY32.ByReference processEntry = new Tlhelp32.PROCESSENTRY32.ByReference();
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));

        try {
            while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry)) {
                if (Native.toString(processEntry.szExeFile).equalsIgnoreCase(processName)) {
                    return true;
                }
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }
        return false;
    }
}