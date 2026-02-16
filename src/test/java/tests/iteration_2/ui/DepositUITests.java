package tests.iteration_2.ui;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import api.context.ScenarioContext;
import api.domain.builders.CreateUserRequestBuilder;
import api.domain.model.comparison.ModelAssertions;
import api.domain.model.requests.UserRequest;
import api.domain.model.response.AccountResponse;
import api.domain.model.response.AccountResponse.Transaction;
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
import ui.pages.DepositPage;

import static com.codeborne.selenide.Condition.*;
import static ui.actions.Pages.DASHBOARD;
import static ui.actions.Pages.DEPOSIT;
import static ui.alert.DepositAlerts.SUCCESSFULLY_DEPOSITED;
import static ui.alert.DepositAlerts.PLEASE_ENTER_VALID_AMOUNT;
import static ui.alert.DepositAlerts.PLEASE_DEPOSIT_LESS_OR_EQUAL_5000;
import static ui.alert.DepositAlerts.PLEASE_SELECT_ACCOUNT;


public class DepositUITests extends BaseUITest {

    private ScenarioContext context = new ScenarioContext();
    private AccountResponse accountResponse;

    @BeforeEach
    void setUp() {
        UserRequest userRequest = new CreateUserRequestBuilder().userBuild();
        Response createUserResponse = new CrudRequester(
                RequestSpecs.adminAuthSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.created()
        ).post(userRequest).extract().response();
        context.setUserTokenFromResponse(createUserResponse);

        accountResponse = new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.created()
        ).post();


        //. захожу под созданным пользоватлем на страницу дашборда
        UserActions.openPageAsCreatedUser(DEPOSIT, context.getUserToken());
    }

    @DisplayName("Проверка отображения раздела 'Deposit Money'") @Test
    void LoginTest() {
        UserActions.openPageAsCreatedUser(DASHBOARD, context.getUserToken());
        new DepositPage().open().getDepositPageLabel().shouldBe(visible);
        //Заходим в раздел с депозитами
        //Проверяем что мы переходим в этот раздел
    }

    @DisplayName("Проверка успешного депозита") @ParameterizedTest
    @ValueSource(strings = {"4999", "5000", "1"})
    void CreateDepositTests(String amount) {

        new DepositPage().open().createDeposit(accountResponse.getAccountNumber(), amount)
                .checkAlertMessageAndAccept(SUCCESSFULLY_DEPOSITED.getMessage());

        //7. Проверяем ui
        UserActions.openPageAsCreatedUser(DEPOSIT, context.getUserToken());

        new DepositPage().open().chekCreatedDepositInDropdown(accountResponse.getAccountNumber(), amount);


        //8. Проверяем api
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse.getId());

        ModelAssertions.assertTransactions(transactions, 1, Double.parseDouble(amount));
    }

    @DisplayName("Депозит с отрицательной и нулевой суммой")
    @ParameterizedTest @ValueSource(strings = {"-1.0" , "0.0" , "0.001"})
    void CreateInvalidDepositTest(String amount){
        new DepositPage().open().createDeposit(accountResponse.getAccountNumber(), amount)
                .checkAlertMessageAndAccept(PLEASE_ENTER_VALID_AMOUNT.getMessage());

        //7. Проверяем ui
        UserActions.openPageAsCreatedUser(DEPOSIT, context.getUserToken());

        new DepositPage().open().chekCreatedDepositInDropdown(accountResponse.getAccountNumber(), "0.00");

        //8. Проверяем api
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse.getId());

        ModelAssertions.assertTransactions(transactions, 0, 0.0);
    }

    @DisplayName("Депозита с суммой превышающей допустимую сумму")
    @ParameterizedTest @ValueSource(strings = {"5000.001", "5000.1", "5001"})
    void createDepositInvalidMaxValue(String amount){
        new DepositPage().open().createDeposit(accountResponse.getAccountNumber(), amount)
                .checkAlertMessageAndAccept(PLEASE_DEPOSIT_LESS_OR_EQUAL_5000.getMessage());

        //7. Проверяем ui
        UserActions.openPageAsCreatedUser(DEPOSIT, context.getUserToken());

        new DepositPage().open().chekCreatedDepositInDropdown(accountResponse.getAccountNumber(), "0.00");

        //8. Проверяем api
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse.getId());

        ModelAssertions.assertTransactions(transactions, 0, 0.0);
    }

    @DisplayName("Депозит без выбора аккаунта")
    @Test
    void createDepositWithoutSelectingAccount(){
        new DepositPage().open().createDeposit("-- Choose an account --", "1000")
                .checkAlertMessageAndAccept(PLEASE_SELECT_ACCOUNT.getMessage());

        //6. Проверяем api что транзакция не создалась
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse.getId());

        ModelAssertions.assertTransactions(transactions, 0, 0.0);
    }

    @DisplayName("Два депозита подряд - баланс складывается в UI")
    @Test
    void twoDepositsBalanceAccumulates(){

        new DepositPage().open().createDeposit(accountResponse.getAccountNumber(), "1000")
                .checkAlertMessageAndAccept(SUCCESSFULLY_DEPOSITED.getMessage());

        //6. Заходим снова в раздел "Депозит"
        UserActions.openPageAsCreatedUser(DEPOSIT, context.getUserToken());

        //7. Выбираем тот же аккаунт и делаем второй депозит
        new DepositPage().open().createDeposit(accountResponse.getAccountNumber(), "2000")
                .checkAlertMessageAndAccept(SUCCESSFULLY_DEPOSITED.getMessage());

        //9. Проверяем ui — баланс должен быть 3000
        UserActions.openPageAsCreatedUser(DEPOSIT, context.getUserToken());

        new DepositPage().open().chekCreatedDepositInDropdown(accountResponse.getAccountNumber(), "3000");

        //10. Проверяем api — 2 транзакции на сумму 3000
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse.getId());

        ModelAssertions.assertTransactions(transactions, 2, 3000.0);
    }

}