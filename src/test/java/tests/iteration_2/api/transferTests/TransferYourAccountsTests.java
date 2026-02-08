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

public class TransferYourAccountsTests {

    //что бы не было магический чисел
    private static final int INITIAL_DEPOSIT_COUNT = 3;
    private static final double DEPOSIT_AMOUNT = 5000.0;
    private static final double INITIAL_BALANCE = INITIAL_DEPOSIT_COUNT * DEPOSIT_AMOUNT;

    private ScenarioContext context = new ScenarioContext();
    private AccountResponse accountResponse;
    private AccountResponse accountResponse1;

    //ПОЗЖЕ сделать удаление всех созданных сущностей с помощью аннотации @AfterEach

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
        context.setUserToken(createUserResponse);
        //создание аккаунтов
        ValidatedCrudRequester<AccountResponse> accountRequester = new ValidatedCrudRequester<>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.created()
        );
        accountResponse = accountRequester.post();
        accountResponse1 = accountRequester.post();

        DepositRequest depositRequest = new CreateDepositRequestBuilder().withId(accountResponse.getId()).withBalance(DEPOSIT_AMOUNT).depositBuild();
        ValidatedCrudRequester<DepositResponse> depositRequester = new ValidatedCrudRequester<>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.DEPOSIT,
                ResponseSpecs.ok()
        );
        repeat(INITIAL_DEPOSIT_COUNT, () -> depositRequester.post(depositRequest));
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
        TransferResponse transferResponse1 = new ValidatedCrudRequester<TransferResponse>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSFER,
                ResponseSpecs.ok()
        ).post(transferRequest);
        //проверка ответа, по кастомному ассерту
        UserAssertions.assertTransferCreated(transferResponse1, transferRequest);

        //проверка транзакций на отправителе
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(accountResponse.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);
        UserAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT + 1, INITIAL_BALANCE + amount);

        //проверка транзакций на получателе
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(accountResponse1.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);
        UserAssertions.assertTransactions(receiverTransactions, 1, amount);
    }

    @DisplayName("Перевод между своими счетами, < 0")
    @ParameterizedTest @ValueSource(doubles = {0, -1, -10001, -0.1})
    void transferBetweenYorAccountInvalidNegativeNumberData(double amount){
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(accountResponse.getId())
                .withReceiverAccountId(accountResponse1.getId())
                .withAmount(amount)
                .transferBuild();

        Response transferResponse = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSFER,
                ResponseSpecs.badRequest()
        ).post(transferRequest).extract().response();

        UserErrorAssertions.assertPlainErrorMessage(transferResponse, "Transfer amount must be at least 0.01");

        //проверка что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(accountResponse.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);
        UserAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);

        //получатель пустой
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(accountResponse1.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);
        UserAssertions.assertTransactions(receiverTransactions, 0, 0.0);
    }

    @DisplayName("Перевод между своими счетами, > 10000")
    @ParameterizedTest @ValueSource(doubles = {10000.1, 10001})
    void transferBetweenYorAccountInvalidData(double amount){
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(accountResponse.getId())
                .withReceiverAccountId(accountResponse1.getId())
                .withAmount(amount)
                .transferBuild();

        Response transferResponse = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSFER,
                ResponseSpecs.badRequest()
        ).post(transferRequest).extract().response();

        UserErrorAssertions.assertPlainErrorMessage(transferResponse, "Transfer amount cannot exceed 10000");

        //проверка что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(accountResponse.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);
        UserAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);

        //получатель пустой
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(accountResponse1.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);
        UserAssertions.assertTransactions(receiverTransactions, 0, 0.0);
    }
}