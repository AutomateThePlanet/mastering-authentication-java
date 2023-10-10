package infrastructure;

import com.mailslurp.apis.InboxControllerApi;
import com.mailslurp.apis.WaitForControllerApi;
import com.mailslurp.clients.ApiClient;
import com.mailslurp.clients.ApiException;
import com.mailslurp.clients.Configuration;
import com.mailslurp.models.Email;
import com.mailslurp.models.InboxDto;
import com.mailslurp.models.SendEmailOptions;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.SneakyThrows;
import models.emails.EmailsItem;
import models.emails.EmailsResponse;
import okhttp3.OkHttpClient;
import org.openqa.selenium.WebDriver;
import utilities.ResourcesReader;
import utilities.TimestampBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MailslurpService {
    private static ApiClient defaultClient;
    private static InboxControllerApi inboxControllerApi;
    private static String API_KEY = System.getenv("MAILSLURP_KEY");
    private static final Long TIMEOUT = 30000L;

    static {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setConnectTimeout(TIMEOUT.intValue());
        defaultClient.setWriteTimeout(TIMEOUT.intValue());
        defaultClient.setReadTimeout(TIMEOUT.intValue());
        defaultClient.setHttpClient(httpClient);
        defaultClient.setApiKey(API_KEY);
        inboxControllerApi = new InboxControllerApi(defaultClient);
    }

    @SneakyThrows
    public static InboxDto createInbox() {
        InboxDto inbox = inboxControllerApi.createInbox(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        return inbox;
    }

    @SneakyThrows
    public static Email waitForLatestEmail(InboxDto inbox, OffsetDateTime since) {
        var waitForControllerApi = new WaitForControllerApi(defaultClient);
        Email receivedEmail = waitForControllerApi
                .waitForLatestEmail(inbox.getId(), TIMEOUT, false, null, since, null, 10000L);

        return receivedEmail;
    }
}
