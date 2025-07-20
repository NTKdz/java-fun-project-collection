package desktop.app.jnatest;

import java.net.http.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TranslateHelper {
    public static String translateText(String text, String targetLang) throws Exception {
        System.out.println("Translating text: " + text + " to language: " + targetLang);

        // Normalize text: replace newlines with spaces to avoid %0A
        String normalizedText = text.replaceAll("\\r?\\n", " ").trim();
        System.out.println("Normalized text: " + normalizedText);

        // Encode text, replacing '+' with '%20' for Lingva API compatibility
        String encodedText = URLEncoder.encode(normalizedText, StandardCharsets.UTF_8).replace("+", "%20");
        System.out.println("Encoded text: " + encodedText);

        String apiUrl = "https://lingva.ml/api/v1/auto/" + targetLang + "/" + encodedText;
        System.out.println("API URL: " + apiUrl);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("API response status: " + response.statusCode());
        System.out.println("API response body: " + response.body());

        // Parse JSON response (Lingva returns { "translation": "translated text" })
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String translated = json.get("translation").getAsString();
        return translated;
    }
}