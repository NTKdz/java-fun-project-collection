import org.bytedeco.opencv.opencv_core.Mat;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

public class AspectRatioChecker {
    public static void main(String[] args) {
        String imagePath = "C:\\Users\\ADMIN\\OneDrive\\Pictures\\cccd\\khoi-png-Photoroomz.png";

        if (isAspectRatio3x4(imagePath)) {
            System.out.println("The image has a 3x4 aspect ratio.");
        } else {
            System.out.println("The image does not have a 3x4 aspect ratio.");
        }
    }

    public static boolean isAspectRatio3x4(String imagePath) {
        // Load the image
        Mat image = imread(imagePath);

        // Check if the image is loaded successfully
        if (image.empty()) {
            System.out.println("Error: Unable to load image.");
            return false;
        }

        // Get image dimensions
        int width = image.cols();
        int height = image.rows();

        // Calculate the aspect ratio
        double aspectRatio = (double) width / height;
        System.out.println("The aspect ratio is " + aspectRatio + " " + (3.0 / 4.0));
        // Check if the aspect ratio is approximately 3/4
        return Math.abs(aspectRatio - (3.0 / 4.0)) < 0.01; // Allowing a small margin for rounding errors
    }
}
