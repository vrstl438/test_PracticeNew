package tests.iteration_2.ui.transferTests;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import context.ScenarioContext;
import domain.builders.CreateDepositRequestBuilder;
import domain.builders.CreateUserRequestBuilder;
import domain.model.comparison.ModelAssertions;
import domain.model.requests.DepositRequest;
import domain.model.requests.UserRequest;
import domain.model.response.AccountResponse;
import domain.model.response.AccountResponse.Transaction;
import domain.model.response.DepositResponse;
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

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Selenide.switchTo;
import static utils.TestUtils.repeat;

public class TransferYourAccountsUITests {

    private static final int INITIAL_DEPOSIT_COUNT = 3;
    private static final double DEPOSIT_AMOUNT = 5000.0;
    private static final double INITIAL_BALANCE = INITIAL_DEPOSIT_COUNT * DEPOSIT_AMOUNT;

    private ScenarioContext context = new ScenarioContext();
    private AccountResponse accountResponse;
    private AccountResponse accountResponse1;

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
        //создание модельки юзера
        UserRequest userRequest = new CreateUserRequestBuilder().userBuild();
        //отправка модельки юзера на сервер и сохранение токена в контекст
        Response createUserResponse = new CrudRequester(
                RequestSpecs.adminAuthSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.created()
        ).post(userRequest).extract().response();
        context.setUserTokenFromResponse(createUserResponse);

