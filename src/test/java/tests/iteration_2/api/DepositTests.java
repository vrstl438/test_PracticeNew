package tests.iteration_2.api;

import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import assertions.UserAssertions;
import assertions.UserErrorAssertions;
import context.ScenarioContext;
import domain.builders.CreateDepositRequestBuilder;
import domain.builders.CreateUserRequestBuilder;
import domain.model.requests.DepositRequest;
import domain.model.requests.UserRequest;
import domain.model.response.AccountResponse;
import domain.model.response.AccountResponse.Transaction;
import domain.model.response.DepositResponse;

import java.util.List;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;

public class DepositTests {
    private ScenarioContext context = new ScenarioContext();
    private AccountResponse accountResponse;

    @BeforeEach
    void setUp(){
        //создание модельки пользователя
        UserRequest userRequest = new CreateUserRequestBuilder().userBuild();
        //отправка запроса на создание юзера и сохранение токена в контекст
        Response createUserResponse = new CrudRequester(
                RequestSpecs.adminAuthSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.created()
        ).post(userRequest).extract().response();
        context.setUserToken(createUserResponse);

        //создаем аккаунт от имени только что созданного юзера
        accountResponse = new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.created()
        ).post();
    }



    @DisplayName("Депозит с валидной суммой на существующий аккаунт")
    @ParameterizedTest @ValueSource(doubles = {4999, 5000, 0.1, 1})
    void validDeposit(double amountDeposit) {
        //создаем депозит модельку
        DepositRequest depositRequest = new CreateDepositRequestBuilder().withId(accountResponse.getId()).withBalance(amountDeposit).depositBuild();
        //отправляем наш созданный депозит
        DepositResponse depositResponse = new ValidatedCrudRequester<DepositResponse>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.DEPOSIT,
                ResponseSpecs.ok()
        ).post(depositRequest);
        //проверка депозита
        UserAssertions.assertDepositCreated(depositResponse, depositRequest);

        //проверка транзакций через гет
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(accountResponse.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);

        UserAssertions.assertTransactions(transactions, 1, amountDeposit);
    }


    @DisplayName("Попытка депозита с отрицательной и нулевой суммой")
    @ParameterizedTest @ValueSource(doubles = {-1.0 , 0.0 , 0.001})
    void invalidNegativeAndNullDeposit(double amountDeposit){
        //создание модельки невалидного депозита
        DepositRequest depositRequest = new CreateDepositRequestBuilder()
                .withId(accountResponse.getId())
                .withBalance(amountDeposit)
                .depositBuild();
        //отправка депозита
        Response response = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.DEPOSIT,
                ResponseSpecs.badRequest()
        ).post(depositRequest).extract().response();
        UserErrorAssertions.assertPlainErrorMessage(response, "Deposit amount must be at least 0.01");

        //проверка что транзакция не создалась
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(accountResponse.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);

        UserAssertions.assertTransactions(transactions, 0, 0.0);
    }

    @DisplayName("Попытка депозита с суммой превышающей допустимую сумму")
    @ParameterizedTest @ValueSource(doubles = {5000.001, 5000.1, 5001})
    void invalidExceedDeposit(double amountDeposit){
        DepositRequest depositRequest = new CreateDepositRequestBuilder()
                .withId(accountResponse.getId())
                .withBalance(amountDeposit)
                .depositBuild();

        Response response = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.DEPOSIT,
                ResponseSpecs.badRequest()
        ).post(depositRequest).extract().response();
        UserErrorAssertions.assertPlainErrorMessage(response, "Deposit amount cannot exceed 5000");

        //проверка что транзакция не создалась
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(accountResponse.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);

        UserAssertions.assertTransactions(transactions, 0, 0.0);
    }

    @DisplayName("Проверка расчета баланса, при сразу нескольких депозитах")
    @Test
    void moreDepositsAtAccountBalance(){
        //создание первого депозита
        DepositRequest depositRequest1 = new CreateDepositRequestBuilder()
                .withId(accountResponse.getId())
                .depositBuild();
        //создание второго депозита
        DepositRequest depositRequest2 = new CreateDepositRequestBuilder()
                .withId(accountResponse.getId())
                .depositBuild();

        Double totalAmountBalance = depositRequest1.getBalance() + depositRequest2.getBalance();

        ValidatedCrudRequester<DepositResponse> depositRequester = new ValidatedCrudRequester<>(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.DEPOSIT,
                ResponseSpecs.ok()
        );
        depositRequester.post(depositRequest1);
        DepositResponse lastDepositResponse = depositRequester.post(depositRequest2);

        //проверка баланса
        assert totalAmountBalance.equals(lastDepositResponse.getBalance());

        //проверка что транзакции зачислены через гет
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getUserToken()),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).get(accountResponse.getId() + "/transactions").extract().jsonPath().getList("", Transaction.class);

        UserAssertions.assertTransactions(transactions, 2, totalAmountBalance);
    }
}