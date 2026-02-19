import ai.djl.Device;
import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;

/**
 * GPU Verification Utility for DJL
 *
 * Use this to diagnose why your Java DQN might be running slowly.
 * Run this BEFORE training to verify your setup.
 */
public class GPUVerification {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          DJL Performance Diagnostic Tool                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        // 1. Check Engine
        checkEngine();

        // 2. Check Devices
        checkDevices();

        // 3. Check GPU Count
        checkGPUCount();

        // 4. Performance Test
        performanceTest();

        // 5. Recommendations
        printRecommendations();
    }

    private static void checkEngine() {
        System.out.println("1️⃣  ENGINE INFORMATION:");
        System.out.println("─────────────────────────────────────────────────────────────");

        Engine engine = Engine.getInstance();
        System.out.println("Engine Name: " + engine.getEngineName());
        System.out.println("Engine Version: " + engine.getVersion());
        System.out.println();

        if (engine.getEngineName().equals("PyTorch")) {
            System.out.println("⚠️  Using PyTorch engine");
            System.out.println("   Note: PyTorch in Java has more overhead than MXNet");
            System.out.println("   Consider switching to MXNet for better performance");
        } else if (engine.getEngineName().equals("MXNet")) {
            System.out.println("✅ Using MXNet engine (optimal for Java)");
        }
        System.out.println();
    }

    private static void checkDevices() {
        System.out.println("2️⃣  DEVICE INFORMATION:");
        System.out.println("─────────────────────────────────────────────────────────────");

        Engine engine = Engine.getInstance();
        Device defaultDevice = engine.defaultDevice();

        System.out.println("Default Device: " + defaultDevice);
        System.out.println("Device Type: " + defaultDevice.getDeviceType());
        System.out.println("Device ID: " + defaultDevice.getDeviceId());

        if (defaultDevice.isGpu()) {
            System.out.println("\n✅ GPU IS ENABLED!");
            System.out.println("   Your code will run on GPU");
        } else {
            System.out.println("\n❌ WARNING: RUNNING ON CPU!");
            System.out.println("   This will be VERY slow for deep learning");
            System.out.println("\n   Possible causes:");
            System.out.println("   1. No GPU available on this machine");
            System.out.println("   2. CUDA not installed");
            System.out.println("   3. Wrong DJL native library (check pom.xml)");
            System.out.println("   4. CUDA version mismatch");
        }
        System.out.println();
    }

    private static void checkGPUCount() {
        System.out.println("3️⃣  GPU COUNT:");
        System.out.println("─────────────────────────────────────────────────────────────");

        Engine engine = Engine.getInstance();
        int gpuCount = engine.getGpuCount();

        System.out.println("GPUs Available: " + gpuCount);

        if (gpuCount > 0) {
            System.out.println("✅ GPU(s) detected");
            System.out.println("\n   Available devices:");
            for (int i = 0; i < gpuCount; i++) {
                System.out.println("   - GPU " + i + ": cuda:" + i);
            }
        } else {
            System.out.println("❌ No GPUs detected");
            System.out.println("   Training will be 10-50x slower on CPU");
        }
        System.out.println();
    }

    private static void performanceTest() {
        System.out.println("4️⃣  PERFORMANCE TEST:");
        System.out.println("─────────────────────────────────────────────────────────────");

        try (NDManager manager = NDManager.newBaseManager()) {
            Engine engine = Engine.getInstance();
            Device device = engine.defaultDevice();

            // Test matrix multiplication
            int size = 1000;
            System.out.println("Testing " + size + "x" + size + " matrix multiplication...");

            NDArray a = manager.randomUniform(0, 1, new ai.djl.ndarray.types.Shape(size, size),
                    ai.djl.ndarray.types.DataType.FLOAT32, device);
            NDArray b = manager.randomUniform(0, 1, new ai.djl.ndarray.types.Shape(size, size),
                    ai.djl.ndarray.types.DataType.FLOAT32, device);

            // Warmup
            for (int i = 0; i < 3; i++) {
                a.matMul(b);
            }

            // Actual test
            long start = System.nanoTime();
            int iterations = 10;
            for (int i = 0; i < iterations; i++) {
                NDArray result = a.matMul(b);
                result.close();
            }
            long end = System.nanoTime();

            double timeMs = (end - start) / 1_000_000.0;
            double avgTimeMs = timeMs / iterations;

            System.out.println("Total time: " + String.format("%.2f", timeMs) + " ms");
            System.out.println("Average per operation: " + String.format("%.2f", avgTimeMs) + " ms");

            // Interpret results
            System.out.println();
            if (device.isGpu()) {
                if (avgTimeMs < 10) {
                    System.out.println("✅ EXCELLENT: GPU is working well!");
                } else if (avgTimeMs < 50) {
                    System.out.println("⚠️  MODERATE: GPU is working but may be slower than expected");
                } else {
                    System.out.println("❌ SLOW: GPU seems to be working poorly");
                    System.out.println("   Check CUDA installation and drivers");
                }
            } else {
                if (avgTimeMs < 100) {
                    System.out.println("⚠️  Running on CPU (fast CPU detected)");
                } else if (avgTimeMs < 500) {
                    System.out.println("⚠️  Running on CPU (moderate speed)");
                } else {
                    System.out.println("❌ Running on CPU (very slow)");
                }
                System.out.println("   Training will be 10-50x slower than with GPU");
            }

            a.close();
            b.close();
        }
        System.out.println();
    }

    private static void printRecommendations() {
        System.out.println("5️⃣  RECOMMENDATIONS:");
        System.out.println("─────────────────────────────────────────────────────────────");

        Engine engine = Engine.getInstance();
        Device device = engine.defaultDevice();
        int gpuCount = engine.getGpuCount();

        if (!device.isGpu() && gpuCount == 0) {
            System.out.println("🔴 CRITICAL: No GPU available");
            System.out.println();
            System.out.println("To fix:");
            System.out.println("1. Install CUDA Toolkit (https://developer.nvidia.com/cuda-downloads)");
            System.out.println("2. Install cuDNN");
            System.out.println("3. Update pom.xml to use correct native library:");
            System.out.println();
            System.out.println("   For CUDA 11.8:");
            System.out.println("   <dependency>");
            System.out.println("       <groupId>ai.djl.pytorch</groupId>");
            System.out.println("       <artifactId>pytorch-native-cu118</artifactId>");
            System.out.println("       <version>2.1.1</version>");
            System.out.println("   </dependency>");
            System.out.println();
            System.out.println("   OR use auto-detection:");
            System.out.println("   <dependency>");
            System.out.println("       <groupId>ai.djl.pytorch</groupId>");
            System.out.println("       <artifactId>pytorch-native-auto</artifactId>");
            System.out.println("       <version>2.1.1</version>");
            System.out.println("   </dependency>");
        } else if (device.isGpu()) {
            System.out.println("✅ GPU is working!");
            System.out.println();
            System.out.println("Additional optimizations:");
            System.out.println("1. Use these JVM flags:");
            System.out.println("   java -Xmx4g -XX:+UseG1GC -XX:+AggressiveOpts");
            System.out.println();
            System.out.println("2. Consider switching to MXNet for better Java performance:");
            System.out.println("   <dependency>");
            System.out.println("       <groupId>ai.djl.mxnet</groupId>");
            System.out.println("       <artifactId>mxnet-engine</artifactId>");
            System.out.println("       <version>0.30.0</version>");
            System.out.println("   </dependency>");
            System.out.println();
            System.out.println("3. Use batch operations and sub-managers");
        }

        System.out.println("\n─────────────────────────────────────────────────────────────");
        System.out.println("For more information, see JAVA_PERFORMANCE_ANALYSIS.md");
        System.out.println("═════════════════════════════════════════════════════════════");
    }
}