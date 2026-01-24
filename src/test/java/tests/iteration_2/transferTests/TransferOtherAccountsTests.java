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

public class TransferOtherAccountsTests {
    private AdminApiClient adminApiClient = new AdminApiClient();
    private UserApiClient userApiClient = new UserApiClient();
    private AccountResponse firstUserAccount;
    private AccountResponse secondUserAccount;

    @BeforeEach
    void preSet(){
        //Создание 2 модельки юзера
        UserRequest firstUserRequest = new CreateUserRequestBuilder().userBuild();
        UserRequest secondUserRequest = new CreateUserRequestBuilder().userBuild();
        //Отправка
        adminApiClient.createUser(firstUserRequest, 201);
        adminApiClient.createUser(secondUserRequest, 201);
        //создание аккаунтов
        Response response = userApiClient.createAccount(firstUserRequest, 201);
        firstUserAccount = response.as(AccountResponse.class);
        Response response1 = userApiClient.createAccount(secondUserRequest, 201);
        secondUserAccount = response1.as(AccountResponse.class);
        //3 депозита
        DepositRequest depositRequest = new CreateDepositRequestBuilder().withId(secondUserAccount.getId()).withBalance(5000.0).depositBuild();
        //Депозит на второго Юзера 15000
        userApiClient.createDeposit(depositRequest, adminApiClient.getUserToken(), 200);
        userApiClient.createDeposit(depositRequest, adminApiClient.getUserToken(), 200);
        userApiClient.createDeposit(depositRequest, adminApiClient.getUserToken(), 200);
    }

    @DisplayName("Перевод на чужой аккаунт (валидная сумма)")
    @ParameterizedTest
    @ValueSource(doubles = {9999, 1000, 1, 0.1})
    void transferOtherAccountsValidData(double amount){
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(secondUserAccount.getId())
                .withReceiverAccountId(firstUserAccount.getId())
                .withAmount(amount)
                .transferBuild();

        Response transferResponse = userApiClient.createTransfer(transferRequest, adminApiClient.getUserToken(), 200);
        TransferResponse transferResponse1 = transferResponse.as(TransferResponse.class);

        UserAssertions.assertTransferCreated(transferResponse1, transferRequest);
    }

    @DisplayName("Попытка перевода на чужой акк суммы (отрицательной и нулевой суммы)")
    @ParameterizedTest
    @ValueSource(doubles = {0, -1, -10001, -0.1})
    void transferOtherAccountsInvalidNegativeNumberData(double amount){
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(secondUserAccount.getId())
                .withReceiverAccountId(firstUserAccount.getId())
                .withAmount(amount)
                .transferBuild();

        Response transferResponse = userApiClient.createTransfer(transferRequest, adminApiClient.getUserToken(), 400);

        UserErrorAssertions.assertPlainErrorMessage(transferResponse, "Transfer amount must be at least 0.01");
    }

    @DisplayName("Попытка перевода на чужой акк суммы (отрицательной и нулевой суммы)")
    @ParameterizedTest
    @ValueSource(doubles = {10000.1, 10001})
    void transferOtherAccountInvalidData(double amount){
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(secondUserAccount.getId())
                .withReceiverAccountId(firstUserAccount.getId())
                .withAmount(amount)
                .transferBuild();

        Response transferResponse = userApiClient.createTransfer(transferRequest, adminApiClient.getUserToken(), 400);

        UserErrorAssertions.assertPlainErrorMessage(transferResponse, "Transfer amount cannot exceed 10000");
    }
}
