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
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class OptimizedScraper {
    private static String token;
    private static final int MAX_CONCURRENT_BROWSERS = Runtime.getRuntime().availableProcessors() - 1; // Reduced for stability
    private static final Semaphore SEMAPHORE = new Semaphore(MAX_CONCURRENT_BROWSERS);
    private static final BlockingQueue<WebDriver> DRIVER_POOL = new ArrayBlockingQueue<>(MAX_CONCURRENT_BROWSERS);
    private static final Random RANDOM = new Random();
    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.7204.101 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.7204.101 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0"
    );

    public static void main(String[] args) {
        int start = 1;
        int end = 707;
        String url = "https://metruyencv.biz/truyen/bat-dau-cam-xuong-nhan-vat-chinh-muoi-muoi-ban-thuong-chi-ton-cot/chuong-";
        String folderPath = "novel/nv-chinh-muoi-muoi/";

        // Login
        String loginUrl = "https://backend.metruyencv.com/api/auth/login";
        String email = "-"; // Replace with your email
        String password = "-"; // Replace with your password
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
        try (ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_BROWSERS * 2)) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = start; i <= end; i++) {
                final int chapter = i;
                futures.add(executor.submit(() -> downloadAndSaveChapter(chapter, url, folderPath, loginUrl, email, password)));
                System.out.println("Submitted task for chapter " + chapter);
            }

            executor.shutdown();
            if (!executor.awaitTermination(2, TimeUnit.HOURS)) {
                System.err.println("Tasks did not complete within the timeout.");
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    System.err.println("Error during task execution: " + e.getCause());
                } catch (InterruptedException e) {
                    System.err.println("Task interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Executor was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            cleanupDriverPool();
        }

        System.out.println("All chapters downloaded. Merging files...");
        mergeFiles(folderPath, "A-merged-novel.txt");
        System.out.println("All tasks completed.");

        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;
        double elapsedMillis = elapsedTime / 1_000_000.0;
        long totalMillis = elapsedTime / 1_000_000;
        long minutes = totalMillis / (60 * 1000);
        long seconds = (totalMillis / 1000) % 60;
        long milliseconds = totalMillis % 1000;

        System.out.printf("Total execution time: %.3f ms%n", elapsedMillis);
        System.out.printf("Total execution time: %d min %d sec %d ms%n", minutes, seconds, milliseconds);
    }

    private static void initializeDriverPool() {
        WebDriverManager.chromedriver().browserVersion("138").setup();
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
        return file.exists() && file.length() > 0; // Check if file exists and is not empty
    }

    private static void downloadAndSaveChapter(int chapter, String url, String folderPath, String loginUrl, String email, String password) {
        String fileName = "chapter-" + chapter + ".txt";
        if (fileExists(fileName, folderPath)) {
            System.out.println("Skipping chapter " + chapter + ": File already exists.");
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

            // Set cookie if not present
            if (driver.manage().getCookieNamed("accessToken") == null) {
                driver.get("https://metruyencv.biz");
                Cookie authCookie = new Cookie("accessToken", token, ".metruyencv.biz", "/", null, false, true);
                driver.manage().addCookie(authCookie);
            }

            long chapterStart = System.nanoTime();
            while (attempts < maxAttempts) {
                try {
                    ((ChromeDriver) driver).executeScript("navigator.userAgent = '" + getRandomUserAgent() + "';");
                    driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
                    driver.get(url + chapter);
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10), Duration.ofMillis(500));
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.id("chapter-content")));

                    String html = driver.getPageSource();
                    Document doc = Jsoup.parse(html);

                    // Check for CAPTCHA
                    if (html.contains("cf-captcha") || html.contains("g-recaptcha")) {
                        System.err.println("CAPTCHA detected for chapter " + chapter + ". Retrying after delay...");
                        attempts++;
                        Thread.sleep(5000 + RANDOM.nextInt(1000));
                        continue;
                    }

                    // Check for verification message
                    String verificationMessage = "Vui lòng xác thực email tài khoản của bạn";
                    if (html.contains(verificationMessage)) {
                        System.out.println("Verification message detected for chapter " + chapter);
                        attempts++;
                        Thread.sleep(1000);
                        continue;
                    }

                    // Check for token expiry
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
                    String content = chapterContent.text();

                    if (content.isEmpty()) {
                        System.err.println("Content is empty for chapter " + chapter + ". Check authentication or page structure.");
                        attempts++;
                        Thread.sleep(1000);
                        continue;
                    } else {
                        saveFile(content, fileName, folderPath);
                        System.out.println("Downloaded and saved: chapter-" + chapter);
                    }
                    System.out.printf("Chapter %d took %.3f ms%n", chapter, (System.nanoTime() - chapterStart) / 1_000_000.0);
                    return;
                } catch (Exception e) {
                    attempts++;
                    System.err.println("Attempt " + attempts + " failed for chapter " + chapter + ": " + e.getMessage());
                    if (attempts == maxAttempts) {
                        System.err.println("Skipping chapter " + chapter + " after " + maxAttempts + " failed attempts.");
                        return;
                    }
                    Thread.sleep(1000 * (1 << attempts) + RANDOM.nextInt(100));
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted during chapter " + chapter + ": " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            returnDriverToPool(driver);
        }
    }

    private static void saveFile(String content, String fileName, String folderPath) {
        File file = new File(folderPath + "/" + fileName);
        try {
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content);
            }
        } catch (IOException e) {
            System.err.println("Error saving file " + fileName + ": " + e.getMessage());
        }
    }

    private static void mergeFiles(String folderPath, String mergedFileName) {
        File folder = new File(folderPath);
        File mergedFile = new File(folderPath + "/" + mergedFileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mergedFile))) {
            File[] files = folder.listFiles((dir, name) -> name.startsWith("chapter-") && name.endsWith(".txt"));
            if (files != null) {
                Arrays.sort(files, Comparator.comparingInt(file -> Integer.parseInt(file.getName().replaceAll("\\D", ""))));
                for (File file : files) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                }
                System.out.println("All chapters merged into: " + mergedFile.getAbsolutePath());
            } else {
                System.out.println("No chapter files found in the folder.");
            }
        } catch (IOException e) {
            System.err.println("Error merging files: " + e.getMessage());
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