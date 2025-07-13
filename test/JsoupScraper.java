import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JsoupScraper {

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new ScrapeHandler());
        server.setExecutor(null);
        System.out.println("Server started at http://localhost:" + port);
        server.start();
    }

    static class ScrapeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = scrapeData();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, html.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes());
            os.close();
        }
    }

    public static String scrapeData() {
        WebDriver driver = null;
        StringBuilder response = new StringBuilder();
        try {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("user-agent=Mozilla/5.0");

            driver = new ChromeDriver(options);
            driver.get("https://www.imdb.com/calendar/");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid=country-selector]")));

            Document doc = Jsoup.parse(driver.getPageSource());
            Elements movieContainer = doc.select("[data-testid=calendar-section]");

            response.append("<html><head><meta charset='UTF-8'><title>IMDB Releases</title>");
            response.append("<style>body { font-family: sans-serif; line-height: 1.6; } h2 { color: darkred; } h3 { color: navy; } hr { margin: 20px 0; }</style>");
            response.append("</head><body>\n");

            for (Element group : movieContainer) {
                String releaseDate = group.selectFirst("h3.ipc-title__text").text();
                String remainingTime = getDateDifferences(releaseDate);

                response.append("<h2>📅 Release Date: ").append(releaseDate).append("</h2>\n");
                response.append("<p><strong>").append(remainingTime).append("</strong></p>\n");

                Elements movieEntries = group.select("[data-testid=coming-soon-entry]");
                for (Element movie : movieEntries) {
                    String title = movie.selectFirst("a.ipc-metadata-list-summary-item__t").text();
                    String link = movie.selectFirst("a.ipc-metadata-list-summary-item__t").attr("href");
                    response.append(movie.select("img").outerHtml());
                    response.append("<h3>🎬 ").append(title).append("</h3>\n");
                    response.append("<p><a href=\"https://www.imdb.com").append(link)
                            .append("\" target=\"_blank\">🔗 IMDb Link</a></p>\n");

                    Elements extraInfos = movie.select("ul");
                    for (int i = 0; i < extraInfos.size(); i++) {
                        Elements spans = extraInfos.get(i).select("span");
                        List<String> infoTextList = new ArrayList<>();
                        for (Element span : spans) {
                            infoTextList.add(span.text());
                        }
                        if (!infoTextList.isEmpty()) {
                            response.append("<p><strong>")
                                    .append(i == 0 ? "Genres: " : "Casts: ")
                                    .append("</strong>").append(String.join(", ", infoTextList)).append("</p>\n");
                        }
                    }
                }

                response.append("<hr>\n");
            }

            response.append("</body></html>");
        } catch (Exception e) {
            response.append("<h2>Error:</h2><pre>").append(e.getMessage()).append("</pre>");
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
        return response.toString();
    }

    public static String getDateDifferences(String date2) {
        StringBuilder result = new StringBuilder();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
            LocalDate targetDate = LocalDate.parse(date2, formatter);
            LocalDate today = LocalDate.now();
            Period period = Period.between(today, targetDate);
            result.append(targetDate.getDayOfWeek()).append(" - Time remaining: ")
                    .append(period.getYears()).append(" years, ")
                    .append(period.getMonths()).append(" months, ")
                    .append(period.getDays()).append(" days");
        } catch (Exception e) {
            result.append("Date parse error");
        }
        return result.toString();
    }
}
