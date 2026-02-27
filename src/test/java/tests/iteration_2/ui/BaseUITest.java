package tests.iteration_2.ui;

import com.codeborne.selenide.Configuration;
import common.annotations.DeleteCreatedEntity;
import common.extensions.CreateUserAndAccountExtensions;
import common.extensions.OpenAsUserExtensions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

@ExtendWith({CreateUserAndAccountExtensions.class, OpenAsUserExtensions.class, CreateUserAndAccountExtensions.class})
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
