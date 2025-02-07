import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class DataCrawler {
    public static void deleteDirectoryRecursively(Path path) throws IOException {
        // Traverse the directory and delete all files/subdirectories
        try (var paths = Files.walk(path)) {

            paths.sorted(Comparator.reverseOrder()) // Sort in reverse order to delete inner files first
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (!file.delete()) { // Check if deletion was successful
                            System.err.println("Failed to delete file: " + file.getAbsolutePath());
                        }
                    });
        } catch (DirectoryNotEmptyException e) {
            System.err.println("Directory is not empty: " + path.toAbsolutePath());
        }
    }

    public static void main(String[] args) {
        int start = 1;
        int end = 896;
        String url = "https://metruyencv.com/truyen/bat-dau-uy-hiep-nu-chinh-cai-nay-phan-phai-ta-duong-dinh/chuong-";
        String folderPath = "novel/uy-hiep";
//        deleteDirectoryRecursively(Path.of(folderPath));
//        System.out.println("Directory and its contents deleted successfully.");

        long startTime = System.nanoTime();

        // Create a thread pool with a fixed number of threads
        try (ExecutorService executor = Executors.newFixedThreadPool(100)) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = start; i <= end; i++) {
                final int chapter = i;
                futures.add(executor.submit(() -> downloadAndSaveChapter(chapter, url, folderPath)));
                System.out.println("Submitted task for chapter " + chapter);
            }

            // Wait for all tasks to finish
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.err.println("Tasks did not complete within the timeout.");
            }

            // Check for task exceptions
            for (Future<?> future : futures) {
                try {
                    future.get(); // Ensure the task has completed successfully
                } catch (ExecutionException e) {
                    System.err.println("Error during task execution: " + e.getCause());
                } catch (InterruptedException e) {
                    System.err.println("Task interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restore the interrupted status
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Executor was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore the interrupted status
        }

        System.out.println("All chapters downloaded. Merging files...");
        mergeFiles(folderPath, "A-merged-novel.txt");
        System.out.println("All tasks completed.");
        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;

        double elapsedMillis = elapsedTime / 1_000_000.0;

        long totalMillis = elapsedTime / 1_000_000; // Convert to milliseconds
        long minutes = totalMillis / (60 * 1000);   // Convert to minutes
        long seconds = (totalMillis / 1000) % 60;   // Remaining seconds
        long milliseconds = totalMillis % 1000;    // Remaining milliseconds

        System.out.printf("Total execution time: %.3f ms%n", elapsedMillis); // Milliseconds with decimals
        System.out.printf("Total execution time: %d min %d sec %d ms%n", minutes, seconds, milliseconds); // Min:Sec:Ms
        System.out.printf("Total execution time: %02d:%02d.%03d%n", minutes, seconds, milliseconds); // Min:Sec.Ms format
    }

    private static void downloadAndSaveChapter(int chapter, String url, String folderPath) {
        File file = new File(folderPath + "/chapter-" + chapter + ".txt");
        if (file.exists()) {
            System.out.println("File already exists: chapter-" + chapter + ". Skipping...");
            return;
        }

        int attempts = 0;
        while (attempts < 10) {
            try {
                Document doc = Jsoup.connect(url + chapter)
                        .timeout(5000)
                        .get();
                Elements chapterContent = doc.select("[data-x-bind=ChapterContent]");

                chapterContent.select("div[id=middle-content-three]").remove();
                chapterContent.select("div[id=middle-content-two]").remove();
                chapterContent.select("div[id=middle-content-one]").remove();

                String content = chapterContent.html()
                        .replaceAll("(?i)<br\\s*/?>\\s*(<br\\s*/?>\\s*)+", "<br>")
                        .replace("<br>", "\n");

//                String content = chapterContent.text();
                saveFile(content, "chapter-" + chapter + ".txt", folderPath);
                System.out.println("Downloaded and saved: chapter-" + chapter);
                return; // Exit after successful download
            } catch (IOException e) {
                attempts++;
                System.out.println("Attempt " + attempts + " failed for chapter " + chapter + ": " + e.getMessage());
                if (attempts == 3) {
                    System.out.println("Skipping chapter " + chapter + " after 3 failed attempts.");
                }
            }
        }
    }

    private static void saveFile(String content, String fileName, String folderPath) {
        File file = new File(folderPath + "/" + fileName);
        try {
            File folder = new File(folderPath);
            if (!folder.exists()) {
                if (folder.mkdirs()) {
                    System.out.println("Created directory: " + folderPath);
                } else {
                    System.out.println("Created directory: ");
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content);
            }
        } catch (IOException e) {
            System.out.println("An error occurred while saving file: " + fileName);
            System.err.println(e.getMessage());
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
            System.out.println("An error occurred while merging files.");
            System.out.println(e.getMessage());
        }
    }
}
