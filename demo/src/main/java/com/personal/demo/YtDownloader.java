package com.personal.demo;

import java.io.*;
import java.net.URL;
import java.nio.file.*;

public class YtDownloader {
    public static void main(String[] args) {
        String videoUrl = "https://www.youtube.com/watch?v=Megsdci7jUc";
        System.out.println("Video URL: " + videoUrl);

        try {
            // Extract yt-dlp.exe
            extractResource("/com/personal/demo/yt-dlp.exe", "yt-dlp.exe");

            // Extract the _internal folder
            extractFolder("/com/personal/demo/_internal", "_internal");

            // Use ProcessBuilder to run yt-dlp.exe
            String outputPath = "downloads/%(title)s.%(ext)s";
            ProcessBuilder processBuilder = new ProcessBuilder(
                    new File("yt-dlp.exe").getAbsolutePath(),
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

    // Method to extract a single resource file
    private static void extractResource(String resourcePath, String outputFileName) throws IOException {
        InputStream resourceStream = YtDownloader.class.getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        // Write the resource to a file
        try (OutputStream outStream = new FileOutputStream(outputFileName)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = resourceStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }
        }
    }

    // Method to extract an entire folder (recursively) from the JAR to the file system
    private static void extractFolder(String resourcePath, String outputFolderName) throws IOException {
        // Create the target directory
        File outputFolder = new File(outputFolderName);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        // Get the list of files inside the _internal folder in the JAR
        File folder = new File(YtDownloader.class.getResource(resourcePath).getPath());
        if (folder.exists() && folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    extractFolder(resourcePath + "/" + file.getName(), outputFolderName + "/" + file.getName());
                } else {
                    extractResource(resourcePath + "/" + file.getName(), outputFolderName + "/" + file.getName());
                }
            }
        }
    }
}
