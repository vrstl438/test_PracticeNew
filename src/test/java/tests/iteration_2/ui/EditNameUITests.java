package tests.iteration_2.ui;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import context.ScenarioContext;
import domain.builders.CreateUserRequestBuilder;
import domain.model.requests.UserRequest;
import domain.model.response.ProfileInfoResponse;
import io.restassured.response.Response;

import domain.generators.NameGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;

import java.util.Map;
import java.util.stream.Stream;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Selenide.switchTo;
import static org.assertj.core.api.Assertions.assertThat;

public class EditNameUITests {

    private ScenarioContext context = new ScenarioContext();

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.0.27:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true));
    }

    @BeforeEach
    void setUp() {
        //создание модельки юзера
        UserRequest userRequest = new CreateUserRequestBuilder().userBuild();
        //отправка модельки юзера на сервер и сохранение токена в контекст
        Response createUserResponse = new CrudRequester(
                RequestSpecs.adminAuthSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.created()
        ).post(userRequest).extract().response();
        context.setUserTokenFromResponse(createUserResponse);

        //захожу под созданным пользоватлем
        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", context.getUserToken());
        Selenide.open("/dashboard");
    }

    @DisplayName("Проверка отображения раздела 'Edit Profile'")
    @Test
    void editProfilePageVisible() {
        //2. Переходим в Edit Profile
        Selenide.open("/edit-profile");

        //3. Проверяем что мы на странице Edit Profile
        $("input.form-control.mt-3").shouldBe(visible);
    }

    @DisplayName("Успешная смена имени профиля")
    @ParameterizedTest @MethodSource("validNameGenerator")
    void validEditName(String name) {
        //2. Переходим в Edit Profile
        Selenide.open("/edit-profile");
        $("input.form-control.mt-3").shouldBe(visible);
        Selenide.sleep(500);

        //3. Вводим новое имя
        $("input.form-control.mt-3").setValue(name);

        //4. Нажимаем на кнопку "Save Changes"
        $(byText("\uD83D\uDCBE Save Changes")).click();

        //5. Проверяем аллерт
        String alert = switchTo().alert().getText();
        assert alert.contains("Name updated successfully!");
        switchTo().alert().accept();

        //6. Возвращаемся на дашборд
        $(byText("\uD83C\uDFE0 Home")).click();

        //7. Проверяем ui — имя отображается в заголовке
        $(".welcome-text").shouldHave(text(name));

        //8. Проверяем api что имя поменялось
        ProfileInfoResponse profileInfo = new ValidatedCrudRequester<ProfileInfoResponse>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.PROFILE_INFO,
                ResponseSpecs.ok()
        ).get();
        assertThat(profileInfo.getName()).isEqualTo(name);
    }

    @DisplayName("Невалидный формат имени")
    @ParameterizedTest @MethodSource("invalidNameGenerator")
    void invalidEditName(String name) {
        //2. Переходим в Edit Profile
        Selenide.open("/edit-profile");
        $("input.form-control.mt-3").shouldBe(visible);
        Selenide.sleep(500);

        //3. Вводим невалидное имя
        $("input.form-control.mt-3").setValue(name);

        //4. Нажимаем на кнопку "Save Changes"
        $(byText("\uD83D\uDCBE Save Changes")).click();

        //5. Проверяем аллерт
        String alert = switchTo().alert().getText();
        assert alert.contains("Name must contain two words with letters only");
        switchTo().alert().accept();

        //6. Проверяем api что имя не поменялось
        ProfileInfoResponse profileInfo = new ValidatedCrudRequester<ProfileInfoResponse>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.PROFILE_INFO,
                ResponseSpecs.ok()
        ).get();
        assertThat(profileInfo.getName()).isNull();
    }

    private static Stream<String> validNameGenerator(){
        return Stream.of(
                NameGenerator.validName(),
                NameGenerator.minLengthName()
        );
    }

    private static Stream<String> invalidNameGenerator(){
        return Stream.of(
                NameGenerator.doubleSpaceName(),
                NameGenerator.threeWordsName(),
                NameGenerator.singleWordName()
        );
    }
}
