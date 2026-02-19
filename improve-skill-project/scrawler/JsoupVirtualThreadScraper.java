import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class JsoupVirtualThreadScraper {

    private static volatile String token;

    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.7204.101 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.7204.101 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.7204.101 Safari/537.36"
    );

    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws Exception {

        int start = 1;
        int end = 2;

        String baseUrl =
                "https://metruyencv.biz/truyen/ban-ton-cau-den-vo-dich-vo-han-phan-than-che-ba-tien-gioi/chuong-";
        String folderPath =
                "novel/ban-ton-cau-den-vo-dich-vo-han-phan-than-che-ba-tien-gioi1";

        String loginUrl = "https://backend.metruyencv.com/api/auth/login";
        String email = "";
        String password = "";

        token = loginAndGetToken(loginUrl, email, password);
        System.out.println("Logged in");

        long startTime = System.nanoTime();

        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            for (int i = start; i <= end; i++) {
                int chapter = i;
                executor.submit(() ->
                        downloadAndSaveChapter(
                                chapter,
                                baseUrl,
                                folderPath,
                                loginUrl,
                                email,
                                password
                        )
                );
            }
        }

        mergeFiles(folderPath, "A-merged-novel.txt");

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        System.out.printf("Completed in %d ms%n", elapsedMs);
    }

    // -------------------- CHAPTER DOWNLOAD --------------------

    private static void downloadAndSaveChapter(
            int chapter,
            String url,
            String folderPath,
            String loginUrl,
            String email,
            String password
    ) {

        String fileName = "chapter-" + chapter + ".txt";
        if (fileExists(fileName, folderPath)) {
            return;
        }

        int attempts = 0;
        int maxAttempts = 3;

        while (attempts < maxAttempts) {
            try {
                Document doc = Jsoup.connect(url + chapter)
                        .cookie("TOKEN", "6996023|oCdR9ulp0CGMmm0z0Obg7lIPfhH2tZGUDOFNYVOM")
                        .userAgent(randomUA())
                        .referrer("https://metruyencv.biz/")
                        .timeout(10_000)
                        .get();

                String html = doc.html();
                System.out.println(html);
                try(BufferedWriter writer = new BufferedWriter(new FileWriter("index.html"))) {
                    writer.write(html);
                }

                if (html.contains("cf-captcha") || html.contains("g-recaptcha")) {
                    throw new IOException("CAPTCHA");
                }

                if (html.contains("Unauthorized") || html.contains("Token expired")) {
                    token = loginAndGetToken(loginUrl, email, password);
                    attempts++;
                    continue;
                }

                Elements chapterContent = doc.select("#chapter-content");
                chapterContent.select("div[id^=middle-content]").remove();
                chapterContent.select("canvas").remove();

                String content = chapterContent.html()
                        .replaceAll("(?i)<br\\s*/?>\\s*(<br\\s*/?>\\s*)+", "<br>")
                        .replace("<br>", "\n")
                        .trim();

                System.out.println(content);
                if (content.isEmpty()) {
                    throw new IOException("Empty content");
                }

                saveFile(content, fileName, folderPath);
                System.out.println("Downloaded chapter " + chapter);
                return;

            } catch (Exception e) {
                attempts++;
                try {
                    Thread.sleep(1000L * attempts + RANDOM.nextInt(300));
                } catch (InterruptedException ignored) {
                }
            }
        }

        System.err.println("Failed chapter " + chapter);
    }

    // -------------------- LOGIN --------------------

    private static String loginAndGetToken(
            String loginUrl,
            String email,
            String password
    ) throws IOException {

        Connection.Response response = Jsoup.connect(loginUrl)
                .method(Connection.Method.POST)
                .header("Content-Type", "application/json")
                .requestBody("""
                        {
                          "email": "%s",
                          "password": "%s",
                          "remember": 1,
                          "device_name": "Chrome"
                        }
                        """.formatted(email, password))
                .ignoreContentType(true)
                .execute();

        String body = response.body();

        int idx = body.indexOf("\"accessToken\":\"");
        if (idx < 0) {
            idx = body.indexOf("\"token\":\"");
        }
        if (idx < 0) {
            throw new IOException("Token not found");
        }

        int start = body.indexOf('"', idx) + 1;
        int end = body.indexOf('"', start);
        return body.substring(start, end);
    }

    // -------------------- FILE UTILS --------------------

    private static boolean fileExists(String fileName, String folderPath) {
        File file = new File(folderPath, fileName);
        return file.exists() && file.length() > 0;
    }

    private static void saveFile(String content, String fileName, String folderPath)
            throws IOException {

        Files.createDirectories(Path.of(folderPath));

        Path file = Path.of(folderPath, fileName);
        try (BufferedWriter writer =
                     Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private static void mergeFiles(String folderPath, String output) {

        File folder = new File(folderPath);
        File[] files = folder.listFiles(
                f -> f.getName().startsWith("chapter-")
                        && f.getName().endsWith(".txt")
        );

        if (files == null || files.length == 0) return;

        Arrays.sort(files, Comparator.comparingInt(
                f -> Integer.parseInt(f.getName().replaceAll("\\D", ""))
        ));

        try (FileChannel out = FileChannel.open(
                Path.of(folderPath, output),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {

            for (File f : files) {
                out.write(ByteBuffer.wrap(
                        ("\n\n**CHAPTER " +
                                f.getName().replaceAll("\\D", "") +
                                "**\n\n").getBytes(StandardCharsets.UTF_8)
                ));
                try (FileChannel in =
                             FileChannel.open(f.toPath(), StandardOpenOption.READ)) {
                    in.transferTo(0, in.size(), out);
                }
            }

            System.out.println("Merged chapters");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String randomUA() {
        return USER_AGENTS.get(RANDOM.nextInt(USER_AGENTS.size()));
    }
}
