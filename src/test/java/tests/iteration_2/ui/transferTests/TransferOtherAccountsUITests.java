package tests.iteration_2.ui.transferTests;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.context.ScenarioContext;
import api.domain.builders.CreateDepositRequestBuilder;
import api.domain.builders.CreateUserRequestBuilder;
import api.domain.model.comparison.ModelAssertions;
import api.domain.model.requests.DepositRequest;
import api.domain.model.requests.UserRequest;
import api.domain.model.response.AccountResponse;
import api.domain.model.response.AccountResponse.Transaction;
import api.domain.model.response.DepositResponse;
import io.restassured.response.Response;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import api.skelethon.Endpoint;
import api.skelethon.requesters.CrudRequester;
import api.skelethon.requesters.ValidatedCrudRequester;
import ui.actions.UserActions;
import ui.pages.TransferPage;

import static api.specs.RequestSpecs.AUTHORIZATION_HEADER;
import static api.utils.TestUtils.repeat;
import static ui.actions.Pages.DASHBOARD;
import static ui.alert.TransferAlerts.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM;
import static ui.alert.TransferAlerts.SUCCESSFULLY_TRANSFER;
import static ui.alert.TransferAlerts.TRANSFER_MIN_AMOUNT;
import static ui.alert.TransferAlerts.TRANSFER_MAX_AMOUNT;
import static common.datakeys.Keys.RECEIVER_TOKEN;
import static common.datakeys.Keys.SENDER_TOKEN;
import static common.datakeys.ResponseKey.RECEIVER_ACCOUNT;
import static common.datakeys.ResponseKey.SENDER_ACCOUNT;

import tests.iteration_2.ui.BaseUITest;

public class TransferOtherAccountsUITests extends BaseUITest {

    private static final int INITIAL_DEPOSIT_COUNT = 3;
    private static final double DEPOSIT_AMOUNT = 5000.0;
    private static final double INITIAL_BALANCE = INITIAL_DEPOSIT_COUNT * DEPOSIT_AMOUNT;

    private ScenarioContext context = new ScenarioContext();

    @BeforeEach
    void preSet() {
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
        context.saveData(SENDER_TOKEN, senderUserResponse.getHeader(AUTHORIZATION_HEADER));
        context.saveData(RECEIVER_TOKEN, receiverUserResponse.getHeader(AUTHORIZATION_HEADER));

        //создание аккаунтов
        AccountResponse senderAccount = new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.userAuthSpec(context.getData(SENDER_TOKEN, String.class)),
                Endpoint.ACCOUNTS,
                ResponseSpecs.created()
        ).post();
        AccountResponse receiverAccount = new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.userAuthSpec(context.getData(RECEIVER_TOKEN, String.class)),
                Endpoint.ACCOUNTS,
                ResponseSpecs.created()
        ).post();
        context.saveData(SENDER_ACCOUNT, senderAccount);
        context.saveData(RECEIVER_ACCOUNT, receiverAccount);

        //начальные депозиты на аккаунт отправителя
        DepositRequest depositRequest = new CreateDepositRequestBuilder()
                .withId(context.getData(SENDER_ACCOUNT, AccountResponse.class).getId())
                .withBalance(DEPOSIT_AMOUNT)
                .depositBuild();
        ValidatedCrudRequester<DepositResponse> depositRequester = new ValidatedCrudRequester<>(
                RequestSpecs.userAuthSpec(context.getData(SENDER_TOKEN, String.class)),
                Endpoint.DEPOSIT,
                ResponseSpecs.ok()
        );
        repeat(INITIAL_DEPOSIT_COUNT, () -> depositRequester.post(depositRequest));

        // захожу под отправителем
        UserActions.openPageAsCreatedUser(DASHBOARD, context.getData(SENDER_TOKEN, String.class));
    }

    @DisplayName("Перевод на чужой аккаунт (валидная сумма)")
    @ParameterizedTest @ValueSource(strings = {"1000", "1"})
    void validTransferToOtherAccount(String amount) {
        new TransferPage().open().createTransfer(
                context.getData(SENDER_ACCOUNT, AccountResponse.class).getAccountNumber(),
                context.getData(RECEIVER_ACCOUNT, AccountResponse.class).getAccountNumber(),
                amount
        ).checkAlertMessageAndAccept(SUCCESSFULLY_TRANSFER.getMessage());

        //9. Проверяем api — транзакции на отправителе
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(SENDER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(SENDER_ACCOUNT, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT + 1, INITIAL_BALANCE + Double.parseDouble(amount));

        //10. Проверяем api — транзакции на получателе
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(RECEIVER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(RECEIVER_ACCOUNT, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(receiverTransactions, 1, Double.parseDouble(amount));
    }

    @DisplayName("Попытка перевода на чужой аккаунт без подтверждения чекбоксом")
    @Test
    void transferWithoutConfirmation() {
        new TransferPage().open().createTransferWithoutConfirmation(
                context.getData(SENDER_ACCOUNT, AccountResponse.class).getAccountNumber(),
                context.getData(RECEIVER_ACCOUNT, AccountResponse.class).getAccountNumber(),
                "1000"
        ).checkAlertMessageAndAccept(PLEASE_FILL_ALL_FIELDS_AND_CONFIRM.getMessage());

        //9. Проверяем api что трансфер не прошел
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(SENDER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(SENDER_ACCOUNT, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);

        //10. Получатель пустой
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(RECEIVER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(RECEIVER_ACCOUNT, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(receiverTransactions, 0, 0.0);
    }

    @DisplayName("Попытка перевода на чужой аккаунт без заполнения полей")
    @Test
    void transferWithoutFillingFields() {
        new TransferPage().open().submitOnlyWithConfirmation()
                .checkAlertMessageAndAccept(PLEASE_FILL_ALL_FIELDS_AND_CONFIRM.getMessage());

        //6. Проверяем api что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(SENDER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(SENDER_ACCOUNT, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);
    }

    @DisplayName("Перевод на чужой аккаунт с суммой меньше минимальной")
    @ParameterizedTest @ValueSource(strings = {"-1", "0", "0.001"})
    void transferBelowMinAmount(String amount) {
        new TransferPage().open().createTransfer(
                context.getData(SENDER_ACCOUNT, AccountResponse.class).getAccountNumber(),
                context.getData(RECEIVER_ACCOUNT, AccountResponse.class).getAccountNumber(),
                amount
        ).checkAlertMessageAndAccept(TRANSFER_MIN_AMOUNT.getMessage());

        //9. Проверяем api что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(SENDER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(SENDER_ACCOUNT, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);
    }

    @DisplayName("Перевод на чужой аккаунт с суммой больше максимальной")
    @ParameterizedTest @ValueSource(strings = {"10001", "50000"})
    void transferAboveMaxAmount(String amount) {
        new TransferPage().open().createTransfer(
                context.getData(SENDER_ACCOUNT, AccountResponse.class).getAccountNumber(),
                context.getData(RECEIVER_ACCOUNT, AccountResponse.class).getAccountNumber(),
                amount
        ).checkAlertMessageAndAccept(TRANSFER_MAX_AMOUNT.getMessage());

        //9. Проверяем api что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(SENDER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(SENDER_ACCOUNT, AccountResponse.class).getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);
    }
}
