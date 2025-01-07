package com.personal.testapp;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class HomepageController {

    @FXML
    private TextField youtubeLinkField;

    @FXML
    private TextField filePathField;

    @FXML
    private TextArea customCommandField;

    @FXML
    protected void onBrowseButtonClick() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("JavaFX Projects");

        File defaultDirectory = new File("C:\\study\\Coding-Project\\java-project\\java-fun-project-collection\\test-app\\download");
        chooser.setInitialDirectory(defaultDirectory);

        Stage stage = (Stage) filePathField.getScene().getWindow();
        File selectedFile = chooser.showDialog(stage);


        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    protected void onDownloadButtonClick() throws IOException {
        String youtubeLink = youtubeLinkField.getText();
        String filePath = filePathField.getText();
        String customCommand = customCommandField.getText();

//        if (youtubeLink.isEmpty() || filePath.isEmpty()) {
//            System.out.println("Please provide both the YouTube link and file save location.");
//            return;
//        }

        System.out.println("Downloading...");
        System.out.println("YouTube Link: " + youtubeLink);
        System.out.println("Save Location: " + filePath);

        if (customCommand.isEmpty()) {
            customCommand = "";
        }

        // Build the command as a single string
        String[] command = {
                "yt-dlp",
                "--yes-playlist", filePath + "/%(playlist_title)s/%(title)s.%(ext)s",
                "https://www.youtube.com/watch?v=mh0dwDLyVXI&list=PL71H7TO8jIQaDqbJnmOTkhyJtE9HIj3lC&index=1"
        };

        // Add custom commands if any
        if (!customCommand.isEmpty()) {
            String[] customCommandParts = customCommand.split(" ");
            String[] finalCommand = new String[command.length + customCommandParts.length];
            System.arraycopy(command, 0, finalCommand, 0, command.length);
            System.arraycopy(customCommandParts, 0, finalCommand, command.length, customCommandParts.length);
            command = finalCommand;
        }

        try {
            // Create and start the process
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Combine stdout and stderr
            Process process = processBuilder.start();

            // Read and print the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Playlist download completed successfully.");
            } else {
                System.out.println("Error: yt-dlp process exited with code " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
