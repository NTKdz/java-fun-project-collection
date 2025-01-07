import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_videoio.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

public class VideoProcessor {

    public static void copyAndRenamePngFiles(String sourceFolder, String destinationFolder) {
        File sourceDir = new File(sourceFolder);
        File destDir = new File(destinationFolder);

        // Ensure the destination directory exists
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        // Get all PNG files in the source directory
        File[] pngFiles = sourceDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

        if (pngFiles == null || pngFiles.length == 0) {
            System.out.println("No PNG files found in the source folder.");
            return;
        }

        int count = 0;
        for (File file : pngFiles) {
            // Create a new name for the file
            String newFileName = "frame_" + count + ".jpg";
            File destFile = new File(destDir, newFileName);

            try {
                // Copy the file to the destination folder
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copied and renamed: " + file.getName() + " -> " + newFileName);
                count++;
            } catch (IOException e) {
                System.err.println("Error copying file: " + file.getName());
                e.printStackTrace();
            }
        }

        System.out.println("Completed copying and renaming " + count + " files.");
    }

    public static void main(String[] args) throws IOException {
        String videoPath = "video/short.mp4";
        String ffmpegOutputFolder = "video/frames_ffmpeg/";
        String opencvOutputFolder = "video/frames_opencv/";
        String sourceFolder = "C:\\game\\vs\\cg\\Sprites";
        String destinationFolder = "video/copy";

//        copyAndRenamePngFiles(sourceFolder, destinationFolder);
        // Process with FFmpeg
        long ffmpegStartTime = System.currentTimeMillis();
//        processWithFFmpeg(videoPath, ffmpegOutputFolder);
        long ffmpegEndTime = System.currentTimeMillis();

        // Process with OpenCV
        long opencvStartTime = System.currentTimeMillis();
//        processWithOpenCV(videoPath, opencvOutputFolder);
        long opencvEndTime = System.currentTimeMillis();
        framesToVideo("video/output/", "video/output_video_short.mp4");
        // Print times
        System.out.println("FFmpeg Processing Time: " + (ffmpegEndTime - ffmpegStartTime) + " ms");
        System.out.println("OpenCV Processing Time: " + (opencvEndTime - opencvStartTime) + " ms");
    }

    public static void processWithFFmpeg(String videoPath, String outputFolder) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath)) {
            grabber.start();

            int frameNumber = 0;
            Frame frame;
            while ((frame = grabber.grab()) != null) {
                if (frame.image != null) {
                    Mat mat = new Mat();
                    String frameFile = outputFolder + "frame_" + frameNumber + ".jpg";
                    imwrite(frameFile, mat);
                    System.out.println("[FFmpeg] Saved: " + frameFile);
                    frameNumber++;
                }
            }

            grabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void processWithOpenCV(String videoPath, String outputFolder) throws IOException {
        VideoCapture capture = new VideoCapture(videoPath);
        if (!capture.isOpened()) {
            System.out.println("Error: Unable to open video file.");
            return;
        }

        Mat frame = new Mat();
        int frameNumber = 0;

        while (capture.read(frame)) {
            if (frame.empty()) break;

            String frameFile = outputFolder + "frame_" + frameNumber + ".jpg";
            imwrite(frameFile, frame);
            System.out.println("[OpenCV] Saved: " + frameFile);
            ASCIIToImage.toASCII(frameFile);
            frameNumber++;
        }

        capture.release();
    }

    public static void framesToVideo(String framesFolder, String outputVideoPath) {
        int frameRate = 60; // Desired frames per second

        // Codec setup for MP4
        String codec = "H264";  // You can also try "MJPG" if "H264" doesn't work
        int fourcc = VideoWriter.fourcc((byte) codec.charAt(0), (byte) codec.charAt(1), (byte) codec.charAt(2), (byte) codec.charAt(3));

        // Read the first frame to determine video dimensions
        String firstFramePath = framesFolder + "frame_0.jpg";  // Use .png for your frame extension
        Mat firstFrame = imread(firstFramePath);

        if (firstFrame.empty()) {
            System.out.println("Error: First frame not found!");
            return;
        }

        // Resize if needed (optional, to avoid handling large resolutions like 6912x3240)
        int targetWidth = 1920;  // Set desired width
        int targetHeight = 1080; // Set desired height
        Size targetSize = new Size(targetWidth, targetHeight);
        Mat resizedFrame = new Mat();
        resize(firstFrame, resizedFrame, targetSize);

        // Verify frame size
        System.out.println("Resized Frame Size: " + resizedFrame.cols() + "x" + resizedFrame.rows());
        if (resizedFrame.cols() <= 0 || resizedFrame.rows() <= 0) {
            System.out.println("Error: Invalid frame dimensions!");
            return;
        }

        // Ensure output directory exists
        File outputFile = new File(outputVideoPath);
        File parentDir = outputFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            System.out.println("Error: Unable to create output directory!");
            return;
        }

        // Define video writer
        Size frameSize = new Size(resizedFrame.cols(), resizedFrame.rows());
        VideoWriter videoWriter = new VideoWriter(outputVideoPath, fourcc, frameRate, frameSize);

        if (!videoWriter.isOpened()) {
            System.out.println("Error: Unable to open video writer. Check codec or output path.");
            return;
        }

        int frameNumber = 0;
        while (true) {
            String framePath = framesFolder + "frame_" + frameNumber + ".jpg";  // Ensure correct file extension (.png)
            File frameFile = new File(framePath);
            if (!frameFile.exists()) break;

            Mat frame = imread(framePath);
            if (frame.empty()) {
                System.out.println("Warning: Skipping empty frame at " + framePath);
                frameNumber++;
                continue;
            }

            // Resize frame to match target size before adding to video
            resize(frame, resizedFrame, targetSize);
            videoWriter.write(resizedFrame);
            System.out.println("Added frame: " + framePath);
            frameNumber++;
        }

        videoWriter.release();
        System.out.println("Video created: " + outputVideoPath);
    }

}
