import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.management.OperatingSystemMXBean;

public class HttpServerTest {

    private static final AtomicInteger requestCount = new AtomicInteger(0);
    private static final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create();
        server.bind(new InetSocketAddress(8080), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                int current = requestCount.incrementAndGet();
                logSystemStats(current, exchange.getRequestURI().toString());

                String response = "Hello, World!";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();

                requestCount.decrementAndGet(); // done processing
            }
        });

        server.setExecutor(Executors.newFixedThreadPool(10)); // allow concurrent requests
        server.start();
        System.out.println("Server started on http://localhost:8080/");
    }

    private static void logSystemStats(int currentRequests, String path) {
        double cpuLoad = osBean.getSystemCpuLoad(); // 0.0 to 1.0
        long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);

        System.out.printf("[Request #%d] Path: %s%n", currentRequests, path);
        System.out.printf("Active Requests: %d | CPU Load: %.2f%% | Memory: %dMB/%dMB%n",
                currentRequests,
                cpuLoad * 100,
                totalMemory - freeMemory,
                totalMemory);
    }
}
