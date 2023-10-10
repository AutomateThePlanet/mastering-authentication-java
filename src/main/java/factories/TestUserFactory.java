package factories;

import com.github.javafaker.Faker;
import infrastructure.MailslurpService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import models.TestUser;
import models.UserStatus;

import static io.restassured.RestAssured.given;

public class TestUserFactory {

    private static final Faker faker = new Faker();

    public static TestUser createDefault() {
        return createDefault(UserStatus.ACTIVE);
    }

    public static TestUser createDefaultWithRealEmail() {
        return createDefaultWithRealEmail(UserStatus.ACTIVE);
    }

    public static TestUser createDefault2FAWithRealEmail() {
        String username = faker.name().username();
        String password = generatePassword();
        String phone = generatePhoneNumber();

        return createTestUser2FAWithRealEmail(username, password, phone, UserStatus.ACTIVE.toString());
    }

    public static TestUser createDefault(UserStatus status) {
        String username = faker.name().username();
        String email = faker.internet().emailAddress();
        String password = generatePassword();
        String phone = generatePhoneNumber();

        return createTestUser(username, email, password, phone, status.toString());
    }

    public static TestUser createTestUserDto() {
        var newInbox = MailslurpService.createInbox();
        String username = generateUsername();
        String email = newInbox.getEmailAddress();
        String password = generatePassword();
        String phone = generatePhoneNumber();
        TestUser user = new TestUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setPhone(phone);
        user.setUserInbox(newInbox);
        return user;
    }

    public static TestUser createDefaultWithRealEmail(UserStatus status) {
        String username = faker.name().username();
        String password = generatePassword();
        String phone = generatePhoneNumber();

        return createTestUserWithRealEmail(username, password, phone, status.toString());
    }

    private static String generateUsername() {
        String username = faker.name().firstName() + faker.number().digits(5);
        username = username.replaceAll("[^a-zA-Z0-9]", "");
        return username;
    }

    private static String generatePassword() {
        // Using Faker to generate a 10-character password.
        // If specific character types are required, this method can be adjusted accordingly.
        return faker.lorem().characters(10) + "Aa";
    }

    private static String generatePhoneNumber() {
        // Generating a 10-digit phone number.
        return faker.number().digits(10);
    }

    private static TestUser createTestUser(String username, String email, String password, String phone, String status) {
        TestUser user = new TestUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setPhone(phone);
        user.setStatus(status);
        RestAssured.useRelaxedHTTPSValidation();
        Response response = given()
                .baseUri("https://chesstv.local:3000/")
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post("/createTestUser")
                .then()
                .statusCode(200)
                .extract()
                .response();

        return response.as(TestUser.class);
    }

    private static TestUser createTestUserWithRealEmail(String username, String password, String phone, String status) {
        var newInbox = MailslurpService.createInbox();
        TestUser user = new TestUser();
        user.setUsername(username);
        user.setEmail(newInbox.getEmailAddress());

        user.setPassword(password);
        user.setPhone(phone);
        user.setStatus(status);
        RestAssured.useRelaxedHTTPSValidation();
        Response response = given()
                .baseUri("https://chesstv.local:3000/")
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post("/createTestUser")
                .then()
                .statusCode(200)
                .extract()
                .response();
        user = response.as(TestUser.class);
        user.setUserInbox(newInbox);
        return user;
    }

    private static TestUser createTestUser2FAWithRealEmail(String username, String password, String phone, String status) {
        var newInbox = MailslurpService.createInbox();
        TestUser user = new TestUser();
        user.setUsername(username);
        user.setEmail(newInbox.getEmailAddress());

        user.setPassword(password);
        user.setPhone(phone);
        user.setStatus(status);
        RestAssured.useRelaxedHTTPSValidation();
        Response response = given()
                .baseUri("https://chesstv.local:3000/")
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post("/createTestUser2FA")
                .then()
                .statusCode(200)
                .extract()
                .response();
        user = response.as(TestUser.class);
        user.setUserInbox(newInbox);
        return user;
    }
}