        //создание аккаунтов
        ValidatedCrudRequester<AccountResponse> accountRequester = new ValidatedCrudRequester<>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.created()
        );
        accountResponse = accountRequester.post();
        accountResponse1 = accountRequester.post();

        //начальные депозиты на первый аккаунт
        DepositRequest depositRequest = new CreateDepositRequestBuilder()
                .withId(accountResponse.getId())
                .withBalance(DEPOSIT_AMOUNT)
                .depositBuild();
        ValidatedCrudRequester<DepositResponse> depositRequester = new ValidatedCrudRequester<>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.DEPOSIT,
                ResponseSpecs.ok()
        );
        repeat(INITIAL_DEPOSIT_COUNT, () -> depositRequester.post(depositRequest));

        //захожу под созданным пользоватлем
        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", context.getUserToken());
        Selenide.open("/dashboard");
    }

    @DisplayName("Перевод между своими счетами, валидная сумма")
    @ParameterizedTest @ValueSource(strings = {"1000", "1"})
    void validTransferBetweenYourAccounts(String amount) {
        //2. Заходим в раздел "Трансфер"
        $(byText("\uD83D\uDD04 Make a Transfer")).click();

        //3. Ожидаем загрузки аккаунтов и выбираем отправителя по номеру аккаунта
        $$(".account-selector option").shouldHave(sizeGreaterThan(1));
        $(".account-selector").selectOptionContainingText(accountResponse.getAccountNumber());

        //4. Вводим номер аккаунта получателя
        $(byAttribute("placeholder", "Enter recipient account number")).setValue(accountResponse1.getAccountNumber());

        //5. Вводим сумму трансфера
        $(byAttribute("placeholder", "Enter amount")).setValue(amount);

        //6. Ставим чекбокс подтверждения
        $("#confirmCheck").click();

        //7. Нажимаем на кнопку "Send Transfer"
        $(byText("\uD83D\uDE80 Send Transfer")).click();

        //8. Проверяем аллерт
        String alert = switchTo().alert().getText();
        assert alert.contains("Successfully transferred");
        switchTo().alert().accept();

        //9. Проверяем api — транзакции на отправителе
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT + 1, INITIAL_BALANCE + Double.parseDouble(amount));

        //10. Проверяем api — транзакции на получателе
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse1.getId());
        ModelAssertions.assertTransactions(receiverTransactions, 1, Double.parseDouble(amount));
    }

    @DisplayName("Перевод между своими счетами без подтверждения чекбоксом")
    @Test
    void transferWithoutConfirmation() {
        //2. Заходим в раздел "Трансфер"
        $(byText("\uD83D\uDD04 Make a Transfer")).click();

        //3. Ожидаем загрузки аккаунтов и выбираем отправителя по номеру аккаунта
        $$(".account-selector option").shouldHave(sizeGreaterThan(1));
        $(".account-selector").selectOptionContainingText(accountResponse.getAccountNumber());

        //4. Вводим номер аккаунта получателя
        $(byAttribute("placeholder", "Enter recipient account number")).setValue(accountResponse1.getAccountNumber());

        //5. Вводим сумму трансфера
        $(byAttribute("placeholder", "Enter amount")).setValue("1000");

        //6. Не ставим чекбокс подтверждения

        //7. Нажимаем на кнопку "Send Transfer"
        $(byText("\uD83D\uDE80 Send Transfer")).click();

        //8. Проверяем аллерт
        String alert = switchTo().alert().getText();
        assert alert.contains("Please fill all fields and confirm.");
        switchTo().alert().accept();

        //9. Проверяем api что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);

        //10. Получатель пустой
        List<Transaction> receiverTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse1.getId());
        ModelAssertions.assertTransactions(receiverTransactions, 0, 0.0);
    }

    @DisplayName("Перевод между своими счетами без заполнения полей")
    @Test
    void transferWithoutFillingFields() {
        //2. Заходим в раздел "Трансфер"
        $(byText("\uD83D\uDD04 Make a Transfer")).click();

        //3. Ставим чекбокс подтверждения но ничего не заполняем
        $("#confirmCheck").click();

        //4. Нажимаем на кнопку "Send Transfer"
        $(byText("\uD83D\uDE80 Send Transfer")).click();

        //5. Проверяем аллерт
        String alert = switchTo().alert().getText();
        assert alert.contains("Please fill all fields and confirm.");
        switchTo().alert().accept();

        //6. Проверяем api что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);
    }

    @DisplayName("Перевод между своими счетами с суммой меньше минимальной")
    @ParameterizedTest @ValueSource(strings = {"-1", "0", "0.001"})
    void transferBelowMinAmount(String amount) {
        //2. Заходим в раздел "Трансфер"
        $(byText("\uD83D\uDD04 Make a Transfer")).click();

        //3. Ожидаем загрузки аккаунтов и выбираем отправителя по номеру аккаунта
        $$(".account-selector option").shouldHave(sizeGreaterThan(1));
        $(".account-selector").selectOptionContainingText(accountResponse.getAccountNumber());

        //4. Вводим номер аккаунта получателя
        $(byAttribute("placeholder", "Enter recipient account number")).setValue(accountResponse1.getAccountNumber());

        //5. Вводим невалидную сумму (меньше минимума)
        $(byAttribute("placeholder", "Enter amount")).setValue(amount);

        //6. Ставим чекбокс подтверждения
        $("#confirmCheck").click();

        //7. Нажимаем на кнопку "Send Transfer"
        $(byText("\uD83D\uDE80 Send Transfer")).click();

        //8. Проверяем аллерт
        String alert = switchTo().alert().getText();
        assert alert.contains("Transfer amount must be at least 0.01");
        switchTo().alert().accept();

        //9. Проверяем api что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);
    }

    @DisplayName("Перевод между своими счетами с суммой больше максимальной")
    @ParameterizedTest @ValueSource(strings = {"10001", "50000"})
    void transferAboveMaxAmount(String amount) {
        //2. Заходим в раздел "Трансфер"
        $(byText("\uD83D\uDD04 Make a Transfer")).click();

        //3. Ожидаем загрузки аккаунтов и выбираем отправителя по номеру аккаунта
        $$(".account-selector option").shouldHave(sizeGreaterThan(1));
        $(".account-selector").selectOptionContainingText(accountResponse.getAccountNumber());

        //4. Вводим номер аккаунта получателя
        $(byAttribute("placeholder", "Enter recipient account number")).setValue(accountResponse1.getAccountNumber());

        //5. Вводим сумму больше максимальной
        $(byAttribute("placeholder", "Enter amount")).setValue(amount);

        //6. Ставим чекбокс подтверждения
        $("#confirmCheck").click();

        //7. Нажимаем на кнопку "Send Transfer"
        $(byText("\uD83D\uDE80 Send Transfer")).click();

        //8. Проверяем аллерт
        String alert = switchTo().alert().getText();
        assert alert.contains("Transfer amount cannot exceed 10000");
        switchTo().alert().accept();

        //9. Проверяем api что трансфер не создался
        List<Transaction> senderTransactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(accountResponse.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);
    }
}
