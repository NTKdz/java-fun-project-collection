import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class ThreadBenchmark {
    private static final int TASKS = 10_000;
    private static final int CPU_WORK = 20_000; // prime calc depth
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Random random = new Random();

    // store results for summary
    private static final List<Result> results = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        List<String> workloads = List.of("io", "cpu", "mixed", "file");

        for (String w : workloads) {
            System.out.println("\n=== Workload: " + w + " ===");

            runScenario(w,
                    Executors.newFixedThreadPool(
                            Runtime.getRuntime().availableProcessors() * 2),
                    "Platform Fixed");

            runScenario(w, Executors.newCachedThreadPool(), "Platform Cached");

            runScenario(w, Executors.newVirtualThreadPerTaskExecutor(), "Virtual Threads");
        }

        printSummary();
    }

    private static void runScenario(String workload, ExecutorService executor, String label) throws Exception {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Runtime rt = Runtime.getRuntime();

        int peakThreads = 0;
        long peakMem = 0;

        Instant start = Instant.now();
        CompletableFuture<?>[] futures = new CompletableFuture[TASKS];

        for (int i = 0; i < TASKS; i++) {
            Runnable task = switch (workload) {
                case "io" -> ThreadBenchmark::simulateIO;
                case "cpu" -> () -> simulateCPU(CPU_WORK);
                case "mixed" -> () -> { simulateCPU(CPU_WORK / 10); simulateIO(); };
                case "file" -> ThreadBenchmark::simulateFileIO;
                case "http" -> ThreadBenchmark::simulateHttpCall;
                default -> throw new IllegalArgumentException("Unknown workload: " + workload);
            };

            futures[i] = CompletableFuture.runAsync(task, executor);

            // periodic log
            if (i % 2000 == 0 && i > 0) {
                int threadCount = threadMXBean.getThreadCount();
                long usedMem = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
                double load = osBean.getSystemLoadAverage();

                System.out.printf("%s running... tasks=%d threads=%d mem=%dMB load=%.2f%n",
                        label, i, threadCount, usedMem, load);

                peakThreads = Math.max(peakThreads, threadCount);
                peakMem = Math.max(peakMem, usedMem);
            }
        }

        CompletableFuture.allOf(futures).join();
        Instant end = Instant.now();

        int finalThreads = threadMXBean.getThreadCount();
        long finalMem = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

        peakThreads = Math.max(peakThreads, finalThreads);
        peakMem = Math.max(peakMem, finalMem);

        long elapsed = Duration.between(start, end).toMillis();
        System.out.printf("%s → %d ms, finalThreads=%d, peakThreads=%d, peakMem=%dMB%n",
                label, elapsed, finalThreads, peakThreads, peakMem);

        results.add(new Result(workload, label, elapsed, peakThreads, peakMem));

        executor.shutdown();
    }

    // ---------------- Workload Simulations ----------------
    private static void simulateIO() {
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
    }

    private static void simulateCPU(int n) {
        int count = 0;
        for (int i = 2; i < n; i++) if (isPrime(i)) count++;
    }

    private static boolean isPrime(int x) {
        if (x < 2) return false;
        for (int i = 2; i * i <= x; i++) if (x % i == 0) return false;
        return true;
    }

    private static void simulateFileIO() {
        try {
            Path tmp = Files.createTempFile("bench", ".txt");
            Files.writeString(tmp, "Hello World " + random.nextInt());
            Files.readString(tmp);
            Files.delete(tmp);
        } catch (IOException ignored) {}
    }

    private static void simulateHttpCall() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://httpbin.org/delay/1"))
                    .build();
            httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {}
    }

    // ---------------- Summary ----------------
    private static void printSummary() {
        System.out.println("\n=== Benchmark Summary ===");
        System.out.printf("%-10s %-15s %-10s %-12s %-10s%n",
                "Workload", "Executor", "Time(ms)", "PeakThreads", "PeakMem(MB)");
        System.out.println("-------------------------------------------------------------");

        for (Result r : results) {
            System.out.printf("%-10s %-15s %-10d %-12d %-10d%n",
                    r.workload, r.executor, r.timeMs, r.peakThreads, r.peakMem);
        }
    }

    private record Result(String workload, String executor, long timeMs, int peakThreads, long peakMem) {}
}
