package pv;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PV {
    public static void main(String[] args) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String place_id = "hanoi";
        String sections = "current,hourly,daily,all";
        String language = "en";
        String unit = "metric";
        String key = "ys4uc5ewb0csjoak025tde5n4ieo5ohfv64n38zl";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://www.meteosource.com/api/v1/free/point?"
                + "place_id=" + place_id +
                "&sections=" + sections + "&language=" + language + "&units=" + unit + "&key=" + key)).build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.statusCode());
        try (BufferedReader reader = new BufferedReader(new StringReader(response.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] section = line.split(",");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("./pv/test.txt", true))) {
                    for (String s : section) {
                        writer.write(s);
                        writer.newLine();
                    }
                }
            }
        }
        client.close();
    }

    public void test() {
        System.out.println("Hello World!");
    }
}
