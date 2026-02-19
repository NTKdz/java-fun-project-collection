import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Weather {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new java.net.InetSocketAddress(8080), 0);

        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/data", new DataHandler());
        server.createContext("/api/sorting", new SortingHandler());
        server.setExecutor(null);
        server.start();
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File("public/index.html");
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());

            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class DataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Document doc = Jsoup.connect("https://www.accuweather.com/en/vn/hanoi/353412/weather-forecast/353412").get();

            List<Map<String,String>> response = new ArrayList<>();

            Elements dailyList = doc.select(".daily-list-body .daily-list-item");

            dailyList.forEach(item -> {
                Map<String,String> map = new HashMap<>();

                Element dateInfo = item.selectFirst(".date");
                map.put("dayOfTheWeek", dateInfo.select("p").get(0).text());
                map.put("day", dateInfo.select("p").get(1).text());

                Element icon = item.selectFirst(".icon");
                map.put("icon", icon.attr("src"));

                Element temp = item.selectFirst(".temp-phrase-wrapper .temp");
                map.put("tempHi", temp.selectFirst(".temp-hi").text());
                map.put("tempLo", temp.selectFirst(".temp-lo").text());

                Element phrase = item.selectFirst(".phrase .no-wrap");
                map.put("weatherStatus", phrase.text());

                Element precip = item.selectFirst(".precip");
                map.put("humid", precip.text());

                response.add(map);
            });

            Gson gson = new Gson();
            String json = gson.toJson(response);
            System.out.println(response);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            byte[] bytes = json.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class SortingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

        }
    }
}
