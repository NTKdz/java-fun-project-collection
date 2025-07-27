import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CCLoader {
    private static final int DEFAULT_NUM_REQUESTS = 10;
    private static final int DEFAULT_CONCURRENCY = 1;

    private static AtomicInteger successCount = new AtomicInteger();
    private static AtomicInteger failureCount = new AtomicInteger();
    private static Map<Integer, AtomicInteger> statusCodeCounts = new ConcurrentHashMap<>();
    private static List<Long> requestTimes = Collections.synchronizedList(new ArrayList<>());
    private static List<Long> timeToFirstBytes = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws Exception {
        String url = null;
        String filePath = null;
        int numRequests = DEFAULT_NUM_REQUESTS;
        int concurrency = DEFAULT_CONCURRENCY;

        args = new String[] {
                "-f", "url.txt",
                "-n", "3000000",
                "-c", "20"
        };
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-u": url = args[++i]; break;
                case "-n": numRequests = Integer.parseInt(args[++i]); break;
                case "-c": concurrency = Integer.parseInt(args[++i]); break;
                case "-f": filePath = args[++i]; break;
            }
        }

        List<String> urls = new ArrayList<>();
        if (filePath != null) {
            urls = readUrlsFromFile(filePath);
        } else if (url != null) {
            for (int i = 0; i < numRequests; i++) urls.add(url);
        } else {
            System.out.println("Usage: -u <url> OR -f <file> [-n <requests>] [-c <concurrency>]");
            return;
        }

        // Repeat URLs if fewer than numRequests
        while (urls.size() < numRequests) {
            urls.addAll(urls);
        }
        urls = urls.subList(0, numRequests);
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        long startTime = System.nanoTime();
        for (String u : urls) {
            executor.submit(() -> sendRequest(u));
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
        long totalTime = System.nanoTime() - startTime;

        reportResults(totalTime);
    }

    private static List<String> readUrlsFromFile(String filePath) throws IOException {
        List<String> urls = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) urls.add(line.trim());
            }
        }
        return urls;
    }

    private static void sendRequest(String urlString) {
        try {
            long start = System.nanoTime();
            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            long firstByteTime = System.nanoTime();
            int code = conn.getResponseCode();
            InputStream in = conn.getInputStream();
            in.read(); // Simulate time to first byte
            long lastByteTime = System.nanoTime();
            while (in.read() != -1) {}
            in.close();

            long end = System.nanoTime();

            requestTimes.add((end - start) / 1_000_000);
            timeToFirstBytes.add((lastByteTime - start) / 1_000_000);

            successCount.incrementAndGet();
            statusCodeCounts.computeIfAbsent(code, k -> new AtomicInteger()).incrementAndGet();
        } catch (Exception e) {
            failureCount.incrementAndGet();
        }
    }

    private static void reportResults(long totalTimeNano) {
        System.out.println("\nResults:");
        System.out.printf(" Total Requests (2XX).......................: %d\n", successCount.get());
        System.out.printf(" Failed Requests............................: %d\n", failureCount.get());
        System.out.printf(" Request/second.............................: %.2f\n",
                successCount.get() / (totalTimeNano / 1_000_000_000.0));

        if (!requestTimes.isEmpty()) {
            reportTiming("Total Request Time (ms)", requestTimes);
            reportTiming("Time to First Byte (ms)", timeToFirstBytes);
        }

        for (Map.Entry<Integer, AtomicInteger> entry : statusCodeCounts.entrySet()) {
            System.out.printf(" Status %d..................................: %d\n",
                    entry.getKey(), entry.getValue().get());
        }
    }

    private static void reportTiming(String label, List<Long> times) {
        long min = Collections.min(times);
        long max = Collections.max(times);
        double avg = times.stream().mapToLong(l -> l).average().orElse(0);
        System.out.printf(" %s (Min, Max, Mean).....: %d, %d, %.2f\n", label, min, max, avg);
    }
}
