package authentication;

import com.mailslurp.clients.ApiException;
import com.mailslurp.models.Email;
import factories.TestUserFactory;
import infrastructure.AuthBypassService;
import infrastructure.MailslurpService;
import io.github.bonigarcia.wdm.WebDriverManager;
import models.UserStatus;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

public class AuthenticationTests {
    private WebDriver driver;

    @BeforeAll
    public static void setUpClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    }

    @Test
    public void loginSuccessfully_usingEmail() throws ApiException {
        driver.navigate().to("https://chesstv.local:3000/");

        var loginTab = driver.findElement(By.xpath("//a[text()='Login']"));
        loginTab.click();

        var emailInput = driver.findElement(By.id("usernameOrEmail"));
        emailInput.sendKeys("john@example.com");
        var passwordInput = driver.findElement(By.id("password"));
        passwordInput.sendKeys("password123");

        var rememberMeCheckbox = driver.findElement(By.id("rememberMe"));
        rememberMeCheckbox.click();

        byPassCaptcha();

        var loginButton = driver.findElement(By.xpath("//button[text()='Login']"));
        loginButton.click();

        var userName = driver.findElement(By.id("username"));

        Assertions.assertEquals("johnDoe", userName.getText());

        var logoutButton = driver.findElement(By.xpath("//a[text()='Logout']"));
        logoutButton.click();
    }

    @Test
    public void loginWithRememberMeAndVerifyOnNextVisit() throws ApiException {
        driver.navigate().to("https://chesstv.local:3000/");

        var loginTab = driver.findElement(By.xpath("//a[text()='Login']"));
        loginTab.click();

        var emailInput = driver.findElement(By.id("usernameOrEmail"));
        emailInput.sendKeys("john@example.com");
        var passwordInput = driver.findElement(By.id("password"));
        passwordInput.sendKeys("password123");

        var rememberMeCheckbox = driver.findElement(By.id("rememberMe"));
        rememberMeCheckbox.click();

        byPassCaptcha();

        var loginButton = driver.findElement(By.xpath("//button[text()='Login']"));
        loginButton.click();

        var userName = driver.findElement(By.id("username"));

        Assertions.assertEquals("johnDoe", userName.getText());

        var originalCookies = driver.manage().getCookies();

        driver.quit();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        driver = new ChromeDriver(options);

        driver.navigate().to("https://chesstv.local:3000/");
        for (var cookie : originalCookies) {
            driver.manage().addCookie(cookie);
        }

        driver.navigate().to("https://chesstv.local:3000/profile");

        userName = driver.findElement(By.id("username"));

        Assertions.assertEquals("johnDoe", userName.getText());

        var logoutButton = driver.findElement(By.xpath("//a[text()='Logout']"));
        logoutButton.click();
    }

    @Test
    public void profileUpdatedSuccessfully_when_newUserUpdatesProfile() throws ApiException {
        var testUser = TestUserFactory.createDefault();
        driver.navigate().to("https://chesstv.local:3000/");

        var loginTab = driver.findElement(By.xpath("//a[text()='Login']"));
        loginTab.click();

        var emailInput = driver.findElement(By.id("usernameOrEmail"));
        emailInput.sendKeys(testUser.getUsername());
        var passwordInput = driver.findElement(By.id("password"));
        passwordInput.sendKeys(testUser.getPassword());

        var rememberMeCheckbox = driver.findElement(By.id("rememberMe"));
        rememberMeCheckbox.click();

        byPassCaptcha();

        var loginButton = driver.findElement(By.xpath("//button[text()='Login']"));
        loginButton.click();

        var userName = driver.findElement(By.id("username"));

        Assertions.assertEquals(testUser.getUsername(), userName.getText());

        // edit profile:
        var userNameEditInput = driver.findElement(By.id("editUsername"));
        var emailEditInput = driver.findElement(By.id("editEmail"));
        var editPhoneNumber = driver.findElement(By.id("editPhoneNumber"));
        var editPassword = driver.findElement(By.id("editPassword"));


        // create update cases for each separate field.
        // Input a username of 4 characters (minimum limit) - Expected Result: No validation error.
        userNameEditInput.clear();
        userNameEditInput.sendKeys("newUserName");

        var updateProfileButton = driver.findElement(By.xpath("//button[text()='Update Profile']"));
        updateProfileButton.click();

        var logoutButton = driver.findElement(By.xpath("//a[text()='Logout']"));
        logoutButton.click();

        emailInput = driver.findElement(By.id("usernameOrEmail"));
        emailInput.sendKeys("newUserName");
        passwordInput = driver.findElement(By.id("password"));
        passwordInput.sendKeys(testUser.getPassword());

        rememberMeCheckbox = driver.findElement(By.id("rememberMe"));
        rememberMeCheckbox.click();

        byPassCaptcha();

        loginButton = driver.findElement(By.xpath("//button[text()='Login']"));
        loginButton.click();

        userNameEditInput = driver.findElement(By.id("editUsername"));
        Assertions.assertEquals("newUserName", userNameEditInput.getAttribute("value"));
    }

    @Test
    public void passwordSuccessfullyRest_whenRequestReset() throws ApiException {
        var testUser = TestUserFactory.createDefaultWithRealEmail(UserStatus.PENDING);
        driver.navigate().to("https://chesstv.local:3000/");

        var activateTab = driver.findElement(By.xpath("//a[text()='Request Reset']"));
        activateTab.click();

        var resetEmailInput = driver.findElement(By.id("resetEmail"));
        resetEmailInput.sendKeys(testUser.getEmail());

        var requestPasswordResetButton = driver.findElement(By.xpath("//button[text()='Request Password Reset']"));
        requestPasswordResetButton.click();

        Email receivedEmail = MailslurpService.waitForLatestEmail(testUser.getUserInbox(), OffsetDateTime.now().minusSeconds(30));
        var activationUrl = extractActivationUrl(receivedEmail.getBody());
        driver.navigate().to(activationUrl);

        var resetPasswordTab = driver.findElement(By.id("reset-tab"));
        resetPasswordTab.click();

        var newPasswordInput = driver.findElement(By.id("newPassword"));
        newPasswordInput.sendKeys("password123");

        var resetPasswordButton = driver.findElement(By.xpath("//button[text()='Reset Password']"));
        resetPasswordButton.click();

        // login with the new password
        var loginTab = driver.findElement(By.xpath("//a[text()='Login']"));
        loginTab.click();

        var emailInput = driver.findElement(By.id("usernameOrEmail"));
        emailInput.sendKeys(testUser.getUsername());
        var passwordInput = driver.findElement(By.id("password"));
        passwordInput.sendKeys("password123");

        var rememberMeCheckbox = driver.findElement(By.id("rememberMe"));
        rememberMeCheckbox.click();

        byPassCaptcha();

        var loginButton = driver.findElement(By.xpath("//button[text()='Login']"));
        loginButton.click();

        var userName = driver.findElement(By.id("username"));

        Assertions.assertEquals(testUser.getUsername(), userName.getText());

        var logoutButton = driver.findElement(By.xpath("//a[text()='Logout']"));
        logoutButton.click();
    }

    @Test
    public void fasterLoginWithCookie() throws ApiException {
        var testUser = TestUserFactory.createDefaultWithRealEmail(UserStatus.ACTIVE);
        driver.navigate().to("https://chesstv.local:3000/");

        var authCookieValue = AuthBypassService.generateAuthCookie(testUser.getUsername(), testUser.getPassword(), String.valueOf(testUser.getId()));
        driver.manage().addCookie(new Cookie("auth", authCookieValue));
        driver.manage().addCookie(new Cookie("userId", String.valueOf(testUser.getId())));

        driver.navigate().to("https://chesstv.local:3000/profile");

        var userName = driver.findElement(By.id("username"));

        Assertions.assertEquals(testUser.getUsername(), userName.getText());

        var logoutButton = driver.findElement(By.xpath("//a[text()='Logout']"));
        logoutButton.click();
    }

    @Test
    public void accountSuccessfullyActivated_when_fillAllRequiredRegistrationFields() throws ApiException {
           /*
        Valid Registration with Correct Input
        Mismatched Password and Confirm Password
        Registration with Existing Username
        Registration with Existing Email
        Username Below Minimum Length
        Username Above Maximum Length
        Username with Special Characters
        Invalid Email Format
        Password Below Minimum Length
        Password Above Maximum Length
        Password Without Uppercase Letter
        Password Without Lowercase Letter
        Password Without Numeric Character
        Phone Number Below Minimum Length
        Phone Number Above Maximum Length
        Phone Number with Alphabets or Special Characters
        Registration without Username
        Registration without Email
        Registration without Phone
         */
        var testUser = TestUserFactory.createTestUserDto();
        driver.navigate().to("https://chesstv.local:3000/");

        var registerTab = driver.findElement(By.xpath("//a[text()='Register']"));
        registerTab.click();
        var userNameInput = driver.findElement(By.id("registerUsername"));
        userNameInput.sendKeys(testUser.getUsername());
        var registerEmailInput = driver.findElement(By.id("registerEmail"));
        registerEmailInput.sendKeys(testUser.getEmail());
        var registerPhoneInput = driver.findElement(By.id("registerPhone"));
        registerPhoneInput.sendKeys(testUser.getPhone());
        var registerPasswordInput = driver.findElement(By.id("registerPassword"));
        registerPasswordInput.sendKeys(testUser.getPassword());
        var confirmPasswordInput = driver.findElement(By.id("confirmPassword"));
        confirmPasswordInput.sendKeys(testUser.getPassword());
        var registerButton = driver.findElement(By.xpath("//button[text()='Register']"));
        registerButton.click();

        var activateTab = driver.findElement(By.xpath("//a[text()='Activate']"));
        activateTab.click();

        var currentTime = OffsetDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        Email receivedEmail = MailslurpService.waitForLatestEmail(testUser.getUserInbox(), currentTime);
        var code = extractActivationCode(receivedEmail.getBody());
        var activationCodeInput = driver.findElement(By.id("activationCode"));
        activationCodeInput.sendKeys(code);

        var activateButton = driver.findElement(By.xpath("//button[text()='Activate']"));
        activateButton.click();

        // try to login

        var loginTab = driver.findElement(By.xpath("//a[text()='Login']"));
        loginTab.click();

        var emailInput = driver.findElement(By.id("usernameOrEmail"));
        emailInput.sendKeys(testUser.getUsername());
        var passwordInput = driver.findElement(By.id("password"));
        passwordInput.sendKeys(testUser.getPassword());

        var rememberMeCheckbox = driver.findElement(By.id("rememberMe"));
        rememberMeCheckbox.click();

        byPassCaptcha();

        var loginButton = driver.findElement(By.xpath("//button[text()='Login']"));
        loginButton.click();

        var userName = driver.findElement(By.id("username"));

        Assertions.assertEquals(testUser.getUsername(), userName.getText());

        var logoutButton = driver.findElement(By.xpath("//a[text()='Logout']"));
        logoutButton.click();
    }

    @Test
    public void loginSuccessfully_usingEmailAndBypass2FA() throws ApiException {
        driver.navigate().to("https://chesstv.local:3000/");

        var loginTab = driver.findElement(By.xpath("//a[text()='Login']"));
        loginTab.click();

        var testUser = TestUserFactory.createDefault2FAWithRealEmail();
        var emailInput = driver.findElement(By.id("usernameOrEmail"));
        emailInput.sendKeys(testUser.getEmail());
        var passwordInput = driver.findElement(By.id("password"));
        passwordInput.sendKeys(testUser.getPassword());

        var rememberMeCheckbox = driver.findElement(By.id("rememberMe"));
        rememberMeCheckbox.click();

        byPassCaptcha();

        var loginButton = driver.findElement(By.xpath("//button[text()='Login']"));
        loginButton.click();

        var twoFaCode = AuthBypassService.generate2FAToken(testUser.getId());
        var twoFACodeInput = driver.findElement(By.id("twoFaToken"));
        twoFACodeInput.sendKeys(twoFaCode);

        loginButton = driver.findElement(By.xpath("//button[text()='Login']"));
        loginButton.click();

        var userName = driver.findElement(By.id("username"));

        Assertions.assertEquals(testUser.getUsername(), userName.getText());

        var logoutButton = driver.findElement(By.xpath("//a[text()='Logout']"));
        logoutButton.click();
    }

    private void byPassCaptcha() {
        var captchaByPass = driver.findElement(By.xpath("//input[@name='captcha-bypass']"));
        JavascriptExecutor jsExecutor = (JavascriptExecutor)driver;
        jsExecutor.executeScript("arguments[0].setAttribute('value', arguments[1]);", captchaByPass, "10685832-cd90-4e91-9224-2ef69ce88f53");
    }

    public static String extractActivationCode(String message) {
        // The regex pattern looks for a sequence of alphanumeric characters after the "Your activation code is:" phrase.
        // Your activation code is: S0KF29
        Pattern pattern = Pattern.compile(".*\\bYour activation code is: ([a-zA-Z0-9]+)\\s*$");
        Matcher matcher = pattern.matcher(message);

        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String extractActivationUrl(String message) {
        // The regex pattern looks for a URL format.
        Pattern pattern = Pattern.compile("http[s]?://[^\\s\"]+");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            return matcher.group(0);  // Returns the first occurrence of the pattern
        }
        return null;
    }


    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
