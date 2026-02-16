package tests.iteration_2.ui;

import com.codeborne.selenide.Configuration;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;

public abstract class BaseUITest {

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = configs.Config.getProperty("uiRemote");
        Configuration.baseUrl = configs.Config.getProperty("uiBaseUrl");
        Configuration.browser = configs.Config.getProperty("browser");
        Configuration.browserSize = configs.Config.getProperty("browserSize");

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true));
    }
}
