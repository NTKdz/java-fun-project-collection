import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

public class YtDownloader {
    public static void main(String[] args) {
        // Check if the URL argument is passed
//        if (args.length < 1) {
//            System.out.println("Please provide a YouTube URL as the first argument.");
//            return;
//        }

        // Get the YouTube URL from the command line argument
        String videoUrl = "https://www.youtube.com/watch?v=Megsdci7jUc";
        System.out.println("Video URL: " + videoUrl);

        try {
            // Get the path of the JAR fil

            // Correctly resolve the path to yt-dlp.exe
            String ytDlpPath = new File("resources/yt-dlp.exe").getAbsolutePath();
            System.out.println("yt-dlp path: " + ytDlpPath);

            String outputPath = "downloads/%(title)s.%(ext)s";

            // Use ProcessBuilder to construct the command
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "yt-dlp",
                    "-o", outputPath,
                    videoUrl
            );

            // Redirect error and standard output streams
            processBuilder.redirectErrorStream(true);

            // Start the process
            Process process = processBuilder.start();

            // Capture output (stdout and stderr)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();
            System.out.println("yt-dlp process exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
