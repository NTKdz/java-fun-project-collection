package pv;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

public class PV {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("hello world");
        Integer a = 128;
        Integer b = 128;
        System.out.print(a==b);
//        test();
//        HttpClient client = HttpClient.newHttpClient();
//        String place_id = "hanoi";
//        String sections = "current,hourly,daily,all";
//        String language = "en";
//        String unit = "metric";
//        String key = "ys4uc5ewb0csjoak025tde5n4ieo5ohfv64n38zl";
//        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://www.meteosource.com/api/v1/free/point?"
//                + "place_id=" + place_id +
//                "&sections=" + sections + "&language=" + language + "&units=" + unit + "&key=" + key)).build();
//
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//        System.out.println(response.statusCode());
//        try (BufferedReader reader = new BufferedReader(new StringReader(response.body()))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                String[] section = line.split(",");
//                try (BufferedWriter writer = new BufferedWriter(new FileWriter("./pv/test.txt", true))) {
//                    for (String s : section) {
//                        writer.write(s);
//                        writer.newLine();
//                    }
//                }
//            }
//        }
//        client.close();
    }

    public static void test() {
        try (BufferedReader reader = new BufferedReader(new FileReader("pv/test.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int[] nums = Arrays.stream(line.split(","))
                        .mapToInt(Integer::parseInt)
                        .toArray();

                int max = -9999999;
                for (int i = 1; i <= nums.length; i++) {
                    int total = 0;
                    int k = 0;
                    for (int j = 0; j < nums.length; j++) {
                        total += nums[j];
                        if (k < i)
                            k++;
                        else {
                            total -= nums[j - i];
                        }
                        if (total > max)
                            max = total;
                    }
                }
                System.out.println(max);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
