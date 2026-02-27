package tests.iteration_2.api.transferTests;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.context.ScenarioContext;
import api.domain.builders.CreateDepositRequestBuilder;
import api.domain.builders.CreateTransferRequestBuilder;
import api.domain.builders.CreateUserRequestBuilder;
import api.domain.model.comparison.ModelAssertions;
import api.domain.model.requests.DepositRequest;
import api.domain.model.requests.TransferRequest;
import api.domain.model.requests.UserRequest;
import api.domain.model.response.AccountResponse;
import api.domain.model.response.AccountResponse.Transaction;
import api.domain.model.response.DepositResponse;
import api.domain.model.response.TransferResponse;

import java.util.List;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import api.skelethon.Endpoint;
import api.skelethon.requesters.CrudRequester;
import api.skelethon.requesters.ValidatedCrudRequester;

import static api.utils.TestUtils.repeat;
import static common.datakeys.Keys.USER_TOKEN;
import static common.datakeys.ResponseKey.FIRST_CREATE_ACC;
import static common.datakeys.ResponseKey.SECOND_CREATE_ACC;

public class TransferYourAccountsTests {

    //что бы не было магический чисел
    private static final int INITIAL_DEPOSIT_COUNT = 3;
    private static final double DEPOSIT_AMOUNT = 5000.0;
    private static final double INITIAL_BALANCE = INITIAL_DEPOSIT_COUNT * DEPOSIT_AMOUNT;

    private ScenarioContext context = new ScenarioContext();

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
        context.saveData(USER_TOKEN, createUserResponse.getHeader(RequestSpecs.AUTHORIZATION_HEADER));
        //создание аккаунтов
        ValidatedCrudRequester<AccountResponse> accountRequester = new ValidatedCrudRequester<>(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.ACCOUNTS,
                ResponseSpecs.created()
        );
        AccountResponse accountResponse = accountRequester.post();
        AccountResponse accountResponse1 = accountRequester.post();
        context.saveData(FIRST_CREATE_ACC, accountResponse);
        context.saveData(SECOND_CREATE_ACC, accountResponse1);

        DepositRequest depositRequest = new CreateDepositRequestBuilder().withId(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId()).withBalance(DEPOSIT_AMOUNT).depositBuild();
        ValidatedCrudRequester<DepositResponse> depositRequester = new ValidatedCrudRequester<>(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
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
                .withSenderAccountId(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId())
                .withReceiverAccountId(context.getData(SECOND_CREATE_ACC, AccountResponse.class).getId())
                .withAmount(amount)
                .transferBuild();
        //перевод с первого аккаунта на второй
        TransferResponse transferResponse1 = new ValidatedCrudRequester<TransferResponse>(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSFER,
                ResponseSpecs.ok()
        ).post(transferRequest);
        //проверка ответа, по кастомному ассерту
        ModelAssertions.assertTransferCreated(transferResponse1, transferRequest);

        //проверка транзакций на отправителе
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT + 1, INITIAL_BALANCE + amount);

        //проверка транзакций на получателе
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(SECOND_CREATE_ACC, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(receiverTransactions, 1, amount);
    }

    @DisplayName("Перевод между своими счетами, < 0")
    @ParameterizedTest @ValueSource(doubles = {0, -1, -10001, -0.1})
    void transferBetweenYorAccountInvalidNegativeNumberData(double amount){
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId())
                .withReceiverAccountId(context.getData(SECOND_CREATE_ACC, AccountResponse.class).getId())
                .withAmount(amount)
                .transferBuild();

        Response transferResponse = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSFER,
                ResponseSpecs.badRequest()
        ).post(transferRequest).extract().response();

        ModelAssertions.assertPlainErrorMessage(transferResponse, ResponseSpecs.TRANSFER_MIN_AMOUNT);

        //проверка что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);

        //получатель пустой
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(SECOND_CREATE_ACC, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(receiverTransactions, 0, 0.0);
    }

    @DisplayName("Перевод между своими счетами, > 10000")
    @ParameterizedTest @ValueSource(doubles = {10000.1, 10001})
    void transferBetweenYorAccountInvalidData(double amount){
        TransferRequest transferRequest = new CreateTransferRequestBuilder()
                .withSenderAccountId(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId())
                .withReceiverAccountId(context.getData(SECOND_CREATE_ACC, AccountResponse.class).getId())
                .withAmount(amount)
                .transferBuild();

        Response transferResponse = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSFER,
                ResponseSpecs.badRequest()
        ).post(transferRequest).extract().response();

        ModelAssertions.assertPlainErrorMessage(transferResponse, ResponseSpecs.TRANSFER_MAX_AMOUNT);

        //проверка что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);

        //получатель пустой
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(SECOND_CREATE_ACC, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(receiverTransactions, 0, 0.0);
    }
}
