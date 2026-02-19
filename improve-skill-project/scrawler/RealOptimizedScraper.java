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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimized Scraper - Focuses on REAL bottlenecks:
 * 1. Reduce Selenium overhead per request
 * 2. Smarter timeout handling
 * 3. Better concurrency control
 * 4. Eliminate unnecessary operations
 */
public class RealOptimizedScraper {
    private static String token;

    // OPTIMIZATION 1: Increase browser pool (you have CPU for it)
    private static final int MAX_CONCURRENT_BROWSERS = Runtime.getRuntime().availableProcessors(); // More aggressive
    private static final Semaphore SEMAPHORE = new Semaphore(MAX_CONCURRENT_BROWSERS);
    private static final BlockingQueue<WebDriver> DRIVER_POOL = new ArrayBlockingQueue<>(MAX_CONCURRENT_BROWSERS);
    private static final Random RANDOM = new Random();

    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.7204.101 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.7204.101 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0"
    );

    // Metrics
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger skipCount = new AtomicInteger(0);
    private static final AtomicInteger failCount = new AtomicInteger(0);

    public static void main(String[] args) {
        int start = 1;
        int end = 300;
        String url = "https://metruyencv.biz/truyen/ban-ton-cau-den-vo-dich-vo-han-phan-than-che-ba-tien-gioi/chuong-";
        String folderPath = "novel/real-optimized/ban-ton-cau-den-vo-dich-vo-han-phan-than-che-ba-tien-gioi";

        String loginUrl = "https://backend.metruyencv.com/api/auth/login";
        String email = ""; // Replace with your email
        String password = ""; // Replace with your password

        System.out.println("=== REAL OPTIMIZED VERSION ===");
        System.out.println("Max concurrent browsers: " + MAX_CONCURRENT_BROWSERS);
        System.out.println();

        try {
            token = loginAndGetToken(loginUrl, email, password);
            System.out.println("✅ Login successful");
        } catch (IOException e) {
            System.err.println("❌ Login failed: " + e.getMessage());
            return;
        }

        // Initialize WebDriver pool
        long poolStart = System.nanoTime();
        initializeDriverPool();
        System.out.println("✅ Driver pool initialized in " + (System.nanoTime() - poolStart) / 1_000_000.0 + " ms");
        System.out.println();

        long startTime = System.nanoTime();

        // OPTIMIZATION 2: Use virtual threads for task submission (lightweight)
        // Platform threads still needed for Selenium, but task management is virtual
        try (ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_BROWSERS * 2)) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = start; i <= end; i++) {
                final int chapter = i;
                futures.add(executor.submit(() -> downloadAndSaveChapter(chapter, url, folderPath, loginUrl, email, password)));
            }

            executor.shutdown();
            if (!executor.awaitTermination(2, TimeUnit.HOURS)) {
                System.err.println("⚠️  Tasks did not complete within timeout");
            }

            // Don't wait for individual futures - let them complete naturally
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS); // Shorter timeout per chapter
                } catch (TimeoutException e) {
                    future.cancel(true);
                } catch (ExecutionException e) {
                    // Silent - already logged
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Executor interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            cleanupDriverPool();
        }

        System.out.println();
        System.out.println("All chapters processed. Merging files...");
        long mergeStart = System.nanoTime();
        mergeFiles(folderPath, "A-merged-novel.txt");
        long mergeTime = System.nanoTime() - mergeStart;

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;

        // Print report
        printReport(totalTime, mergeTime);
    }

    private static void printReport(long totalTime, long mergeTime) {
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("PERFORMANCE REPORT - REAL OPTIMIZED VERSION");
        System.out.println("=".repeat(70));

        double totalSeconds = totalTime / 1_000_000_000.0;
        long minutes = (long) (totalSeconds / 60);
        double seconds = totalSeconds % 60;

        System.out.printf("Total execution time:  %.3f seconds (%d min %.2f sec)%n", totalSeconds, minutes, seconds);
        System.out.printf("Merge time:            %.3f seconds%n", mergeTime / 1_000_000_000.0);
        System.out.printf("Chapters successful:   %d%n", successCount.get());
        System.out.printf("Chapters skipped:      %d%n", skipCount.get());
        System.out.printf("Chapters failed:       %d%n", failCount.get());
        System.out.printf("Concurrent browsers:   %d%n", MAX_CONCURRENT_BROWSERS);
        System.out.println("=".repeat(70));
    }

    private static void initializeDriverPool() {
        WebDriverManager.chromedriver().setup();

        // OPTIMIZATION 3: Parallel driver initialization (faster startup)
        List<Thread> initThreads = new ArrayList<>();

        for (int i = 0; i < MAX_CONCURRENT_BROWSERS; i++) {
            Thread t = new Thread(() -> {
                ChromeOptions options = new ChromeOptions();

                // OPTIMIZATION 4: Maximum performance flags
                options.addArguments(
                        "--headless=new",
                        "--disable-gpu",
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-extensions",
                        "--disable-images",
                        "--blink-settings=imagesEnabled=false",
                        "--disable-javascript-harmony-shipping", // Reduce JS overhead
                        "--disable-setuid-sandbox",
                        "--no-first-run",
                        "--no-default-browser-check",
                        "--disable-background-networking",
                        "--disable-background-timer-throttling",
                        "--disable-backgrounding-occluded-windows",
                        "--disable-breakpad",
                        "--disable-component-extensions-with-background-pages",
                        "--disable-features=TranslateUI,BlinkGenPropertyTrees",
                        "--disable-ipc-flooding-protection",
                        "--disable-renderer-backgrounding",
                        "--enable-features=NetworkService,NetworkServiceInProcess",
                        "--force-color-profile=srgb",
                        "--hide-scrollbars",
                        "--metrics-recording-only",
                        "--mute-audio"
                );

                options.addArguments("user-agent=" + getRandomUserAgent());
                options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation", "enable-logging"));

//                // OPTIMIZATION 5: Set page load strategy to 'eager' - don't wait for all resources
//                options.setPageLoadStrategy(org.openqa.selenium.PageLoadStrategy.EAGER);

                WebDriver driver = new ChromeDriver(options);

                // OPTIMIZATION 6: Set aggressive timeouts
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(8)); // Reduced from 10
                driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(5));
                driver.manage().timeouts().implicitlyWait(Duration.ofMillis(100)); // Very short

                DRIVER_POOL.add(driver);
            });
            initThreads.add(t);
            t.start();
        }

        // Wait for all drivers to initialize
        for (Thread t : initThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void cleanupDriverPool() {
        for (WebDriver driver : DRIVER_POOL) {
            try {
                driver.quit();
            } catch (Exception e) {
                // Silent cleanup
            }
        }
        DRIVER_POOL.clear();
    }

    private static WebDriver getDriverFromPool() throws InterruptedException {
        SEMAPHORE.acquire();
        return DRIVER_POOL.poll(5, TimeUnit.SECONDS); // Reduced timeout
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

    private static void downloadAndSaveChapter(int chapter, String url, String folderPath, String loginUrl, String email, String password) {
        String fileName = "chapter-" + chapter + ".txt";
        if (fileExists(fileName, folderPath)) {
            skipCount.incrementAndGet();
            return; // Silent skip
        }

        WebDriver driver = null;
        int maxAttempts = 3;
        int attempts = 0;

        try {
            driver = getDriverFromPool();
            if (driver == null) {
                System.err.println("❌ No WebDriver for chapter " + chapter);
                failCount.incrementAndGet();
                return;
            }

            // OPTIMIZATION 7: Only set cookie once per driver (check first)
            if (driver.manage().getCookieNamed("accessToken") == null) {
                driver.get("https://metruyencv.biz");
                Cookie authCookie = new Cookie("accessToken", token, ".metruyencv.biz", "/", null, false, true);
                driver.manage().addCookie(authCookie);
            }

            while (attempts < maxAttempts) {
                try {
                    long chapterStart = System.nanoTime();

                    // OPTIMIZATION 8: Don't change user agent per request (causes overhead)
                    // User agent is already set in ChromeOptions

                    driver.get(url + chapter);

                    // OPTIMIZATION 9: Shorter wait, check more frequently
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8), Duration.ofMillis(200));
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.id("chapter-content")));

                    String html = driver.getPageSource();

                    // Quick checks first (fail fast)
                    if (html.contains("cf-captcha") || html.contains("g-recaptcha")) {
                        attempts++;
                        Thread.sleep(3000 + RANDOM.nextInt(2000)); // Reduced wait
                        continue;
                    }

                    if (html.contains("Vui lòng xác thực email tài khoản của bạn")) {
                        attempts++;
                        Thread.sleep(500);
                        continue;
                    }

                    if (html.contains("Unauthorized") || html.contains("Token expired")) {
                        synchronized (RealOptimizedScraper.class) {
                            // Only one thread should refresh token
                            token = loginAndGetToken(loginUrl, email, password);
                        }
                        driver.manage().deleteCookieNamed("accessToken");
                        driver.get("https://metruyencv.biz");
                        Cookie authCookie = new Cookie("accessToken", token, ".metruyencv.biz", "/", null, false, true);
                        driver.manage().addCookie(authCookie);
                        attempts++;
                        continue;
                    }

                    // OPTIMIZATION 10: Parse only once
                    Document doc = Jsoup.parse(html);
                    Elements chapterContent = doc.select("#chapter-content");

                    // Remove unwanted elements in one go
                    chapterContent.select("div[id^=middle-content], canvas").remove();

                    String content = chapterContent.html()
                            .replaceAll("(?i)<br\\s*/?>\\s*(<br\\s*/?>\\s*)+", "<br>")
                            .replace("<br>", "\n");

                    if (content.isEmpty()) {
                        attempts++;
                        Thread.sleep(500);
                        continue;
                    }

                    // Write file (already fast, keep as is)
                    saveFile(content, fileName, folderPath);

                    long elapsed = (System.nanoTime() - chapterStart) / 1_000_000;
                    successCount.incrementAndGet();

                    // OPTIMIZATION 11: Less verbose logging
                    if (chapter % 100 == 0) {
                        System.out.printf("✓ Chapter %d (%.0f ms) [%d/%d/%d]%n",
                                chapter, (double) elapsed, successCount.get(), skipCount.get(), failCount.get());
                    }

                    return;

                } catch (Exception e) {
                    attempts++;
                    if (attempts == maxAttempts) {
                        System.err.println("❌ Failed chapter " + chapter + ": " + e.getMessage());
                        failCount.incrementAndGet();
                        return;
                    }
                    // OPTIMIZATION 12: Shorter retry backoff
                    Thread.sleep(500L * attempts + RANDOM.nextInt(500));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failCount.incrementAndGet();
        } finally {
            returnDriverToPool(driver);
        }
    }

    public static void saveFile(String content, String fileName, String folderPath) {
        Path file = Path.of(folderPath, fileName);
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(content);
            }
        } catch (IOException e) {
            System.err.println("❌ Write error " + fileName + ": " + e.getMessage());
        }
    }

    public static void mergeFiles(String folderPath, String mergedFileName) {
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
                System.out.println("⚠️  No chapter files found");
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

            System.out.println("✅ Merged into: " + mergedFile.toAbsolutePath());

        } catch (IOException e) {
            System.err.printf("❌ Merge error: %s%n", e.getMessage());
        }
    }

    public static String loginAndGetToken(String loginUrl, String email, String password) throws IOException {
        Connection.Response loginResponse = Jsoup.connect(loginUrl)
                .header("Content-Type", "application/json")
                .requestBody("{\"email\": \"" + email + "\", \"password\": \"" + password + "\", \"remember\": 1, \"device_name\": \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.7204.101 Safari/537.36\"}")
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .timeout(10000) // OPTIMIZATION 13: Set timeout on login too
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
            throw new IOException("Token not found in response");
        }
    }
}