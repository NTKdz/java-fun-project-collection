package desktop.app.jnatest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TranslateHelper {

    public static String translateText(String text, String targetLang) throws IOException, InterruptedException {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String apiUrl = "https://lingva.ml/api/v1/auto/" + targetLang + "/" + encodedText;

        System.out.println("API URL: " + apiUrl);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String json = response.body();
        System.out.println("Response JSON: " + json);

        // More robust translation extraction using pattern
        String marker = "\"translation\":\"";
        int start = json.indexOf(marker);
        if (start == -1) {
            throw new IOException("Translation field not found in response.");
        }
        start += marker.length();
        int end = json.indexOf("\"", start);
        while (end != -1 && json.charAt(end - 1) == '\\') { // handle escaped quotes
            end = json.indexOf("\"", end + 1);
        }

        String rawTranslation = json.substring(start, end);

        // Replace encoded characters like '+' and '\\n'

        return rawTranslation
                .replace("+", " ")
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }
}