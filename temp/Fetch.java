import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Fetch {
    public static void main(String[] args) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://metruyencv.info/truyen/ta-tong-mon-qua-khong-chiu-thua-kem-co-the-tu-dong-thang-cap/chuong-246"))
                .build();

        // Send asynchronous request to fetch the page content
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("HTTP Status Code: " + response.statusCode());
                    return response.body(); // Get the HTML response as a String
                })
                .thenApply(html -> {
                    // Extract content using regular expression
                    String content = extractContent(html);
                    if (content != null) {
                        saveToFile(content); // Save to a file
                    } else {
                        System.out.println("Content not found.");
                    }
                    return null;
                })
                .join(); // Wait for the async operation to complete

        System.out.println("Done");
        client.close();
    }

    // Extract content based on the data-x-bind="ChapterContent" attribute
    private static String extractContent(String html) {
        // Regular expression to match the content inside data-x-bind="ChapterContent"
        String regex = "data-x-bind=\"ChapterContent\"[^>]*>(.*?)</div>";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL); // DOTALL flag to match multiline content
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1).trim(); // Extract the content and remove extra whitespace
        }
        return null;
    }

    // Save the extracted content to a text file
    private static void saveToFile(String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
            writer.write(content);
            System.out.println("Content saved to output.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
