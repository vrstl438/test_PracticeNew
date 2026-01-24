package tests.iteration_2;

import api.client.AdminApiClient;
import api.client.UserApiClient;
import assertions.UserAssertions;
import assertions.UserErrorAssertions;
import domain.builders.CreateDepositRequestBuilder;
import domain.builders.CreateUserRequestBuilder;
import domain.model.requests.DepositRequest;
import domain.model.requests.UserRequest;
import domain.model.response.DepositResponse;
import domain.model.response.UserResponse;
import domain.model.response.AccountResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DepositTests {
    private AdminApiClient adminApiClient = new AdminApiClient();
    private UserApiClient userApiClient = new UserApiClient();
    private AccountResponse accountResponse;

    @BeforeEach
    void setUp(){
        //создание модельки пользователя
        UserRequest userRequest = new CreateUserRequestBuilder().userBuild();
        //отправка запроса на создание юзера
        UserResponse userResponse = adminApiClient.createUser(userRequest, 201);
        //проверка результата, через кастомный ассерт
//        UserAssertions.assertUserCreated(userResponse, userRequest);

        //создаем аккаунт от имени только что созданного юзера
        Response response = userApiClient.createAccount(adminApiClient.getUserToken(), 201);
        accountResponse = response.as(AccountResponse.class);
    }



    @DisplayName("Депозит с валидной суммой на существующий аккаунт")
    @ParameterizedTest @ValueSource(doubles = {4999, 5000, 0.1, 1})
    void validDeposit(double amountDeposit) {
        //создаем депозит модельку
        DepositRequest depositRequest = new CreateDepositRequestBuilder().withId(accountResponse.getId()).withBalance(amountDeposit).depositBuild();
        //отправляем наш созданный депозит
        Response response = userApiClient.createDeposit(depositRequest, adminApiClient.getUserToken(), 200);
        DepositResponse depositResponse = response.as(DepositResponse.class);
        //проверка депозита
        UserAssertions.assertDepositCreated(depositResponse, depositRequest);
    }


    @DisplayName("Попытка депозита с отрицательной и нулевой суммой")
    @ParameterizedTest @ValueSource(doubles = {-1.0 , 0.0 , 0.001})
    void invalidNegativeAndNullDeposit(double amountDeposit){
        //создание модельки невалидного депозита
        DepositRequest depositRequest = new CreateDepositRequestBuilder()
                .withId(accountResponse.getId())
                .withBalance(amountDeposit)
                .depositBuild();
        //отправка депозита
        Response response = userApiClient.createDeposit(depositRequest, adminApiClient.getUserToken(), 400);
        UserErrorAssertions.assertPlainErrorMessage(response, "Deposit amount must be at least 0.01");
    }

    @DisplayName("Попытка депозита с суммой превышающей допустимую сумму")
    @ParameterizedTest @ValueSource(doubles = {5000.001, 5000.1, 5001})
    void invalidExceedDeposit(double amountDeposit){
        DepositRequest depositRequest = new CreateDepositRequestBuilder()
                .withId(accountResponse.getId())
                .withBalance(amountDeposit)
                .depositBuild();

        Response response = userApiClient.createDeposit(depositRequest, adminApiClient.getUserToken(), 400);
        UserErrorAssertions.assertPlainErrorMessage(response, "Deposit amount cannot exceed 5000");
    }

    @DisplayName("Проверка расчета баланса, при сразу нескольких депозитах")
    @Test
    void moreDepositsAtAccountBalance(){
        //создание первого депозита
        DepositRequest depositRequest1 = new CreateDepositRequestBuilder()
                .withId(accountResponse.getId())
                .depositBuild();
        //создание второго депозита
        DepositRequest depositRequest2 = new CreateDepositRequestBuilder()
                .withId(accountResponse.getId())
                .depositBuild();

        Double totalAmountBalance = depositRequest1.getBalance() + depositRequest2.getBalance();

        Response response1 = userApiClient.createDeposit(depositRequest1, adminApiClient.getUserToken(), 200);
        Response response2 = userApiClient.createDeposit(depositRequest2, adminApiClient.getUserToken(), 200);

        DepositResponse lastDepositResponse = response2.as(DepositResponse.class);

        assert totalAmountBalance.equals(lastDepositResponse.getBalance());
    }
}