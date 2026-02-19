import javax.swing.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {
    public static void main(String[] args) throws InterruptedException {
        try {
            Pattern pattern = Pattern.compile("w3schools", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher("Visit W3Schools!");
            boolean matchFound = matcher.find();
            if (matchFound) {
                System.out.println("Match found");
            } else {
                System.out.println("Match not found");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
