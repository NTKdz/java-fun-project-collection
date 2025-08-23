import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;

public class ScraperRedo {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8888), 0);

        server.createContext("/scrape", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "Scraping completed successfully!";
                byte[] respBytes = response.getBytes();

                exchange.sendResponseHeaders(200, respBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(respBytes);
                }
            }
        });

        server.setExecutor(null); // use default thread pool
        server.start();
        System.out.println("Server running at http://localhost:8888/scrape");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ssh", "-o", "StrictHostKeyChecking=no", "-R", "test:80:localhost:8888", "serveo.net");
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Stream logs in background
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[LOG] " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
