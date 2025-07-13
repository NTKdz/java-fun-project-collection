//import java.io.FileInputStream;
//import java.io.InputStream;
//import java.net.CookieManager;
//import java.net.URI;
//import java.net.URLEncoder;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.nio.charset.StandardCharsets;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Properties;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
//public class LoginScript {
//
//    // A User-Agent header makes our script look like a real browser.
//    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36";
//
//    public static void main(String[] args) {
//        // --- Configuration ---
//        Properties prop = new Properties();
//        String username;
//        String password;
//
//        try (InputStream input = new FileInputStream("config.properties")) {
//            prop.load(input);
//            username = prop.getProperty("username");
//            password = prop.getProperty("password");
//        } catch (Exception ex) {
//            System.err.println("Error: Could not read config.properties file.");
//            System.out.println(ex.getMessage());
//            return;
//        }
//
//        String initialPageUrl = "https://www.pidantuan.com/forum.php";
//
//        // --- Step 1: Create an HttpClient with a Cookie Manager ---
//        // The CookieManager will automatically store and send cookies for us. This is crucial.
//        HttpClient client = HttpClient.newBuilder()
//                .cookieHandler(new CookieManager())
//                .followRedirects(HttpClient.Redirect.NORMAL) // Follow redirects automatically
//                .build();
//
//        try {
//            // --- Step 2: GET request to get the dynamic formhash ---
//            System.out.println("Step 1: Fetching login page to get formhash...");
//            HttpRequest initialRequest = HttpRequest.newBuilder()
//                    .uri(new URI(initialPageUrl))
//                    .header("User-Agent", USER_AGENT)
//                    .GET()
//                    .build();
//
//            HttpResponse<String> initialResponse = client.send(initialRequest, HttpResponse.BodyHandlers.ofString());
//            String pageHtml = initialResponse.body();
//
//            // Use regex to find the formhash value in the HTML response
//            // The formhash is usually in an <input type="hidden" name="formhash" value="..." /> tag
//            Pattern pattern = Pattern.compile("name=\"formhash\" value=\"([a-f0-9]{8})\"");
//            Matcher matcher = pattern.matcher(pageHtml);
//
//            String formhash;
//            if (matcher.find()) {
//                formhash = matcher.group(1);
//                System.out.println("Successfully found formhash: " + formhash);
//            } else {
//                System.err.println("Error: Could not find formhash on the page. The website structure might have changed.");
//                System.err.println("Page HTML: " + pageHtml.substring(0, Math.min(pageHtml.length(), 1000))); // Print first 1000 chars for debugging
//                return;
//            }
//
//            // The loginhash is part of the URL itself. It can often be found on the page too,
//            // but for this specific AJAX login, it's often generated on the fly. We'll use a common one.
//            // If this fails, it might need to be extracted dynamically as well.
//            String loginhash = "L4144"; // This is a common pattern, but might need adjusting.
//
//            // --- Step 3: Construct and send the POST login request ---
//            String loginUrl = "https://www.pidantuan.com/member.php?mod=logging&action=login&loginsubmit=yes&inajax=1&loginhash=" + loginhash;
//            String refererUrl = "https://www.pidantuan.com/forum.php"; // The page we are "coming from"
//
//            // Build the form data payload (as application/x-www-form-urlencoded)
//            Map<String, String> formData = new HashMap<>();
//            formData.put("formhash", formhash);
//            formData.put("referer", refererUrl);
//            formData.put("loginfield", "username");
//            formData.put("username", username);
//            formData.put("password", password);
//            formData.put("questionid", "0");
//            formData.put("answer", "");
//
//            // Helper to convert the map to a URL-encoded string
//            String formBody = formData.entrySet().stream()
//                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
//                    .collect(Collectors.joining("&"));
//
//            System.out.println("\nStep 2: Sending POST request to log in...");
//            System.out.println("URL: " + loginUrl);
//            System.out.println("Form Data: " + formBody);
//
//            HttpRequest loginRequest = HttpRequest.newBuilder()
//                    .uri(new URI(loginUrl))
//                    .header("User-Agent", USER_AGENT)
//                    .header("Content-Type", "application/x-www-form-urlencoded")
//                    .header("Referer", refererUrl)
//                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
//                    .build();
//
//            HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
//
//            // --- Step 4: Analyze the response ---
//            System.out.println("\n--- Login Response ---");
//            System.out.println("Status Code: " + loginResponse.statusCode());
//            System.out.println("Response Body: " + loginResponse.body());
//
//            // For this website (a Discuz! forum), a successful AJAX login response often contains the text 'succeed'.
//            if (loginResponse.body().contains("succeed") || loginResponse.body().contains(username)) {
//                System.out.println("\nResult: Login Successful!");
//            } else {
//                System.out.println("\nResult: Login Failed. Check credentials and response body for error messages.");
//            }
//
////            client.close();
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//    }
//}