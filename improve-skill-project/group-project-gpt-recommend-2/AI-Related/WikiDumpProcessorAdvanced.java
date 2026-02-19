import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Ultra-optimized Wikipedia dump processor with:
 * - Pre-compiled regex patterns
 * - Bounded thread pool with back-pressure
 * - Efficient buffer management
 * - Directory creation caching
 * - Batch processing hints
 * - Skip redirects early
 * - Better progress reporting
 */
public class WikiDumpProcessorAdvanced {

    private static final String DUMP_FILE = "D:\\dataset\\enwiki-latest-pages-articles.xml.bz2";
    private static final String OUTPUT_DIR = "D:\\dataset\\txt";
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int QUEUE_SIZE = 5000;
    private static final int MIN_ARTICLE_LENGTH = 500;

    private static final AtomicInteger totalProcessed = new AtomicInteger(0);
    private static final AtomicInteger skippedCount = new AtomicInteger(0);
    private static final AtomicInteger redirectCount = new AtomicInteger(0);
    private static final AtomicLong bytesProcessed = new AtomicLong(0);

    // Cache for directory existence to avoid repeated filesystem checks
    private static final ConcurrentHashMap<String, Boolean> directoryCache = new ConcurrentHashMap<>();

    private static final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private static final ExecutorService executor = new ThreadPoolExecutor(
            THREAD_COUNT,
            THREAD_COUNT,
            0L,
            TimeUnit.MILLISECONDS,
            workQueue,
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // Pre-compiled regex patterns
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{.*?\\}\\}", Pattern.DOTALL);
    private static final Pattern FILE_PATTERN = Pattern.compile("\\[\\[File:.*?\\]\\]", Pattern.DOTALL);
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("\\[\\[Category:.*?\\]\\]", Pattern.DOTALL);
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[\\[(?:[^|]*\\|)?([^\\]]*)\\]\\]");
    private static final Pattern HEADER_PATTERN = Pattern.compile("==+ (.*?) ==+");
    private static final Pattern REF_PATTERN = Pattern.compile("(?s)<ref.*?>.*?</ref>");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final Pattern NON_ALPHA = Pattern.compile("[^a-zA-Z]");

    // Common namespace prefixes to skip
    private static final Set<String> SKIP_NAMESPACES = new HashSet<>(Arrays.asList(
            "Wikipedia:", "Talk:", "User:", "User talk:", "File:", "Template:",
            "Category:", "Portal:", "Draft:", "Help:", "Module:", "MediaWiki:"
    ));

    public static void main(String[] args) {
        System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "0");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");

        // Print system info
        System.out.println("=== Wikipedia Dump Processor (Advanced) ===");
        System.out.println("CPU Cores: " + THREAD_COUNT);
        System.out.println("Thread Pool Size: " + THREAD_COUNT);
        System.out.println("Queue Capacity: " + QUEUE_SIZE);
        System.out.println("Input: " + DUMP_FILE);
        System.out.println("Output: " + OUTPUT_DIR);
        System.out.println("==========================================\n");

        long startTime = System.currentTimeMillis();
        try {
            processParallel();
        } catch (Exception e) {
            System.err.println("Main parser error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
            try {
                System.out.println("\nWaiting for threads to complete...");
                if (!executor.awaitTermination(2, TimeUnit.HOURS)) {
                    System.err.println("Timeout reached, forcing shutdown...");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        long endTime = System.currentTimeMillis();
        long totalSeconds = (endTime - startTime) / 1000;

        System.out.println("\n==========================================");
        System.out.println("FINISHED!");
        System.out.println("==========================================");
        System.out.println("Total Time: " + formatDuration(totalSeconds));
        System.out.println("Articles Processed: " + String.format("%,d", totalProcessed.get()));
        System.out.println("Articles Skipped (existing): " + String.format("%,d", skippedCount.get()));
        System.out.println("Redirects Skipped: " + String.format("%,d", redirectCount.get()));

        if (totalSeconds > 0) {
            System.out.println("Processing Rate: " + String.format("%,d", totalProcessed.get() / totalSeconds) + " articles/sec");
        }
    }

    private static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    public static void processParallel() throws Exception {
        Files.createDirectories(Paths.get(OUTPUT_DIR));

        // Larger buffer for decompression
        BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(
                new BufferedInputStream(new FileInputStream(DUMP_FILE), 16 * 1024 * 1024)); // 16MB buffer

        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_COALESCING, false);
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XMLStreamReader reader = factory.createXMLStreamReader(bzIn, "UTF-8");

        System.out.println("Starting extraction...\n");

        long lastReportTime = System.currentTimeMillis();
        long startTime = lastReportTime;

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "page".equals(reader.getLocalName())) {
                String title = null;
                String redirect = null;
                StringBuilder rawText = new StringBuilder(16384);

                // Parse page
                while (reader.hasNext()) {
                    int pEvent = reader.next();
                    if (pEvent == XMLStreamConstants.START_ELEMENT) {
                        String name = reader.getLocalName();
                        if ("title".equals(name)) {
                            title = reader.getElementText();
                        } else if ("redirect".equals(name)) {
                            redirect = reader.getAttributeValue(null, "title");
                        } else if ("text".equals(name)) {
                            while (reader.hasNext()) {
                                int t = reader.next();
                                if (t == XMLStreamConstants.CHARACTERS || t == XMLStreamConstants.CDATA) {
                                    rawText.append(reader.getText());
                                } else if (t == XMLStreamConstants.END_ELEMENT) {
                                    break;
                                }
                            }
                        }
                    } else if (pEvent == XMLStreamConstants.END_ELEMENT && "page".equals(reader.getLocalName())) {
                        break;
                    }
                }

                // Early filtering
                if (title == null || rawText.length() == 0) continue;

                // Skip redirects
                if (redirect != null) {
                    redirectCount.incrementAndGet();
                    continue;
                }

                // Skip namespace pages
                if (shouldSkipNamespace(title)) continue;

                final String finalTitle = title;
                final String finalRaw = rawText.toString();

                // Quick redirect check in content
                String trimmed = finalRaw.trim();
                if (trimmed.length() < 100 && trimmed.toLowerCase().startsWith("#redirect")) {
                    redirectCount.incrementAndGet();
                    continue;
                }

                // Check if file exists
                if (fileExists(finalTitle)) {
                    int s = skippedCount.incrementAndGet();
                    if (s % 10000 == 0) {
                        System.out.println("Skipped " + String.format("%,d", s) + " existing articles...");
                    }
                    continue;
                }

                // Submit for processing
                executor.submit(() -> processArticle(finalTitle, finalRaw));

                // Progress reporting
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastReportTime > 15000) { // Every 15 seconds
                    reportProgress(startTime, currentTime);
                    lastReportTime = currentTime;
                }
            }
        }

        reader.close();
        bzIn.close();

        System.out.println("\nAll pages submitted. Waiting for processing to complete...");
    }

    private static void reportProgress(long startTime, long currentTime) {
        int processed = totalProcessed.get();
        int skipped = skippedCount.get();
        int redirects = redirectCount.get();
        long elapsed = (currentTime - startTime) / 1000;

        System.out.println(String.format(
                "[%s] Processed: %,d | Skipped: %,d | Redirects: %,d | Queue: %d | Rate: %d/sec",
                formatDuration(elapsed), processed, skipped, redirects,
                workQueue.size(),
                elapsed > 0 ? processed / elapsed : 0
        ));
    }

    private static boolean shouldSkipNamespace(String title) {
        for (String namespace : SKIP_NAMESPACES) {
            if (title.startsWith(namespace)) {
                return true;
            }
        }
        return false;
    }

    private static void processArticle(String title, String rawText) {
        try {
            String clean = simpleWikiClean(rawText);
            if (clean.length() > MIN_ARTICLE_LENGTH) {
                saveToFile(title, clean);
                int count = totalProcessed.incrementAndGet();
                if (count % 5000 == 0) {
                    System.out.println("✓ Newly Processed: " + String.format("%,d", count));
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing '" + title + "': " + e.getMessage());
        }
    }

    private static boolean fileExists(String title) {
        String folderName = getFolderName(title);
        String fileName = sanitizeFileName(title) + ".txt";
        return Files.exists(Paths.get(OUTPUT_DIR, folderName, fileName));
    }

    private static void saveToFile(String title, String content) throws IOException {
        String folderName = getFolderName(title);
        Path dir = Paths.get(OUTPUT_DIR, folderName);

        // Use cache to avoid repeated directory existence checks
        if (!directoryCache.getOrDefault(folderName, false)) {
            synchronized (directoryCache) {
                if (!directoryCache.getOrDefault(folderName, false)) {
                    if (!Files.exists(dir)) {
                        Files.createDirectories(dir);
                    }
                    directoryCache.put(folderName, true);
                }
            }
        }

        String fileName = sanitizeFileName(title) + ".txt";
        Path filePath = dir.resolve(fileName);

        Files.writeString(filePath, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String getFolderName(String title) {
        if (title.length() > 2) {
            return NON_ALPHA.matcher(title.substring(0, 2)).replaceAll("x");
        }
        return "misc";
    }

    private static String sanitizeFileName(String title) {
        return INVALID_FILENAME_CHARS.matcher(title).replaceAll("_");
    }

    private static String simpleWikiClean(String wikiText) {
        String cleaned = COMMENT_PATTERN.matcher(wikiText).replaceAll("");
        cleaned = TEMPLATE_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = FILE_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = CATEGORY_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = REF_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = LINK_PATTERN.matcher(cleaned).replaceAll("$1");
        cleaned = HEADER_PATTERN.matcher(cleaned).replaceAll("$1");

        // Remove excessive whitespace
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        return cleaned.trim();
    }
}