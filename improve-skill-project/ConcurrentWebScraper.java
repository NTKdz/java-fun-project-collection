import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ConcurrentWebScraper {

    // Reusable, thread-safe client
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        List<String> siteLinks = List.of(
                "https://example.com",
                "https://example.org"
        );

        ScrapeType type = ScrapeType.CHINESE_NOVEL;
        ScrapeResultHandler handler = initScrapeResultHandler(type);

        // Create async requests
        List<CompletableFuture<Void>> futures = siteLinks.stream()
                .map(link -> scrapeAsync(link)
                        .thenAccept(handler::handle)
                        .exceptionally(ex -> {
                            System.err.println("Error scraping " + link + ": " + ex.getMessage());
                            return null;
                        })
                )
                .toList();

        // Wait for all to complete
        CompletableFuture<Void> all =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        all.join();  // Waits for all async tasks to finish
    }

    public static CompletableFuture<HttpResponse<String>> scrapeAsync(String link) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(link))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static ScrapeResultHandler initScrapeResultHandler(ScrapeType type) {

        Map<ScrapeType, ScrapeResultHandler> handlers = Map.of(
                ScrapeType.CHINESE_NOVEL, new NovelScrapeResultHandler()
        );

        return handlers.getOrDefault(type, new NovelScrapeResultHandler());
    }
}

