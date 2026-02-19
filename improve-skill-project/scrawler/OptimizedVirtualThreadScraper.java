import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.By;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class OptimizedVirtualThreadScraper {
    private static String token;
    private static final int MAX_CONCURRENT_BROWSERS = Runtime.getRuntime().availableProcessors() - 1;
    private static final Semaphore SEMAPHORE = new Semaphore(MAX_CONCURRENT_BROWSERS);
    private static final BlockingQueue<WebDriver> DRIVER_POOL = new ArrayBlockingQueue<>(MAX_CONCURRENT_BROWSERS);
    private static final Random RANDOM = new Random();
    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.7204.101 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.7204.101 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0"
    );

    // Timing metrics
    private static final AtomicLong totalFetchTime = new AtomicLong(0);
    private static final AtomicLong totalWriteTime = new AtomicLong(0);

    // Data structure to hold fetched content
    private static class ChapterData {
        final int chapterNumber;
        final String content;
        final String fileName;
        final long fetchTime;

        ChapterData(int chapterNumber, String content, String fileName, long fetchTime) {
            this.chapterNumber = chapterNumber;
            this.content = content;
            this.fileName = fileName;
            this.fetchTime = fetchTime;
        }
    }

    // Queue for decoupling fetch from write
    private static final BlockingQueue<ChapterData> writeQueue = new LinkedBlockingQueue<>();
    private static final AtomicLong chaptersWritten = new AtomicLong(0);

    public static void main(String[] args) {
        int start = 1;
        int end = 300;
        String url = "https://metruyencv.biz/truyen/ban-ton-cau-den-vo-dich-vo-han-phan-than-che-ba-tien-gioi/chuong-";
        String folderPath = "novel/optimized/ban-ton-cau-den-vo-dich-vo-han-phan-than-che-ba-tien-gioi";

        // Login
        String loginUrl = "https://backend.metruyencv.com/api/auth/login";
        String email = ""; // Replace with your email
        String password = ""; // Replace with your password

        System.out.println("=== OPTIMIZED VERSION (Virtual Threads) ===");

        try {
            token = loginAndGetToken(loginUrl, email, password);
            System.out.println("Extracted token: " + token);
        } catch (IOException e) {
            System.err.println("Login failed: " + e.getMessage());
            return;
        }

        // Initialize WebDriver pool
        initializeDriverPool();

        long startTime = System.nanoTime();

        // Start virtual thread pool for writing (completely decoupled from fetching)
        ExecutorService writerExecutor = Executors.newVirtualThreadPerTaskExecutor();
        int totalChapters = end - start + 1;

        // Launch writer threads
        List<Future<?>> writerFutures = new ArrayList<>();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            writerFutures.add(writerExecutor.submit(() -> writerTask(folderPath, totalChapters)));
        }

        // Fetch chapters using platform threads
        try (ExecutorService fetcherExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_BROWSERS * 2)) {
            List<Future<?>> fetcherFutures = new ArrayList<>();

            for (int i = start; i <= end; i++) {
                final int chapter = i;
                fetcherFutures.add(fetcherExecutor.submit(() ->
                        fetchChapter(chapter, url, folderPath, loginUrl, email, password)));
            }

            fetcherExecutor.shutdown();
            if (!fetcherExecutor.awaitTermination(2, TimeUnit.HOURS)) {
                System.err.println("Fetch tasks did not complete within the timeout.");
            }

            // Wait for all fetchers to complete
            for (Future<?> future : fetcherFutures) {
                try {
                    future.get(1, TimeUnit.MINUTES);
                } catch (TimeoutException e) {
                    System.err.println("Task timed out, cancelling...");
                    future.cancel(true);
                } catch (ExecutionException e) {
                    System.err.println("Error during task execution: " + e.getCause());
                } catch (InterruptedException e) {
                    System.err.println("Task interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("\nAll chapters fetched. Waiting for writes to complete...");

        } catch (InterruptedException e) {
            System.err.println("Executor was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            cleanupDriverPool();
        }

        // Signal writers to stop and wait for them to finish
        writerExecutor.shutdown();
        try {
            if (!writerExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println("Writers did not complete within timeout.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\nAll chapters written. Merging files...");
        long mergeStart = System.nanoTime();
        mergeFilesVirtualThread(folderPath, "A-merged-novel.txt");
        long mergeTime = System.nanoTime() - mergeStart;
        System.out.println("Merge completed in " + (mergeTime / 1_000_000.0) + " ms");

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;

        // Print detailed timing report
        printTimingReport(totalTime, mergeTime);
    }

    private static void writerTask(String folderPath, int totalChapters) {
        while (chaptersWritten.get() < totalChapters) {
            try {
                ChapterData data = writeQueue.poll(100, TimeUnit.MILLISECONDS);
                if (data != null) {
                    long writeStart = System.nanoTime();
                    saveFile(data.content, data.fileName, folderPath);
                    long writeEnd = System.nanoTime();
                    totalWriteTime.addAndGet(writeEnd - writeStart);
                    totalFetchTime.addAndGet(data.fetchTime);

                    chaptersWritten.incrementAndGet();
                    System.out.println("Written: chapter-" + data.chapterNumber +
                            " (Fetch: " + (data.fetchTime / 1_000_000.0) + " ms)");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void printTimingReport(long totalTime, long mergeTime) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TIMING REPORT - OPTIMIZED VERSION (Virtual Threads)");
        System.out.println("=".repeat(60));

        double totalSeconds = totalTime / 1_000_000_000.0;
        long minutes = (long)(totalSeconds / 60);
        double seconds = totalSeconds % 60;

        System.out.printf("Total execution time: %.3f seconds (%d min %.2f sec)%n", totalSeconds, minutes, seconds);
        System.out.printf("Total fetch time:     %.3f seconds%n", totalFetchTime.get() / 1_000_000_000.0);
        System.out.printf("Total write time:     %.3f seconds%n", totalWriteTime.get() / 1_000_000_000.0);
        System.out.printf("Merge time:          %.3f seconds%n", mergeTime / 1_000_000_000.0);
        System.out.printf("Other overhead:      %.3f seconds%n",
                (totalTime - totalFetchTime.get() - totalWriteTime.get() - mergeTime) / 1_000_000_000.0);
        System.out.printf("Chapters written:    %d%n", chaptersWritten.get());
        System.out.println("\nBENEFIT: Selenium doesn't wait for file I/O to complete!");
        System.out.println("=".repeat(60));
    }

    private static void initializeDriverPool() {
        WebDriverManager.chromedriver().setup();
        for (int i = 0; i < MAX_CONCURRENT_BROWSERS; i++) {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
            options.addArguments("--disable-extensions", "--disable-images", "--blink-settings=imagesEnabled=false");
            options.addArguments("user-agent=" + getRandomUserAgent());
            options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation", "enable-logging"));
            WebDriver driver = new ChromeDriver(options);
            DRIVER_POOL.add(driver);
        }
    }

    private static void cleanupDriverPool() {
        for (WebDriver driver : DRIVER_POOL) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error closing WebDriver: " + e.getMessage());
            }
        }
        DRIVER_POOL.clear();
    }

    private static WebDriver getDriverFromPool() throws InterruptedException {
        SEMAPHORE.acquire();
        return DRIVER_POOL.poll(10, TimeUnit.SECONDS);
    }

    private static void returnDriverToPool(WebDriver driver) {
        if (driver != null) {
            DRIVER_POOL.offer(driver);
            SEMAPHORE.release();
        }
    }

    private static String getRandomUserAgent() {
        return USER_AGENTS.get(RANDOM.nextInt(USER_AGENTS.size()));
    }

    private static boolean fileExists(String fileName, String folderPath) {
        File file = new File(folderPath + "/" + fileName);
        return file.exists() && file.length() > 0;
    }

    private static void fetchChapter(int chapter, String url, String folderPath, String loginUrl, String email, String password) {
        String fileName = "chapter-" + chapter + ".txt";
        if (fileExists(fileName, folderPath)) {
            System.out.println("Skipping chapter " + chapter + ": File already exists.");
            chaptersWritten.incrementAndGet(); // Count skipped as written
            return;
        }

        WebDriver driver = null;
        int maxAttempts = 3;
        int attempts = 0;

        try {
            driver = getDriverFromPool();
            if (driver == null) {
                System.err.println("No available WebDriver for chapter " + chapter);
                return;
            }

            if (driver.manage().getCookieNamed("accessToken") == null) {
                driver.get("https://metruyencv.biz");
                Cookie authCookie = new Cookie("accessToken", token, ".metruyencv.biz", "/", null, false, true);
                driver.manage().addCookie(authCookie);
            }

            while (attempts < maxAttempts) {
                try {
                    // FETCH PHASE - Timed (Selenium is NOT blocked by file I/O)
                    long fetchStart = System.nanoTime();

                    ((ChromeDriver) driver).executeScript("navigator.userAgent = '" + getRandomUserAgent() + "';");
                    driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
                    driver.get(url + chapter);
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(500));
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.id("chapter-content")));

                    String html = driver.getPageSource();
                    Document doc = Jsoup.parse(html);

                    if (html.contains("cf-captcha") || html.contains("g-recaptcha")) {
                        System.err.println("CAPTCHA detected for chapter " + chapter + ". Retrying after delay...");
                        attempts++;
                        Thread.sleep(5000 + RANDOM.nextInt(1000));
                        continue;
                    }

                    String verificationMessage = "Vui lòng xác thực email tài khoản của bạn";
                    if (html.contains(verificationMessage)) {
                        System.out.println("Verification message detected for chapter " + chapter);
                        attempts++;
                        Thread.sleep(1000);
                        continue;
                    }

                    if (html.contains("Unauthorized") || html.contains("Token expired")) {
                        System.out.println("Token expired for chapter " + chapter + ". Refreshing token...");
                        token = loginAndGetToken(loginUrl, email, password);
                        driver.manage().deleteCookieNamed("accessToken");
                        driver.get("https://metruyencv.biz");
                        Cookie authCookie = new Cookie("accessToken", token, ".metruyencv.biz", "/", null, false, true);
                        driver.manage().addCookie(authCookie);
                        attempts++;
                        Thread.sleep(1000);
                        continue;
                    }

                    Elements chapterContent = doc.select("#chapter-content");
                    chapterContent.select("div[id=middle-content-three]").remove();
                    chapterContent.select("div[id=middle-content-two]").remove();
                    chapterContent.select("div[id=middle-content-one]").remove();
                    chapterContent.select("canvas").remove();

                    String content = chapterContent.html()
                            .replaceAll("(?i)<br\\s*/?>\\s*(<br\\s*/?>\\s*)+", "<br>")
                            .replace("<br>", "\n");

                    long fetchEnd = System.nanoTime();
                    long fetchTime = fetchEnd - fetchStart;

                    if (content.isEmpty()) {
                        System.err.println("Content is empty for chapter " + chapter + ". Check authentication or page structure.");
                        attempts++;
                        Thread.sleep(1000);
                        continue;
                    } else {
                        // Push to write queue instead of writing directly
                        // Selenium can immediately continue to next chapter!
                        ChapterData chapterData = new ChapterData(chapter, content, fileName, fetchTime);
                        writeQueue.put(chapterData);
                        System.out.println("Fetched: chapter-" + chapter + " -> queued for writing");
                    }
                    return;
                } catch (Exception e) {
                    attempts++;
                    System.err.println("Attempt " + attempts + " failed for chapter " + chapter + ": " + e.getMessage());
                    if (attempts == maxAttempts) {
                        System.err.println("Skipping chapter " + chapter + " after " + maxAttempts + " failed attempts.");
                        return;
                    }
                    Thread.sleep(1000L * (1L << attempts) + RANDOM.nextInt(100));
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted during chapter " + chapter + ": " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            returnDriverToPool(driver);
        }
    }

    public static void saveFile(String content, String fileName, String folderPath) {
        Path file = Path.of(folderPath, fileName);
        try {
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(content);
            }
        } catch (IOException e) {
            System.err.println("Error saving file " + fileName + ": " + e.getMessage());
        }
    }

    // Merge using virtual threads for parallel processing
    public static void mergeFilesVirtualThread(String folderPath, String mergedFileName) {
        Path mergedFile = Path.of(folderPath, mergedFileName);
        File folder = new File(folderPath);

        try (FileChannel mergedChannel = FileChannel.open(
                mergedFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {
            File[] files = folder.listFiles((dir, name) -> name.startsWith("chapter-") && name.endsWith(".txt"));

            if (files == null || files.length == 0) {
                System.out.println("⚠ No chapter files found in: " + folderPath);
                return;
            }

            Arrays.sort(files, Comparator.comparingInt(
                    f -> Integer.parseInt(f.getName().replaceAll("\\D", "")))
            );

            for (File file : files) {
                String header = "**CHAPTER " + file.getName().replaceAll("\\D", "") + "\n\n";
                mergedChannel.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));

                try (FileChannel chapterChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
                    chapterChannel.transferTo(0, chapterChannel.size(), mergedChannel);
                }

                mergedChannel.write(ByteBuffer.wrap("\n\n".getBytes(StandardCharsets.UTF_8)));
            }

            System.out.println("✅ All chapters merged into: " + mergedFile.toAbsolutePath());

        } catch (IOException e) {
            System.err.printf("❌ Error merging files: %s%n", e.getMessage());
        }
    }

    public static String loginAndGetToken(String loginUrl, String email, String password) throws IOException {
        Connection.Response loginResponse = Jsoup.connect(loginUrl)
                .header("Content-Type", "application/json")
                .requestBody("{\"email\": \"" + email + "\", \"password\": \"" + password + "\", \"remember\": 1, \"device_name\": \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.7204.101 Safari/537.36\"}")
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .execute();

        String responseBody = loginResponse.body();
        if (responseBody.contains("\"accessToken\":")) {
            int tokenStart = responseBody.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
            int tokenEnd = responseBody.indexOf("\"", tokenStart);
            return responseBody.substring(tokenStart, tokenEnd);
        } else if (responseBody.contains("\"token\":")) {
            int tokenStart = responseBody.indexOf("\"token\":\"") + "\"token\":\"".length();
            int tokenEnd = responseBody.indexOf("\"", tokenStart);
            return responseBody.substring(tokenStart, tokenEnd);
        } else {
            throw new IOException("Token not found in response: " + responseBody);
        }
    }
}