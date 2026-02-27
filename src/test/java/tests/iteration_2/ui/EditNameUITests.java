package tests.iteration_2.ui;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import api.context.ScenarioContext;
import api.domain.builders.CreateUserRequestBuilder;
import api.domain.model.requests.UserRequest;
import api.domain.model.response.ProfileInfoResponse;
import io.restassured.response.Response;

import api.domain.generators.NameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import api.skelethon.Endpoint;
import api.skelethon.requesters.CrudRequester;
import api.skelethon.requesters.ValidatedCrudRequester;
import ui.actions.UserActions;
import ui.pages.EditProfilePage;

import java.util.stream.Stream;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;
import static ui.actions.Pages.DASHBOARD;
import static ui.alert.EditProfileAlerts.NAME_UPDATED_SUCCESSFULLY;
import static ui.alert.EditProfileAlerts.NAME_MUST_CONTAIN_TWO_WORDS;

public class EditNameUITests extends BaseUITest {

    private ScenarioContext context = new ScenarioContext();

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
        UserActions.openPageAsCreatedUser(DASHBOARD, context.getUserToken());
    }

    @DisplayName("Проверка отображения раздела 'Edit Profile'")
    @Test
    void editProfilePageVisible() {
        //2. Переходим в Edit Profile и проверяем что поле ввода видимо
        new EditProfilePage().open().getNameInput().shouldBe(visible);
    }

    @DisplayName("Успешная смена имени профиля")
    @ParameterizedTest @MethodSource("validNameGenerator")
    void validEditName(String name) {
        //2. Переходим в Edit Profile и вводим имя
        EditProfilePage editProfilePage = new EditProfilePage().open();
        editProfilePage.editName(name).checkAlertMessageAndAccept(NAME_UPDATED_SUCCESSFULLY.getMessage());

        //6. Возвращаемся на дашборд
        editProfilePage.goHome();

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
        //2. Переходим в Edit Profile и вводим невалидное имя
        new EditProfilePage().open().editName(name)
                .checkAlertMessageAndAccept(NAME_MUST_CONTAIN_TWO_WORDS.getMessage());

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
