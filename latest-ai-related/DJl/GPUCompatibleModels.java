import ai.djl.Application;
import ai.djl.Device;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.CategoryMask;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import ai.djl.modality.Classifications;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Examples of PyTorch models that work well with GPU
 * These models don't have the device mismatch issues
 */
public class GPUCompatibleModels {

    // ============ OBJECT DETECTION ============

    /**
     * Single Shot Detection (SSD) - Object Detection
     * Works well with GPU
     */
    public static void runSSD() throws IOException, ModelException, TranslateException {
        Image img = ImageFactory.getInstance().fromUrl(
                "https://github.com/pytorch/hub/raw/master/images/dog.jpg");

        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optApplication(Application.CV.OBJECT_DETECTION)
                .optEngine("PyTorch")
                .optModelUrls("djl://ai.djl.pytorch/ssd")
                .optDevice(Device.gpu())
                .build();

        try (ZooModel<Image, DetectedObjects> model = criteria.loadModel();
             Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            DetectedObjects result = predictor.predict(img);
            System.out.println("SSD Detection Results: " + result);
        }
    }

    /**
     * YOLOv5 - Object Detection (faster and more accurate than SSD)
     * Works well with GPU
     */
    public static void runYOLOv5() throws IOException, ModelException, TranslateException {
        Image img = ImageFactory.getInstance().fromUrl(
                "https://github.com/pytorch/hub/raw/master/images/dog.jpg");

        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optApplication(Application.CV.OBJECT_DETECTION)
                .optEngine("PyTorch")
                .optFilter("backbone", "yolov5s")
                .optDevice(Device.gpu())
                .build();

        try (ZooModel<Image, DetectedObjects> model = criteria.loadModel();
             Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            DetectedObjects result = predictor.predict(img);
            System.out.println("YOLOv5 Detection Results: " + result);
        }
    }

    // ============ IMAGE CLASSIFICATION ============

    /**
     * ResNet50 - Image Classification
     * Works well with GPU
     */
    public static void runResNet50() throws IOException, ModelException, TranslateException {
        Image img = ImageFactory.getInstance().fromUrl(
                "https://github.com/pytorch/hub/raw/master/images/dog.jpg");

        Criteria<Image, Classifications> criteria = Criteria.builder()
                .setTypes(Image.class, Classifications.class)
                .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                .optEngine("PyTorch")
                .optFilter("dataset", "imagenet")
                .optFilter("layers", "50")
                .optDevice(Device.gpu())
                .build();

        try (ZooModel<Image, Classifications> model = criteria.loadModel();
             Predictor<Image, Classifications> predictor = model.newPredictor()) {
            Classifications result = predictor.predict(img);
            System.out.println("ResNet50 Classification: " + result);
        }
    }

    /**
     * MobileNet - Lightweight Image Classification
     * Works well with GPU (good for mobile/edge deployment)
     */
    public static void runMobileNet() throws IOException, ModelException, TranslateException {
        Image img = ImageFactory.getInstance().fromUrl(
                "https://github.com/pytorch/hub/raw/master/images/dog.jpg");

        Criteria<Image, Classifications> criteria = Criteria.builder()
                .setTypes(Image.class, Classifications.class)
                .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                .optEngine("PyTorch")
                .optFilter("backbone", "mobilenet")
                .optDevice(Device.gpu())
                .build();

        try (ZooModel<Image, Classifications> model = criteria.loadModel();
             Predictor<Image, Classifications> predictor = model.newPredictor()) {
            Classifications result = predictor.predict(img);
            System.out.println("MobileNet Classification: " + result);
        }
    }

    // ============ INSTANCE SEGMENTATION ============

    /**
     * Mask R-CNN - Instance Segmentation
     * Works well with GPU
     */
    public static void runMaskRCNN() throws IOException, ModelException, TranslateException {
        Image img = ImageFactory.getInstance().fromUrl(
                "https://github.com/pytorch/hub/raw/master/images/dog.jpg");

        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optApplication(Application.CV.INSTANCE_SEGMENTATION)
                .optEngine("PyTorch")
                .optFilter("backbone", "resnet50")
                .optDevice(Device.gpu())
                .build();

        try (ZooModel<Image, DetectedObjects> model = criteria.loadModel();
             Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            DetectedObjects result = predictor.predict(img);
            System.out.println("Mask R-CNN Segmentation: " + result);
        }
    }

    // ============ POSE ESTIMATION ============

    /**
     * SimplePose - Pose Estimation
     * Works well with GPU
     */
    public static void runSimplePose() throws IOException, ModelException, TranslateException {
        Image img = ImageFactory.getInstance().fromUrl(
                "https://github.com/pytorch/hub/raw/master/images/dog.jpg");

        Criteria<Image, ai.djl.modality.cv.output.Joints> criteria = Criteria.builder()
                .setTypes(Image.class, ai.djl.modality.cv.output.Joints.class)
                .optApplication(Application.CV.POSE_ESTIMATION)
                .optEngine("PyTorch")
                .optFilter("backbone", "resnet50")
                .optDevice(Device.gpu())
                .build();

        try (ZooModel<Image, ai.djl.modality.cv.output.Joints> model = criteria.loadModel();
             Predictor<Image, ai.djl.modality.cv.output.Joints> predictor = model.newPredictor()) {
            ai.djl.modality.cv.output.Joints result = predictor.predict(img);
            System.out.println("Pose Estimation Results: " + result);
        }
    }

    // ============ STYLE TRANSFER ============

    /**
     * Fast Neural Style Transfer
     * Works well with GPU
     */
    public static void runStyleTransfer() throws IOException, ModelException, TranslateException {
        Image img = ImageFactory.getInstance().fromUrl(
                "https://github.com/pytorch/hub/raw/master/images/dog.jpg");

        Criteria<Image, Image> criteria = Criteria.builder()
                .setTypes(Image.class, Image.class)
                .optApplication(Application.CV.IMAGE_GENERATION)
                .optEngine("PyTorch")
                .optFilter("artist", "cezanne")
                .optDevice(Device.gpu())
                .build();

        try (ZooModel<Image, Image> model = criteria.loadModel();
             Predictor<Image, Image> predictor = model.newPredictor()) {
            Image result = predictor.predict(img);
            System.out.println("Style Transfer completed!");
            result.save(Files.newOutputStream(Paths.get("stylized_output.png")), "png");
        }
    }

    public static void main(String[] args) throws IOException, ModelException, TranslateException {
        System.out.println("=== GPU-Compatible PyTorch Models ===\n");

        // Run whichever model you want to test
        System.out.println("1. Running SSD Object Detection...");
        runSSD();

        System.out.println("\n2. Running ResNet50 Image Classification...");
        runResNet50();

        // Uncomment to try other models:
        // runYOLOv5();
        // runMobileNet();
        // runMaskRCNN();
        // runSimplePose();
        // runStyleTransfer();
    }
}