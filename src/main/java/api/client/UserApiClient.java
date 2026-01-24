package api.client;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import domain.model.requests.DepositRequest;
import domain.model.requests.EditNameRequest;
import domain.model.requests.TransferRequest;
import domain.model.requests.UserRequest;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class UserApiClient {
    private static final String PROFILE_URL = "/api/v1/customer/profile";
    private static final String CREATE_ACCOUNT_URL = "/api/v1/accounts";
    private static final String CREATE_DEPOSIT_URL = "/api/v1/accounts/deposit";
    private static final String TRANSFER_URL = "api/v1/accounts/transfer";
    private static final String EDIT_NAME_URL = "api/v1/customer/profile";

    public Response createAccount(String token, int expectedStatusCode) {
        return given()
                .spec(RequestSpecs.userAuthSpec(token))
                .post(CREATE_ACCOUNT_URL)
                .then()
                .spec(ResponseSpecs.expectedStatusCode(expectedStatusCode))
                .extract()
                .response();
//                .as(CreatedAccountResponse.class);
    }

    public Response createAccount(UserRequest user, int expectedStatusCode) {
        return given()
                .spec(RequestSpecs.userAuthSpec(user.getUsername(), user.getPassword()))
                .post(CREATE_ACCOUNT_URL)
                .then()
                .spec(ResponseSpecs.expectedStatusCode(expectedStatusCode))
                .extract()
                .response();
//                .as(CreatedAccountResponse.class);
    }


    public Response createDeposit(DepositRequest deposit, String token, int expectedStatusCode){
        return given()
                .spec(RequestSpecs.userAuthSpec(token))
                .body(deposit)
                .post(CREATE_DEPOSIT_URL)
                .then()
                .spec(ResponseSpecs.expectedStatusCode(expectedStatusCode))
                .extract()
                .response();
//                .as(CreatedDepositResponse.class);
    }

    public Response createTransfer(TransferRequest transfer, String token, int expectedStatusCode){
        return given()
                .spec(RequestSpecs.userAuthSpec(token))
                .body(transfer)
                .post(TRANSFER_URL)
                .then()
                .spec(ResponseSpecs.expectedStatusCode(expectedStatusCode))
                .extract()
                .response();
        //                .as(CreatedDepositResponse.class);
    }

    public Response editName(EditNameRequest editNameRequest, String token, int expectedStatusCode){
        return given()
                .spec(RequestSpecs.userAuthSpec(token))
                .body(editNameRequest)
                .put(EDIT_NAME_URL)
                .then()
                .spec(ResponseSpecs.expectedStatusCode(expectedStatusCode))
                .extract()
                .response();
    }
}