import netscape.javascript.JSObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.Month;

public class Main {
    public static void main(String[] args) {
        LocalDate birthDate = LocalDate.of(2003, 3, 25);
        String zodiacSign = getZodiacSign(birthDate);
        System.out.println("Zodiac Sign: " + zodiacSign);

        try {
            String duration = "monthly";
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://horoscope-app-api.vercel.app/api/v1/get-horoscope/"
                            + duration + "?sign=" + zodiacSign))
                    .build();

            long startTime = System.nanoTime();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            long endTime = System.nanoTime();
            long durationMillis = (endTime - startTime) / 1_000_000;
            System.out.println("Time taken for request: " + durationMillis + " ms");

            System.out.println("Response Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());

            // Print out the specific fields you need
            String date = extractJsonValue(response.body(), "challenging_days");
            String horoscopeData = extractJsonValue(response.body(), "horoscope_data");

            System.out.println("Date: " + date);
            System.out.println("Horoscope Data: " + horoscopeData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getZodiacSign(LocalDate birthDate) {
        int day = birthDate.getDayOfMonth();
        Month month = birthDate.getMonth();

        return switch (month) {
            case MARCH -> (day >= 21) ? "Aries" : "Pisces";
            case APRIL -> (day <= 19) ? "Aries" : "Taurus";
            case MAY -> (day <= 20) ? "Taurus" : "Gemini";
            case JUNE -> (day <= 20) ? "Gemini" : "Cancer";
            case JULY -> (day <= 22) ? "Cancer" : "Leo";
            case AUGUST -> (day <= 22) ? "Leo" : "Virgo";
            case SEPTEMBER -> (day <= 22) ? "Virgo" : "Libra";
            case OCTOBER -> (day <= 22) ? "Libra" : "Scorpio";
            case NOVEMBER -> (day <= 21) ? "Scorpio" : "Sagittarius";
            case DECEMBER -> (day <= 21) ? "Sagittarius" : "Capricorn";
            case JANUARY -> (day <= 19) ? "Capricorn" : "Aquarius";
            case FEBRUARY -> (day <= 18) ? "Aquarius" : "Pisces";
            default -> throw new IllegalArgumentException("Invalid Month");
        };
    }

    public static String extractJsonValue(String json, String key) {
        String searchPattern = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchPattern) + searchPattern.length();
        int endIndex = json.indexOf("\"", startIndex);

        if (endIndex != -1) {
            return json.substring(startIndex, endIndex);
        } else {
            return null; // Key not found
        }
    }
}
