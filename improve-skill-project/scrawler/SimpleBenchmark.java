import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Simple Benchmark Runner
 *
 * This runs both scrapers sequentially and compares their performance.
 * Make sure both CurrentScraper.java and OptimizedVirtualThreadScraper.java
 * are compiled before running this.
 */
public class SimpleBenchmark {

    static class Result {
        String version;
        long startTime;
        long endTime;
        long totalMs;
        boolean success;
        String error;

        double getSeconds() {
            return totalMs / 1000.0;
        }

        String getFormattedTime() {
            long seconds = totalMs / 1000;
            long minutes = seconds / 60;
            long secs = seconds % 60;
            long ms = totalMs % 1000;
            return String.format("%dm %ds %dms", minutes, secs, ms);
        }
    }

    public static void main(String[] args) {
        printHeader();

        // Get configuration
        Scanner scanner = new Scanner(System.in);

        System.out.println("Which test do you want to run?");
        System.out.println("  1) Current version only");
        System.out.println("  2) Optimized version only");
        System.out.println("  3) Both versions (comparison)");
        System.out.print("\nEnter choice [1-3]: ");

        String choice = scanner.nextLine().trim();

        Result currentResult = null;
        Result optimizedResult = null;

        System.out.println();
        System.out.println("Note: Both scrapers must be compiled and use the credentials");
        System.out.println("      hardcoded in their source files.");
        System.out.println();

        try {
            if ("1".equals(choice) || "3".equals(choice)) {
                currentResult = runVersion("CurrentScraper", "CURRENT (Baseline)");
                Thread.sleep(2000); // Brief pause between runs
            }

            if ("2".equals(choice) || "3".equals(choice)) {
                optimizedResult = runVersion("OptimizedVirtualThreadScraper", "OPTIMIZED (Virtual Threads)");
            }

            // Display comparison
            if (currentResult != null && optimizedResult != null) {
                displayComparison(currentResult, optimizedResult);
            } else if (currentResult != null) {
                displaySingleResult(currentResult);
            } else if (optimizedResult != null) {
                displaySingleResult(optimizedResult);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        scanner.close();
    }

    private static Result runVersion(String className, String displayName) {
        Result result = new Result();
        result.version = displayName;

        System.out.println();
        System.out.println("═".repeat(70));
        System.out.println("  RUNNING: " + displayName);
        System.out.println("═".repeat(70));
        System.out.println();

        try {
            // Load the class
            Class<?> scraperClass = Class.forName(className);

            // Find the main method
            Method mainMethod = scraperClass.getMethod("main", String[].class);

            // Capture start time
            result.startTime = System.currentTimeMillis();
            System.out.println("⏱️  Started at: " + new Date(result.startTime));
            System.out.println();

            // Invoke main method
            String[] mainArgs = new String[0];
            mainMethod.invoke(null, (Object) mainArgs);

            // Capture end time
            result.endTime = System.currentTimeMillis();
            result.totalMs = result.endTime - result.startTime;
            result.success = true;

            System.out.println();
            System.out.println("⏱️  Finished at: " + new Date(result.endTime));
            System.out.println("✅ Completed in: " + result.getFormattedTime() + " (" + result.getSeconds() + "s)");

        } catch (ClassNotFoundException e) {
            result.success = false;
            result.error = "Class not found: " + className + ". Please compile it first.";
            System.err.println("❌ " + result.error);
        } catch (Exception e) {
            result.success = false;
            result.error = e.getMessage();
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private static void displaySingleResult(Result result) {
        System.out.println();
        System.out.println("╔" + "═".repeat(68) + "╗");
        System.out.println("║" + centerText("BENCHMARK RESULT", 68) + "║");
        System.out.println("╚" + "═".repeat(68) + "╝");
        System.out.println();

        if (result.success) {
            System.out.printf("Version:      %s%n", result.version);
            System.out.printf("Total Time:   %s (%.3f seconds)%n", result.getFormattedTime(), result.getSeconds());
            System.out.printf("Started:      %s%n", new Date(result.startTime));
            System.out.printf("Finished:     %s%n", new Date(result.endTime));
        } else {
            System.out.println("Status:       FAILED");
            System.out.println("Error:        " + result.error);
        }

        System.out.println();
    }

    private static void displayComparison(Result current, Result optimized) {
        System.out.println();
        System.out.println();
        System.out.println("╔" + "═".repeat(68) + "╗");
        System.out.println("║" + centerText("PERFORMANCE COMPARISON", 68) + "║");
        System.out.println("╚" + "═".repeat(68) + "╝");
        System.out.println();

        if (!current.success || !optimized.success) {
            System.out.println("⚠️  Cannot compare: One or both versions failed to run");
            if (!current.success) {
                System.out.println("   Current: " + current.error);
            }
            if (!optimized.success) {
                System.out.println("   Optimized: " + optimized.error);
            }
            return;
        }

        // Calculate differences
        long timeSavedMs = current.totalMs - optimized.totalMs;
        double percentChange = (timeSavedMs / (double)current.totalMs) * 100.0;

        // Display table
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│ Metric                    │  Current     │  Optimized   │ Diff  │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");

        System.out.printf("│ Total Time                │ %9.2fs  │ %9.2fs  │%6.1f%% │%n",
                current.getSeconds(), optimized.getSeconds(), percentChange);

        System.out.printf("│ Formatted Time            │ %12s │ %12s │       │%n",
                current.getFormattedTime(), optimized.getFormattedTime());

        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println();

        // Summary box
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                           SUMMARY                               │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");

        if (timeSavedMs > 0) {
            System.out.printf("│ ✅ Optimized version is FASTER by %.1f%%                        │%n", percentChange);
            System.out.printf("│    Time saved: %.2f seconds (%s)                │%n",
                    timeSavedMs / 1000.0, formatMs(timeSavedMs));
        } else if (timeSavedMs < 0) {
            System.out.printf("│ ⚠️  Optimized version is SLOWER by %.1f%%                       │%n", -percentChange);
            System.out.printf("│    Time lost: %.2f seconds (%s)                 │%n",
                    -timeSavedMs / 1000.0, formatMs(-timeSavedMs));
        } else {
            System.out.println("│ ⚖️  Both versions performed identically                          │");
        }

        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println();

        // Visual comparison
        displayVisualBars(current, optimized);

        // Verdict
        displayVerdict(percentChange);
    }

    private static void displayVisualBars(Result current, Result optimized) {
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                      VISUAL COMPARISON                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println();

        int maxBarLength = 50;
        long maxTime = Math.max(current.totalMs, optimized.totalMs);

        printBar("Current    ", current.totalMs, maxTime, maxBarLength, current.getSeconds());
        printBar("Optimized  ", optimized.totalMs, maxTime, maxBarLength, optimized.getSeconds());

        System.out.println();
    }

    private static void printBar(String label, long timeMs, long maxTime, int maxLength, double seconds) {
        int barLength = (int)((timeMs / (double)maxTime) * maxLength);
        barLength = Math.max(1, Math.min(barLength, maxLength));

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            bar.append("█");
        }

        System.out.printf("%s [%-50s] %.2fs%n", label, bar.toString(), seconds);
    }

    private static void displayVerdict(double percentChange) {
        System.out.println("╔" + "═".repeat(68) + "╗");
        System.out.println("║" + centerText("VERDICT", 68) + "║");
        System.out.println("╚" + "═".repeat(68) + "╝");
        System.out.println();

        if (percentChange > 25) {
            System.out.println("🏆 EXCELLENT! The optimization provides MAJOR improvements!");
            System.out.println();
            System.out.println("   Your analysis was spot-on. Decoupling I/O from Selenium");
            System.out.println("   significantly reduced the bottleneck. The virtual thread");
            System.out.println("   approach is working exceptionally well.");
            System.out.println();
            System.out.println("   💡 Your disk I/O was likely a major bottleneck.");

        } else if (percentChange > 15) {
            System.out.println("✅ GREAT! The optimization shows substantial improvement!");
            System.out.println();
            System.out.println("   The virtual thread approach is delivering solid gains.");
            System.out.println("   Selenium is no longer waiting for file writes to complete,");
            System.out.println("   which is clearly helping throughput.");
            System.out.println();
            System.out.println("   💡 Benefits scale with more chapters and slower disks.");

        } else if (percentChange > 10) {
            System.out.println("👍 GOOD! The optimization provides clear benefits.");
            System.out.println();
            System.out.println("   While not dramatic, the improvement is measurable and");
            System.out.println("   consistent. The decoupled I/O approach is working as");
            System.out.println("   expected.");
            System.out.println();
            System.out.println("   💡 You'd see bigger gains with slower storage or more files.");

        } else if (percentChange > 5) {
            System.out.println("😐 MODERATE: Small improvement observed.");
            System.out.println();
            System.out.println("   The optimization helps, but the gains are modest. This");
            System.out.println("   suggests your disk is fast (SSD?) or network fetch is");
            System.out.println("   the dominant bottleneck.");
            System.out.println();
            System.out.println("   💡 Still worth using for cleaner architecture!");

        } else if (percentChange > 0) {
            System.out.println("⚠️  MINIMAL: Very small improvement.");
            System.out.println();
            System.out.println("   The optimization barely helps. Your bottleneck is likely");
            System.out.println("   in network latency, not I/O. The virtual thread overhead");
            System.out.println("   nearly matches the I/O savings.");
            System.out.println();
            System.out.println("   💡 Network fetch time dominates in your case.");

        } else if (percentChange > -5) {
            System.out.println("⚖️  NEUTRAL: Roughly equivalent performance.");
            System.out.println();
            System.out.println("   Both versions perform similarly. This is expected when:");
            System.out.println("   - Using very fast SSDs (I/O is negligible)");
            System.out.println("   - Network fetch dominates total time");
            System.out.println("   - Small number of files");
            System.out.println();
            System.out.println("   💡 The optimized version still has architectural benefits.");

        } else {
            System.out.println("❌ SLOWER: Optimized version underperformed.");
            System.out.println();
            System.out.println("   This is unexpected but can happen when:");
            System.out.println("   - Virtual thread overhead exceeds I/O savings");
            System.out.println("   - Queue operations add latency");
            System.out.println("   - System is under heavy load");
            System.out.println();
            System.out.println("   💡 Try running again or with more chapters.");
        }

        System.out.println();

        // Additional insights
        System.out.println("KEY INSIGHTS:");
        System.out.println("─".repeat(70));
        System.out.println("• The optimized version decouples Selenium from file I/O");
        System.out.println("• WebDriver returns to pool immediately after fetching");
        System.out.println("• Virtual threads handle all file operations asynchronously");
        System.out.println("• Benefits increase with: slower disks, more files, more concurrency");
        System.out.println();
    }

    private static void printHeader() {
        System.out.println();
        System.out.println("╔" + "═".repeat(68) + "╗");
        System.out.println("║" + centerText("WEB SCRAPER BENCHMARK COMPARISON", 68) + "║");
        System.out.println("║" + centerText("Current vs Optimized (Virtual Threads)", 68) + "║");
        System.out.println("╚" + "═".repeat(68) + "╝");
        System.out.println();
    }

    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text + " ".repeat(width - padding - text.length());
    }

    private static String formatMs(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        long millis = ms % 1000;

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else if (secs > 0) {
            return String.format("%ds %dms", secs, millis);
        } else {
            return String.format("%dms", millis);
        }
    }
}