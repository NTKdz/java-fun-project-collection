import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitedLoad {
    public static void main(String[] args) throws Exception {
        final int durationSeconds = 60;
        final int requestsPerSecond = 30; // 16 RPS

        final int totalRequests = durationSeconds * requestsPerSecond; // 960 total

        // Interval between requests in microseconds (may be fractional -> use double for calc)
        double secondsPerRequest = (double) durationSeconds / (double) totalRequests; // 0.0006 s
        long periodMicros = (long) Math.round(secondsPerRequest * 1_000_000.0); // 600 microseconds

        System.out.println("Requests: " + totalRequests + ", Duration(s): " + durationSeconds +
                ", Period(µs): " + periodMicros);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                // Provide an executor for async tasks; tune pool size as needed
                .executor(Executors.newFixedThreadPool(500))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://khoidev.uk/"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        // We will collect futures to wait for completion
        List<CompletableFuture<Void>> futures = new ArrayList<>(totalRequests);
        AtomicInteger sentCount = new AtomicInteger(0);
        CountDownLatch doneLatch = new CountDownLatch(1);

        long startMillis = System.currentTimeMillis();

        // Scheduled task: send one async request each tick
        Runnable tick = () -> {
            int idx = sentCount.getAndIncrement();
            if (idx >= totalRequests) {
                // reached target -> stop scheduling
                return;
            }

            CompletableFuture<Void> fut = client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        // optional: minimal logging (avoid heavy I/O here)
                        // System.out.println("Req " + (idx + 1) + " -> " + response.statusCode());
                    })
                    .exceptionally(e -> {
                        // System.err.println("Req " + (idx + 1) + " error: " + e.getMessage());
                        return null;
                    })
                    .thenRun(() -> {
                        // If last request finished and we've sent all, count down
                        if (sentCount.get() >= totalRequests && futures.size() >= totalRequests) {
                            // nothing here — final wait handled below
                        }
                    });

            // keep reference so we can wait later
            synchronized (futures) {
                futures.add(fut);
                // if we've scheduled all requests, stop the scheduler
                if (futures.size() >= totalRequests) {
                    // shutdown scheduler after current tick
                    scheduler.shutdown();
                    doneLatch.countDown();
                }
            }
        };

        // scheduleAtFixedRate accepts TimeUnit up to MICROSECONDS in Java 9+
        // initialDelay 0, period = periodMicros, unit = MICROSECONDS
        scheduler.scheduleAtFixedRate(tick, 0, periodMicros, TimeUnit.MICROSECONDS);

        // Wait until we've queued all requests (scheduler shutdown triggers doneLatch)
        doneLatch.await();

        // Wait for scheduler to terminate (it was asked to shutdown)
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
            scheduler.shutdownNow();
        }

        System.out.println("All requests scheduled. Waiting for completions...");

        // Wait for all request futures to complete
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        all.join();

        long endMillis = System.currentTimeMillis();
        double elapsedSec = (endMillis - startMillis) / 1000.0;
        System.out.println("Done. Elapsed: " + elapsedSec + "s. Sent: " + futures.size());
        // shutdown client's executor if it was custom
        Executor executor = client.executor().orElse(null);
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }
}
