package tests.iteration_2;

import api.client.AdminApiClient;
import api.client.UserApiClient;
import assertions.UserAssertions;
import assertions.UserErrorAssertions;
import domain.builders.CreateUserRequestBuilder;
import domain.generators.NameGenerator;
import domain.model.requests.EditNameRequest;
import domain.model.requests.UserRequest;
import domain.model.response.EditUserResponse;
import domain.model.response.UserResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class EditNameUserTests {
    private AdminApiClient adminApiClient = new AdminApiClient();
    private UserApiClient userApiClient = new UserApiClient();

    @BeforeEach
    void preSet() {
        UserRequest userRequest = new CreateUserRequestBuilder().userBuild();
        UserResponse userResponse = adminApiClient.createUser(userRequest, 201);
    }

    @DisplayName("Валидный формат имени")
    @ParameterizedTest @MethodSource("validNameGenerator")
    void validEditingName(String name) {
        EditNameRequest editNameRequest = new EditNameRequest()
                .setName(name);

        Response response = userApiClient.editName(editNameRequest, adminApiClient.getUserToken(), 200);

        EditUserResponse editingResponse = response.as(EditUserResponse.class);

        UserAssertions.assertEditUserResponse(editingResponse, editNameRequest);
    }

    @DisplayName("Невалидный формат имени")
    @ParameterizedTest @MethodSource("invalidNameGenerator")
    void invalidEditingName(String name) {
        EditNameRequest editNameRequest = new EditNameRequest()
                .setName(name);

        Response response = userApiClient.editName(editNameRequest, adminApiClient.getUserToken(), 400);

        UserErrorAssertions.assertPlainErrorMessage(response, "Name must contain two words with letters only");
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