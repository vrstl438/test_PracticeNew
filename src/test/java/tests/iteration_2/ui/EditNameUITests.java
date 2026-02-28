package tests.iteration_2.ui;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.annotations.CreateUserAndAccount;
import common.annotations.DeleteCreatedEntity;
import common.annotations.OpenAsUser;
import common.context.ScenarioContext;
import api.domain.model.response.ProfileInfoResponse;

import api.domain.generators.NameGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import api.skelethon.Endpoint;
import api.skelethon.requesters.ValidatedCrudRequester;
import ui.actions.UserActions;
import ui.pages.DashboardPage;
import ui.pages.EditProfilePage;

import java.util.stream.Stream;

import static com.codeborne.selenide.Condition.*;
import static org.assertj.core.api.Assertions.assertThat;
import static ui.actions.Pages.DASHBOARD;
import static common.datakeys.Keys.USER_TOKEN;
import static ui.actions.Pages.EDIT_PROFILE;
import static ui.alert.EditProfileAlerts.NAME_UPDATED_SUCCESSFULLY;
import static ui.alert.EditProfileAlerts.NAME_MUST_CONTAIN_TWO_WORDS;

@DeleteCreatedEntity
public class EditNameUITests extends BaseUITest {

    private ScenarioContext context = new ScenarioContext();

    @CreateUserAndAccount(accountCount = 0) @OpenAsUser(page = EDIT_PROFILE)
    @DisplayName("Проверка отображения раздела 'Edit Profile'") @Test
    void editProfilePageVisible() {
        //2. Переходим в Edit Profile и проверяем что поле ввода видимо
        new EditProfilePage().getNameInput().shouldBe(visible);
    }

    @CreateUserAndAccount(accountCount = 0) @OpenAsUser(page = EDIT_PROFILE)
    @DisplayName("Успешная смена имени профиля") @ParameterizedTest @MethodSource("validNameGenerator")
    void validEditName(String name) {
        //2. Переходим в Edit Profile и вводим имя
        new EditProfilePage().editName(name).checkAlertMessageAndAccept(NAME_UPDATED_SUCCESSFULLY.getMessage());

        //6. Возвращаемся на дашборд
        UserActions.openPageAsCreatedUser(DASHBOARD);

        //7. Проверяем ui — имя отображается в заголовке
        new DashboardPage().getWelcomeText().shouldHave(text(name));

        //8. Проверяем api что имя поменялось
        ProfileInfoResponse profileInfo = new ValidatedCrudRequester<ProfileInfoResponse>(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.PROFILE_INFO,
                ResponseSpecs.ok()
        ).get();
        assertThat(profileInfo.getName()).isEqualTo(name);
    }

    @CreateUserAndAccount(accountCount = 0) @OpenAsUser(page = EDIT_PROFILE)
    @DisplayName("Невалидный формат имени") @ParameterizedTest @MethodSource("invalidNameGenerator")
    void invalidEditName(String name) {
        //2. Переходим в Edit Profile и вводим невалидное имя
        new EditProfilePage().editName(name)
                .checkAlertMessageAndAccept(NAME_MUST_CONTAIN_TWO_WORDS.getMessage());

        //6. Проверяем api что имя не поменялось
        ProfileInfoResponse profileInfo = new ValidatedCrudRequester<ProfileInfoResponse>(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
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
