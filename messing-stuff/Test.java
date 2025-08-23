import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;
import java.util.NoSuchElementException;

public class Test {
    public static void main(String[] args) throws InterruptedException {
        WebDriverManager.chromedriver().setup();

        // Random User-Agent
        List<String> userAgents = Arrays.asList(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Safari/605.1.15",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36"
        );
        String randomUserAgent = userAgents.get(new Random().nextInt(userAgents.size()));

        // ChromeOptions setup
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--user-agent=" + randomUserAgent);
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);

        options.setExperimentalOption("prefs", prefs);
        // Launch driver
        WebDriver driver = new ChromeDriver(options);
        driver.get("https://www.facebook.com/");
        Cookie cUser = new Cookie("c_user", "100027574952215");
        Cookie xs = new Cookie("xs", "12%3ACxBBt8cm0-VFFg%3A2%3A1753586260%3A-1%3A-1%3A%3AAcVe1M0udbE9jRHhEksMb0xOHjdIzJFVJueC_Dl7Vw");
        driver.manage().addCookie(cUser);
        driver.manage().addCookie(xs);

        driver.navigate().to("https://www.facebook.com/messages/e2ee/t/8197893953621304/");

        // Stealth: Remove navigator.webdriver
        ((JavascriptExecutor) driver).executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
        );

        // Print page title
        System.out.println("Title of the page is: " + driver.getTitle());

        // Check for login success
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            WebElement closeOneTIme = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@aria-label='Close' and @role='button']")
            ));
            closeOneTIme.click();

            WebElement person = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@role='navigation']//div[.//img]//span[contains(text(),'Sinh viên dốt của trg U')]")
            ));
            person.click();

            wait.until( ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[@role='textbox']"))
            ).sendKeys("this is automated message", Keys.ENTER);
        } catch (NoSuchElementException e) {
            System.out.println("Login failed or profile link not found.");
        }

        // driver.quit();
    }
}
