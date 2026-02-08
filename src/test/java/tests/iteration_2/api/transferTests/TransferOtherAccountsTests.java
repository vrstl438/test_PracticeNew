package tests.iteration_2.api.transferTests;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import assertions.UserAssertions;
import assertions.UserErrorAssertions;
import context.ScenarioContext;
import domain.builders.CreateDepositRequestBuilder;
import domain.builders.CreateTransferRequestBuilder;
import domain.builders.CreateUserRequestBuilder;
import domain.model.requests.DepositRequest;
import domain.model.requests.TransferRequest;
import domain.model.requests.UserRequest;
import domain.model.response.AccountResponse;
import domain.model.response.AccountResponse.Transaction;
import domain.model.response.DepositResponse;
import domain.model.response.TransferResponse;

import java.util.List;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;

import static utils.TestUtils.repeat;

public class TransferOtherAccountsTests {

    private static final int INITIAL_DEPOSIT_COUNT = 3;
    private static final double DEPOSIT_AMOUNT = 5000.0;
    private static final double INITIAL_BALANCE = INITIAL_DEPOSIT_COUNT * DEPOSIT_AMOUNT;

    private ScenarioContext senderContext = new ScenarioContext();
    private ScenarioContext receiverContext = new ScenarioContext();
    private AccountResponse senderAccount;
    private AccountResponse receiverAccount;

    @BeforeEach
    void preSet(){
        //создание моделек юзеров
        UserRequest senderUserRequest = new CreateUserRequestBuilder().userBuild();
        UserRequest receiverUserRequest = new CreateUserRequestBuilder().userBuild();
        //отправка
        CrudRequester adminRequester = new CrudRequester(
                RequestSpecs.adminAuthSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.created()
        );
        Response senderUserResponse = adminRequester.post(senderUserRequest).extract().response();
        Response receiverUserResponse = adminRequester.post(receiverUserRequest).extract().response();
        senderContext.setUserToken(senderUserResponse);
        receiverContext.setUserToken(receiverUserResponse);

        //создание аккаунтов
        senderAccount = new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.created()
        ).post();
        receiverAccount = new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.userAuthSpec(receiverContext.getUserToken()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.created()
        ).post();

        //начальные депозиты на аккаунт отправителя
        DepositRequest depositRequest = new CreateDepositRequestBuilder().withId(senderAccount.getId()).withBalance(DEPOSIT_AMOUNT).depositBuild();
        ValidatedCrudRequester<DepositResponse> depositRequester = new ValidatedCrudRequester<>(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.DEPOSIT,
                ResponseSpecs.ok()
        );
        repeat(INITIAL_DEPOSIT_COUNT, () -> depositRequester.post(depositRequest));
    }

    @DisplayName("Перевод на чужой аккаунт (валидная сумма)")
    @ParameterizedTest
    @ValueSource(doubles = {9999, 1000, 1, 0.1})
    void transferOtherAccountsValidData(double amount){
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(senderAccount.getId())
                .withReceiverAccountId(receiverAccount.getId())
                .withAmount(amount)
                .transferBuild();

        TransferResponse transferResponse = new ValidatedCrudRequester<TransferResponse>(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSFER,
                ResponseSpecs.ok()
        ).post(transferRequest);

        UserAssertions.assertTransferCreated(transferResponse, transferRequest);

        //проверка транзакций на отправителе
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(senderAccount.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);
        UserAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT + 1, INITIAL_BALANCE + amount);

        //проверка транзакций на получателе
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(receiverContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(receiverAccount.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);
        UserAssertions.assertTransactions(receiverTransactions, 1, amount);
    }

    @DisplayName("Попытка перевода на чужой акк суммы (отрицательной и нулевой суммы)")
    @ParameterizedTest
    @ValueSource(doubles = {0, -1, -10001, -0.1})
    void transferOtherAccountsInvalidNegativeNumberData(double amount){
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(senderAccount.getId())
                .withReceiverAccountId(receiverAccount.getId())
                .withAmount(amount)
                .transferBuild();

        Response transferResponse = new CrudRequester(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSFER,
                ResponseSpecs.badRequest()
        ).post(transferRequest).extract().response();

        UserErrorAssertions.assertPlainErrorMessage(transferResponse, "Transfer amount must be at least 0.01");

        //проверка что трансфер не прошел
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(senderAccount.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);
        UserAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);

        //получатель пустой
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(receiverContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(receiverAccount.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);
        UserAssertions.assertTransactions(receiverTransactions, 0, 0.0);
    }

    @DisplayName("Попытка перевода на чужой акк суммы (> 10000)")
    @ParameterizedTest
    @ValueSource(doubles = {10000.1, 10001})
    void transferOtherAccountInvalidData(double amount){
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(senderAccount.getId())
                .withReceiverAccountId(receiverAccount.getId())
                .withAmount(amount)
                .transferBuild();

        Response transferResponse = new CrudRequester(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSFER,
                ResponseSpecs.badRequest()
        ).post(transferRequest).extract().response();

        UserErrorAssertions.assertPlainErrorMessage(transferResponse, "Transfer amount cannot exceed 10000");

        //проверка что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(senderAccount.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);
        UserAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);

        //получатель пустой
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(receiverContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(receiverAccount.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);
        UserAssertions.assertTransactions(receiverTransactions, 0, 0.0);
    }
}
