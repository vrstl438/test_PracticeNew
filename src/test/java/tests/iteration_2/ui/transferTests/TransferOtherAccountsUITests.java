package tests.iteration_2.ui.transferTests;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import api.context.ScenarioContext;
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

import static api.utils.TestUtils.generateTransferAmount;
import static api.utils.TestUtils.repeat;
import static ui.actions.Pages.DASHBOARD;
import static ui.alert.TransferAlerts.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM;
import static ui.alert.TransferAlerts.SUCCESSFULLY_TRANSFER;
import static ui.alert.TransferAlerts.TRANSFER_MIN_AMOUNT;
import static ui.alert.TransferAlerts.TRANSFER_MAX_AMOUNT;

import tests.iteration_2.ui.BaseUITest;

public class TransferOtherAccountsUITests extends BaseUITest {

    private static final int INITIAL_DEPOSIT_COUNT = 3;
    private static final double DEPOSIT_AMOUNT = 5000.0;
    private static final double INITIAL_BALANCE = INITIAL_DEPOSIT_COUNT * DEPOSIT_AMOUNT;

    private ScenarioContext senderContext = new ScenarioContext();
    private ScenarioContext receiverContext = new ScenarioContext();
    private AccountResponse senderAccount;
    private AccountResponse receiverAccount;

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
        senderContext.setUserTokenFromResponse(senderUserResponse);
        receiverContext.setUserTokenFromResponse(receiverUserResponse);

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
        DepositRequest depositRequest = new CreateDepositRequestBuilder()
                .withId(senderAccount.getId())
                .withBalance(DEPOSIT_AMOUNT)
                .depositBuild();
        ValidatedCrudRequester<DepositResponse> depositRequester = new ValidatedCrudRequester<>(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.DEPOSIT,
                ResponseSpecs.ok()
        );
        repeat(INITIAL_DEPOSIT_COUNT, () -> depositRequester.post(depositRequest));

        // захожу под отправителем
        UserActions.openPageAsCreatedUser(DASHBOARD, senderContext.getUserToken());
    }

    @DisplayName("Перевод на чужой аккаунт (валидная сумма)")
    @ParameterizedTest @ValueSource(strings = {"1000", "1"})
    void validTransferToOtherAccount(String amount) {
        new TransferPage().open().createTransfer(
                senderAccount.getAccountNumber(),
                receiverAccount.getAccountNumber(),
                amount
        ).checkAlertMessageAndAccept(SUCCESSFULLY_TRANSFER.getMessage());

        //9. Проверяем api — транзакции на отправителе
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(senderAccount.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT + 1, INITIAL_BALANCE + Double.parseDouble(amount));

        //10. Проверяем api — транзакции на получателе
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(receiverContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(receiverAccount.getId());
        ModelAssertions.assertTransactions(receiverTransactions, 1, Double.parseDouble(amount));
    }

    @DisplayName("Попытка перевода на чужой аккаунт без подтверждения чекбоксом")
    @Test
    void transferWithoutConfirmation() {
        new TransferPage().open().createTransferWithoutConfirmation(
                senderAccount.getAccountNumber(),
                receiverAccount.getAccountNumber(),
                generateTransferAmount()
        ).checkAlertMessageAndAccept(PLEASE_FILL_ALL_FIELDS_AND_CONFIRM.getMessage());

        //9. Проверяем api что трансфер не прошел
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(senderAccount.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);

        //10. Получатель пустой
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(receiverContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(receiverAccount.getId());
        ModelAssertions.assertTransactions(receiverTransactions, 0, 0.0);
    }

    @DisplayName("Попытка перевода на чужой аккаунт без заполнения полей")
    @Test
    void transferWithoutFillingFields() {
        new TransferPage().open().submitOnlyWithConfirmation()
                .checkAlertMessageAndAccept(PLEASE_FILL_ALL_FIELDS_AND_CONFIRM.getMessage());

        //6. Проверяем api что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(senderAccount.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);
    }

    @DisplayName("Перевод на чужой аккаунт с суммой меньше минимальной")
    @ParameterizedTest @ValueSource(strings = {"-1", "0", "0.001"})
    void transferBelowMinAmount(String amount) {
        new TransferPage().open().createTransfer(
                senderAccount.getAccountNumber(),
                receiverAccount.getAccountNumber(),
                amount
        ).checkAlertMessageAndAccept(TRANSFER_MIN_AMOUNT.getMessage());

        //9. Проверяем api что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(senderAccount.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);
    }

    @DisplayName("Перевод на чужой аккаунт с суммой больше максимальной")
    @ParameterizedTest @ValueSource(strings = {"10001", "50000"})
    void transferAboveMaxAmount(String amount) {
        new TransferPage().open().createTransfer(
                senderAccount.getAccountNumber(),
                receiverAccount.getAccountNumber(),
                amount
        ).checkAlertMessageAndAccept(TRANSFER_MAX_AMOUNT.getMessage());

        //9. Проверяем api что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(senderAccount.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);
    }
}
