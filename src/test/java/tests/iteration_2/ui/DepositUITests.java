package tests.iteration_2.ui;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.annotations.CreateUserAndAccount;
import api.domain.model.comparison.ModelAssertions;
import api.domain.model.response.AccountResponse;
import api.domain.model.response.AccountResponse.Transaction;
import common.annotations.DeleteCreatedEntity;
import common.annotations.OpenAsUser;
import common.context.ScenarioContext;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import api.skelethon.Endpoint;
import api.skelethon.requesters.CrudRequester;
import ui.actions.UserActions;
import ui.pages.DepositPage;

import static com.codeborne.selenide.Condition.*;
import static common.datakeys.Keys.USER_TOKEN;
import static common.datakeys.ResponseKey.FIRST_CREATE_ACC;
import static ui.actions.Pages.DASHBOARD;
import static ui.actions.Pages.DEPOSIT;
import static ui.alert.DepositAlerts.SUCCESSFULLY_DEPOSITED;
import static ui.alert.DepositAlerts.PLEASE_ENTER_VALID_AMOUNT;
import static ui.alert.DepositAlerts.PLEASE_DEPOSIT_LESS_OR_EQUAL_5000;
import static ui.alert.DepositAlerts.PLEASE_SELECT_ACCOUNT;

@DeleteCreatedEntity
public class DepositUITests extends BaseUITest {

    private ScenarioContext context = new ScenarioContext();

    @CreateUserAndAccount @OpenAsUser(page = DASHBOARD)
    @Test @DisplayName("Проверка отображения раздела 'Deposit Money'")
    void LoginTest() {
        new DepositPage().open().getDepositPageLabel().shouldBe(visible);
    }

    @CreateUserAndAccount @OpenAsUser(page = DEPOSIT)
    @DisplayName("Проверка успешного депозита") @ParameterizedTest @ValueSource(strings = {"4999", "5000", "1"})
    void CreateDepositTests(String amount) {

        new DepositPage().createDeposit(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getAccountNumber(), amount)
                .checkAlertMessageAndAccept(SUCCESSFULLY_DEPOSITED.getMessage());

        //7. Проверяем ui
        UserActions.openPageAsCreatedUser(DEPOSIT);

        new DepositPage().chekCreatedDepositInDropdown(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getAccountNumber(), amount);


        //8. Проверяем api
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId());

        ModelAssertions.assertTransactions(transactions, 1, Double.parseDouble(amount));
    }

    @CreateUserAndAccount @OpenAsUser(page = DEPOSIT)
    @DisplayName("Депозит с отрицательной и нулевой суммой") @ParameterizedTest @ValueSource(strings = {"-1.0" , "0.0" , "0.001"})
    void CreateInvalidDepositTest(String amount){
        new DepositPage().open().createDeposit(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getAccountNumber(), amount)
                .checkAlertMessageAndAccept(PLEASE_ENTER_VALID_AMOUNT.getMessage());

        //7. Проверяем ui
        UserActions.openPageAsCreatedUser(DEPOSIT);

        new DepositPage().open().chekCreatedDepositInDropdown(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getAccountNumber(), "0.00");

        //8. Проверяем api
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId());

        ModelAssertions.assertTransactions(transactions, 0, 0.0);
    }

    @CreateUserAndAccount @OpenAsUser(page = DEPOSIT)
    @DisplayName("Депозита с суммой превышающей допустимую сумму") @ParameterizedTest @ValueSource(strings = {"5000.001", "5000.1", "5001"})
    void createDepositInvalidMaxValue(String amount){
        new DepositPage().open().createDeposit(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getAccountNumber(), amount)
                .checkAlertMessageAndAccept(PLEASE_DEPOSIT_LESS_OR_EQUAL_5000.getMessage());

        //7. Проверяем ui
        UserActions.openPageAsCreatedUser(DEPOSIT);

        new DepositPage().open().chekCreatedDepositInDropdown(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getAccountNumber(), "0.00");

        //8. Проверяем api
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId());

        ModelAssertions.assertTransactions(transactions, 0, 0.0);
    }

    @CreateUserAndAccount @OpenAsUser(page = DEPOSIT)
    @Test @DisplayName("Депозит без выбора аккаунта")
    void createDepositWithoutSelectingAccount(){
        new DepositPage().open().createDeposit("-- Choose an account --", "1000")
                .checkAlertMessageAndAccept(PLEASE_SELECT_ACCOUNT.getMessage());

        //6. Проверяем api что транзакция не создалась
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId());

        ModelAssertions.assertTransactions(transactions, 0, 0.0);
    }

    @CreateUserAndAccount(accountCount = 1) @OpenAsUser(page = DEPOSIT)
    @Test @DisplayName("Два депозита подряд - баланс складывается в UI")
    void twoDepositsBalanceAccumulates(){

        new DepositPage().open().createDeposit(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getAccountNumber(), "1000")
                .checkAlertMessageAndAccept(SUCCESSFULLY_DEPOSITED.getMessage());

        //6. Заходим снова в раздел "Депозит"
        UserActions.openPageAsCreatedUser(DEPOSIT);

        //7. Выбираем тот же аккаунт и делаем второй депозит
        new DepositPage().open().createDeposit(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getAccountNumber(), "2000")
                .checkAlertMessageAndAccept(SUCCESSFULLY_DEPOSITED.getMessage());

        //9. Проверяем ui — баланс должен быть 3000
        UserActions.openPageAsCreatedUser(DEPOSIT);

        new DepositPage().open().chekCreatedDepositInDropdown(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getAccountNumber(), "3000");

        //10. Проверяем api — 2 транзакции на сумму 3000
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId());

        ModelAssertions.assertTransactions(transactions, 2, 3000.0);
    }
}