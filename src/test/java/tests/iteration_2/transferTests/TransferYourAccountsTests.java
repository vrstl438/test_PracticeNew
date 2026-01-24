package tests.iteration_2.transferTests;

import api.client.AdminApiClient;
import api.client.UserApiClient;
import assertions.UserAssertions;
import assertions.UserErrorAssertions;
import domain.builders.CreateDepositRequestBuilder;
import domain.builders.CreateTransferRequestBuilder;
import domain.builders.CreateUserRequestBuilder;
import domain.model.requests.DepositRequest;
import domain.model.requests.TransferRequest;
import domain.model.requests.UserRequest;
import domain.model.response.AccountResponse;
import domain.model.response.TransferResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TransferYourAccountsTests {

    private AdminApiClient adminApiClient = new AdminApiClient();
    private UserApiClient userApiClient = new UserApiClient();
    private AccountResponse accountResponse;
    private AccountResponse accountResponse1;

    //ПОЗЖЕ сделать удаление всех созданных сущностей с помощью аннотации @AfterEach

    @BeforeEach
    void setUp() {
        //создание модельки юзера
        UserRequest userRequest = new CreateUserRequestBuilder().userBuild();
        //отправка модельки юзера на сервер
        adminApiClient.createUser(userRequest, 201);
        //создание аккаунтов
        Response response = userApiClient.createAccount(adminApiClient.getUserToken(), 201);
        accountResponse = response.as(AccountResponse.class);
        Response response1 = userApiClient.createAccount(adminApiClient.getUserToken(), 201);
        accountResponse1 = response.as(AccountResponse.class);
        //3 депозита
        DepositRequest depositRequest = new CreateDepositRequestBuilder().withId(accountResponse.getId()).withBalance(5000.0).depositBuild();
        //если бы это был масштабный проект, то сделал бы метод, который создавал бы депозит под капотом на любую сумму, путей отправкой под капотом множество запросов на 500,
        //т.к проект учебный не стал делать такой метод, а просто 3 раза продублировал депозит, что тоже работает
        userApiClient.createDeposit(depositRequest, adminApiClient.getUserToken(), 200);
        userApiClient.createDeposit(depositRequest, adminApiClient.getUserToken(), 200);
        userApiClient.createDeposit(depositRequest, adminApiClient.getUserToken(), 200);
    }

    @DisplayName("Перевода между своими счетами, валидная сумма")
    @ParameterizedTest @ValueSource(doubles = {9999, 1000, 1, 0.1})
    void transferBetweenYourAccountsValidData(double amount) {
        //создание модельки трансфера
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(accountResponse.getId())
                .withReceiverAccountId(accountResponse1.getId())
                .withAmount(amount)
                .transferBuild();
        //перевод с первого аккаунта на второй
        Response transferResponse = userApiClient.createTransfer(transferRequest, adminApiClient.getUserToken(), 200);
        TransferResponse transferResponse1 = transferResponse.as(TransferResponse.class);
        //проверка ответа, по кастомному ассерту
        UserAssertions.assertTransferCreated(transferResponse1, transferRequest);
    }

    @DisplayName("Перевод между своими счетами, < 0")
    @ParameterizedTest @ValueSource(doubles = {0, -1, -10001, -0.1})
    void transferBetweenYorAccountInvalidNegativeNumberData(double amount){
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(accountResponse.getId())
                .withReceiverAccountId(accountResponse1.getId())
                .withAmount(amount)
                .transferBuild();

        Response transferResponse = userApiClient.createTransfer(transferRequest, adminApiClient.getUserToken(), 400);

        UserErrorAssertions.assertPlainErrorMessage(transferResponse, "Transfer amount must be at least 0.01");
    }

    @DisplayName("Перевод между своими счетами, > 10000")
    @ParameterizedTest @ValueSource(doubles = {10000.1, 10001})
    void transferBetweenYorAccountInvalidData(double amount){
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(accountResponse.getId())
                .withReceiverAccountId(accountResponse1.getId())
                .withAmount(amount)
                .transferBuild();

        Response transferResponse = userApiClient.createTransfer(transferRequest, adminApiClient.getUserToken(), 400);

        UserErrorAssertions.assertPlainErrorMessage(transferResponse, "Transfer amount cannot exceed 10000");
    }
}