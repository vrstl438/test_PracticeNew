package api.client;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import domain.model.requests.UserRequest;
import domain.model.response.UserResponse;
import io.restassured.response.Response;
import lombok.Getter;

import static io.restassured.RestAssured.given;

public class AdminApiClient {
    @Getter
    private String userToken;

    private static final String USERS_URL = "/api/v1/admin/users";
    private static final String DELETE_USER_URL = "/api/v1/admin/users/";
    private static final String GET_ALL_USERS_URL = "/api/v1/admin/users";

    public UserResponse createUser(UserRequest request, int expectedStatusCode) {
        Response response = given()
                .spec(RequestSpecs.adminAuthSpec())
                .body(request)
                .post(USERS_URL)
                .then()
                .spec(ResponseSpecs.expectedStatusCode(expectedStatusCode))
                .extract()
                .response();

        this.userToken = response.getHeader("Authorization");

        return response.as(UserResponse.class);
    }
}