package tests.iteration_2.api;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import context.ScenarioContext;
import domain.builders.CreateUserRequestBuilder;
import domain.generators.NameGenerator;
import domain.model.comparison.ModelAssertions;
import domain.model.requests.EditNameRequest;
import domain.model.requests.UserRequest;
import domain.model.response.EditUserResponse;
import domain.model.response.ProfileInfoResponse;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;

import java.util.stream.Stream;

public class EditNameUserTests {
    private ScenarioContext context = new ScenarioContext();

    @BeforeEach
    void preSet() {
        UserRequest userRequest = new CreateUserRequestBuilder().userBuild();
        Response createUserResponse = new CrudRequester(
                RequestSpecs.adminAuthSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.created()
        ).post(userRequest).extract().response();
        context.setUserTokenFromResponse(createUserResponse);
    }

    @DisplayName("Валидный формат имени")
    @ParameterizedTest @MethodSource("validNameGenerator")
    void validEditingName(String name) {
        EditNameRequest editNameRequest = new EditNameRequest()
                .setName(name);

        EditUserResponse editingResponse = new ValidatedCrudRequester<EditUserResponse>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.EDIT_PROFILE,
                ResponseSpecs.ok()
        ).put(editNameRequest);

        ModelAssertions.assertEditUserResponse(editingResponse, editNameRequest);

        //проверка что имя поменялось
        ProfileInfoResponse profileInfo = new ValidatedCrudRequester<ProfileInfoResponse>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.PROFILE_INFO,
                ResponseSpecs.ok()
        ).get();
        assertThat(profileInfo.getName()).isEqualTo(name);
    }

    @DisplayName("Невалидный формат имени")
    @ParameterizedTest @MethodSource("invalidNameGenerator")
    void invalidEditingName(String name) {
        EditNameRequest editNameRequest = new EditNameRequest()
                .setName(name);

        //запоминаем имя до попытки изменения
        ProfileInfoResponse profileBefore = new ValidatedCrudRequester<ProfileInfoResponse>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.PROFILE_INFO,
                ResponseSpecs.ok()
        ).get();

        Response response = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.EDIT_PROFILE,
                ResponseSpecs.badRequest()
        ).put(editNameRequest).extract().response();

        ModelAssertions.assertPlainErrorMessage(response, ResponseSpecs.INVALID_NAME_FORMAT);

        //проверка что имя не поменялось
        ProfileInfoResponse profileAfter = new ValidatedCrudRequester<ProfileInfoResponse>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.PROFILE_INFO,
                ResponseSpecs.ok()
        ).get();
        assertThat(profileAfter.getName()).isEqualTo(null);
    }


    private static Stream<String> invalidNameGenerator(){
        return Stream.of(
                NameGenerator.doubleSpaceName(),
                NameGenerator.threeWordsName(),
                NameGenerator.singleWordName()
        );
    }

    private static Stream<String> validNameGenerator(){
        return Stream.of(
                NameGenerator.validName(),
                NameGenerator.minLengthName()
        );
    }
}
