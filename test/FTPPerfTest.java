import org.apache.commons.net.ftp.FTPClient;
import java.io.*;

public class FTPPerfTest {
    public static void main(String[] args) {
        FTPClient ftp = new FTPClient();

        try {
            long start = System.currentTimeMillis();

            ftp.connect("your-ftp-host");
            ftp.login("username", "password");
            ftp.enterLocalPassiveMode();

            // Upload
            try (InputStream input = new FileInputStream("testfile.dat")) {
                ftp.storeFile("/remote_testfile.dat", input);
            }

            // Download
            try (OutputStream output = new FileOutputStream("download_testfile.dat")) {
                ftp.retrieveFile("/remote_testfile.dat", output);
            }

            long end = System.currentTimeMillis();
            System.out.println("Total time (ms): " + (end - start));

            ftp.logout();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { ftp.disconnect(); } catch (Exception ignored) {}
        }
    }
}
