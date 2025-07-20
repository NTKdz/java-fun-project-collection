import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleHttpServer {
    private static final Logger LOGGER = Logger.getLogger(SimpleHttpServer.class.getName());
    private static final int PORT = 8080;
    private static final int CONNECTION_BACKLOG = 0;
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    // Configure HttpClient with timeout and connection pooling
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .executor(Executors.newFixedThreadPool(THREAD_POOL_SIZE))
            .build();

    // API endpoints configuration
    private static final Map<String, String> API_ENDPOINTS = new HashMap<>();
    static {
        API_ENDPOINTS.put("/", "https://uselessfacts.jsph.pl/api/v2/facts/random");
        API_ENDPOINTS.put("/joke", "https://sv443.net/jokeapi/v2/joke/Any");
        API_ENDPOINTS.put("/joke/programming", "https://sv443.net/jokeapi/v2/joke/Programming");
        API_ENDPOINTS.put("/joke/search", "https://sv443.net/jokeapi/v2/joke/Any");
    }

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(PORT),
                    CONNECTION_BACKLOG
            );

            // Register static endpoint
            server.createContext("/api/test", new TestHandler());

            // Register dynamic API proxy endpoints
            for (String path : API_ENDPOINTS.keySet()) {
                server.createContext(path, new ApiProxyHandler(API_ENDPOINTS.get(path)));
            }

            // Use a thread pool for better performance
            server.setExecutor(Executors.newFixedThreadPool(THREAD_POOL_SIZE));
            server.start();

            LOGGER.info("Server started on port " + PORT);

            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Stopping server...");
                server.stop(3); // 3 second delay before forceful shutdown
                LOGGER.info("Server stopped");
            }));

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start server", e);
        }
    }

    // Handler for static content
    static class TestHandler implements HttpHandler {
        private static final String RESPONSE = "Hello, this is a test API!";
        private static final byte[] RESPONSE_BYTES = RESPONSE.getBytes(StandardCharsets.UTF_8);

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                sendResponse(exchange, 200, RESPONSE_BYTES);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error handling /api/test request", e);
                sendErrorResponse(exchange, 500);
            }
        }
    }

    // Generic handler for API proxying
    static class ApiProxyHandler implements HttpHandler {
        private final String targetUrl;

        public ApiProxyHandler(String targetUrl) {
            this.targetUrl = targetUrl;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                URI requestUri = exchange.getRequestURI();
                String query = requestUri.getQuery();
                System.out.println(query);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(targetUrl))
                        .timeout(Duration.ofSeconds(5))
                        .header("User-Agent", "SimpleHttpServer/1.0")
                        .GET()
                        .build();

                HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                sendResponse(exchange, response.statusCode(), response.body());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Request interrupted", e);
                sendErrorResponse(exchange, 503);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error proxying request to " + targetUrl, e);
                sendErrorResponse(exchange, 502);
            }
        }
    }

    // Helper methods for response handling
    private static void sendResponse(HttpExchange exchange, int statusCode, byte[] responseBody) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBody);
        }
    }

    private static void sendErrorResponse(HttpExchange exchange, int statusCode) throws IOException {
        String error = "{\"error\":\"" + getErrorMessage(statusCode) + "\"}";
        byte[] responseBytes = error.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private static String getErrorMessage(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "Unknown Error";
        };
    }
}