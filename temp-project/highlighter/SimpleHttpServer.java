import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class SimpleHttpServer {
    public static void main(String[] args) throws IOException {
        // Create a server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        // Define a handler for "/"
        server.createContext("/", new MyHandler());

        // Start the server
        server.setExecutor(null); // default executor
        server.start();

        System.out.println("Server started on http://localhost:8080/");
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hello, World! This is a simple Java HTTP server.";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
