package tests.iteration_2.ui;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.sleep;

public class DepositUITests {

    @BeforeAll
    public static void setupSelenoid(){
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.0.13:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true));
    }

    @DisplayName("Логинимся") @Test
    void LoginTest (){
        Selenide.open("/login");
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys("admin");
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys("admin");

        $("button").click();

        sleep(50000);

        $("h1").getText().equals("Admin Panel");
    }

}
