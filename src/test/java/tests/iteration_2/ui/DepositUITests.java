package tests.iteration_2.ui;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import context.ScenarioContext;
import domain.builders.CreateUserRequestBuilder;
import domain.model.comparison.ModelAssertions;
import domain.model.requests.UserRequest;
import domain.model.response.AccountResponse;
import domain.model.response.AccountResponse.Transaction;
import io.restassured.response.Response;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;

import java.util.Map;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Selenide.switchTo;


public class DepositUITests {

    private ScenarioContext context = new ScenarioContext();
    private AccountResponse accountResponse;

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.0.27:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true));
    }

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


        //. захожу под созданным пользоватлем.
        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", context.getUserToken());
        Selenide.open("/dashboard");
    }

    @DisplayName("Проверка отображения раздела 'Deposit Money'") @Test
    void LoginTest() {
        //Заходим в раздел с депозитами
        $(byText("\uD83D\uDCB0 Deposit Money")).click();
        //Проверяем что мы переходим в этот раздел
        $(byText("\uD83D\uDCB0 Deposit Money")).shouldBe(visible);
    }

    @DisplayName("Проверка успешного депозита") @ParameterizedTest
    @ValueSource(strings = {"4999", "5000", "1"})
    void CreateDepositTests(String amount) {

        //2. Заходим в раздел "Депозит"
        $(byText("\uD83D\uDCB0 Deposit Money")).click();

        //3.Выбираем в дропдане созданный аккаунт
        $(".account-selector").selectOption(1);

        //4.Вводим сумму в инпут
        $(byAttribute("placeholder", "Enter amount")).setValue(amount);

        //5.Нажимаем на кнопку "депозит"
        $(byText("\uD83D\uDCB5 Deposit")).click();

        //6.Проверем аллерт
        String alert = switchTo().alert().getText();
        assert alert.contains("Successfully deposited");
        switchTo().alert().accept();

        //7. Проверяем ui
        $(byText("\uD83D\uDCB0 Deposit Money")).click();

        $$(".account-selector option")
                .get(1)
                .shouldHave(text(amount));

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
        //2. Заходим в раздел "Депозит"
        $(byText("\uD83D\uDCB0 Deposit Money")).click();

        //3.Выбираем в дропдане созданный аккаунт
        $(".account-selector").selectOption(1);

        //4.Вводим сумму в инпут
        $(byAttribute("placeholder", "Enter amount")).setValue(amount);

        //5.Нажимаем на кнопку "депозит"
        $(byText("\uD83D\uDCB5 Deposit")).click();

        //6.Проверем аллерт
        String alert = switchTo().alert().getText();
        assert alert.contains("Please enter a valid amount.");
        switchTo().alert().accept();

        //7. Проверяем ui
        $(byText("\uD83D\uDCB0 Deposit Money")).click();

        $$(".account-selector option")
                .get(1)
                .shouldHave(text("0.00"));

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
        //2. Заходим в раздел "Депозит"
        $(byText("\uD83D\uDCB0 Deposit Money")).click();

        //3.Выбираем в дропдане созданный аккаунт
        $(".account-selector").selectOption(1);

        //4.Вводим сумму в инпут
        $(byAttribute("placeholder", "Enter amount")).setValue(amount);

        //5.Нажимаем на кнопку "депозит"
        $(byText("\uD83D\uDCB5 Deposit")).click();

        //6.Проверем аллерт
        String alert = switchTo().alert().getText();
        assert alert.contains("Please deposit less or equal to 5000$.");
        switchTo().alert().accept();

        //7. Проверяем ui
        $(byText("\uD83D\uDCB0 Deposit Money")).click();

        $$(".account-selector option")
                .get(1)
                .shouldHave(text("0.00"));

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
        //2. Заходим в раздел "Депозит"
        $(byText("\uD83D\uDCB0 Deposit Money")).click();

        //3. Вводим сумму в инпут 
        $(byAttribute("placeholder", "Enter amount")).setValue("1000");

        //4. Нажимаем на кнопку "депозит"
        $(byText("\uD83D\uDCB5 Deposit")).click();

        //5. Проверяем аллерт
        String alert = switchTo().alert().getText();
        assert alert.contains("Please select an account.");
        switchTo().alert().accept();

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
        //2. Заходим в раздел "Депозит"
        $(byText("\uD83D\uDCB0 Deposit Money")).click();

        //3. Выбираем в дропдане созданный аккаунт
        $(".account-selector").selectOption(1);

        //4. Вводим первую сумму и делаем депозит
        $(byAttribute("placeholder", "Enter amount")).setValue("1000");
        $(byText("\uD83D\uDCB5 Deposit")).click();

        //5. Проверяем аллерт первого депозита
        String alert1 = switchTo().alert().getText();
        assert alert1.contains("Successfully deposited");
        switchTo().alert().accept();

        //6. Заходим снова в раздел "Депозит"
        $(byText("\uD83D\uDCB0 Deposit Money")).click();

        //7. Выбираем тот же аккаунт и делаем второй депозит
        $(".account-selector").selectOption(1);
        $(byAttribute("placeholder", "Enter amount")).setValue("2000");
        $(byText("\uD83D\uDCB5 Deposit")).click();

        //8. Проверяем аллерт второго депозита
        String alert2 = switchTo().alert().getText();
        assert alert2.contains("Successfully deposited");
        switchTo().alert().accept();

        //9. Проверяем ui — баланс должен быть 3000
        $(byText("\uD83D\uDCB0 Deposit Money")).click();

        $$(".account-selector option")
                .get(1)
                .shouldHave(text("3000"));

        //10. Проверяем api — 2 транзакции на сумму 3000
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse.getId());

        ModelAssertions.assertTransactions(transactions, 2, 3000.0);
    }

}