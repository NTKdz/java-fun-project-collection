import java.net.http.HttpResponse;

public class NovelScrapeResultHandler implements ScrapeResultHandler {

    @Override
    public void handle(HttpResponse<String> response) {

        if (response.statusCode() != 200) {
            System.err.println("Request failed with status: " + response.statusCode());
            return;
        }

        String body = response.body();

        // Process HTML here
        System.out.println("Fetched content length: " + body.length());
    }
}
