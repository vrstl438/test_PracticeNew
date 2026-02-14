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

public class TransferOtherAccountsUITests {

    private static final int INITIAL_DEPOSIT_COUNT = 3;
    private static final double DEPOSIT_AMOUNT = 5000.0;
    private static final double INITIAL_BALANCE = INITIAL_DEPOSIT_COUNT * DEPOSIT_AMOUNT;

    private ScenarioContext senderContext = new ScenarioContext();
    private ScenarioContext receiverContext = new ScenarioContext();
    private AccountResponse senderAccount;
    private AccountResponse receiverAccount;

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
        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", senderContext.getUserToken());
        Selenide.open("/dashboard");
    }

    @DisplayName("Перевод на чужой аккаунт (валидная сумма)")
    @ParameterizedTest @ValueSource(strings = {"1000", "1"})
    void validTransferToOtherAccount(String amount) {
        //2. Заходим в раздел "Трансфер"
        $(byText("\uD83D\uDD04 Make a Transfer")).click();

        //3. Ожидаем загрузки аккаунтов в дропдаун и выбираем отправителя
        $$(".account-selector option").shouldHave(sizeGreaterThan(1));
        $(".account-selector").selectOption(1);

        //4. Вводим номер аккаунта получателя
        $(byAttribute("placeholder", "Enter recipient account number")).setValue(receiverAccount.getAccountNumber());

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
        //2. Заходим в раздел "Трансфер"
        $(byText("\uD83D\uDD04 Make a Transfer")).click();

        //3. Ожидаем загрузки аккаунтов и выбираем отправителя
        $$(".account-selector option").shouldHave(sizeGreaterThan(1));
        $(".account-selector").selectOption(1);

        //4. Вводим номер аккаунта получателя
        $(byAttribute("placeholder", "Enter recipient account number")).setValue(receiverAccount.getAccountNumber());

        //5. Вводим сумму трансфера
        $(byAttribute("placeholder", "Enter amount")).setValue("1000");

        //6. НЕ ставим чекбокс подтверждения

        //7. Нажимаем на кнопку "Send Transfer"
        $(byText("\uD83D\uDE80 Send Transfer")).click();

        //8. Проверяем аллерт
        String alert = switchTo().alert().getText();
        assert alert.contains("Please fill all fields and confirm.");
        switchTo().alert().accept();

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
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(senderAccount.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);
    }

    @DisplayName("Перевод на чужой аккаунт с суммой меньше минимальной")
    @ParameterizedTest @ValueSource(strings = {"-1", "0", "0.001"})
    void transferBelowMinAmount(String amount) {
        //2. Заходим в раздел "Трансфер"
        $(byText("\uD83D\uDD04 Make a Transfer")).click();

        //3. Ожидаем загрузки аккаунтов и выбираем отправителя
        $$(".account-selector option").shouldHave(sizeGreaterThan(1));
        $(".account-selector").selectOption(1);

        //4. Вводим номер аккаунта получателя
        $(byAttribute("placeholder", "Enter recipient account number")).setValue(receiverAccount.getAccountNumber());

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
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(senderAccount.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);
    }

    @DisplayName("Перевод на чужой аккаунт с суммой больше максимальной")
    @ParameterizedTest @ValueSource(strings = {"10001", "50000"})
    void transferAboveMaxAmount(String amount) {
        //2. Заходим в раздел "Трансфер"
        $(byText("\uD83D\uDD04 Make a Transfer")).click();

        //3. Ожидаем загрузки аккаунтов и выбираем отправителя
        $$(".account-selector option").shouldHave(sizeGreaterThan(1));
        $(".account-selector").selectOption(1);

        //4. Вводим номер аккаунта получателя
        $(byAttribute("placeholder", "Enter recipient account number")).setValue(receiverAccount.getAccountNumber());

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
                RequestSpecs.userAuthSpec(senderContext.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(senderAccount.getId());
        ModelAssertions.assertTransactions(senderTransactions, INITIAL_DEPOSIT_COUNT, INITIAL_BALANCE);
    }
}
