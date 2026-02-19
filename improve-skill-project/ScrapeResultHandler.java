import java.net.http.HttpResponse;

public interface ScrapeResultHandler {
    void handle(HttpResponse<String> response);
}
