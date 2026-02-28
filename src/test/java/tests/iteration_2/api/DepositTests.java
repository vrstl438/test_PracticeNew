package tests.iteration_2.api;

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

import java.util.List;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import api.skelethon.Endpoint;
import api.skelethon.requesters.CrudRequester;
import api.skelethon.requesters.ValidatedCrudRequester;

import static common.datakeys.Keys.USER_TOKEN;
import static common.datakeys.ResponseKey.FIRST_CREATE_ACC;

public class DepositTests {
    private ScenarioContext context = new ScenarioContext();

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
        context.saveData(USER_TOKEN, createUserResponse.getHeader(RequestSpecs.AUTHORIZATION_HEADER));

        //создаем аккаунт от имени только что созданного юзера
        AccountResponse accountResponse = new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.ACCOUNTS,
                ResponseSpecs.created()
        ).post();
        context.saveData(FIRST_CREATE_ACC, accountResponse);
    }



    @DisplayName("Депозит с валидной суммой на существующий аккаунт")
    @ParameterizedTest @ValueSource(doubles = {4999, 5000, 0.1, 1})
    void validDeposit(double amountDeposit) {
        //создаем депозит модельку
        DepositRequest depositRequest = new CreateDepositRequestBuilder().withId(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId()).withBalance(amountDeposit).depositBuild();
        //отправляем наш созданный депозит
        DepositResponse depositResponse = new ValidatedCrudRequester<DepositResponse>(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.DEPOSIT,
                ResponseSpecs.ok()
        ).post(depositRequest);
        //проверка депозита
        ModelAssertions.assertDepositCreated(depositResponse, depositRequest);

        //проверка транзакций через гет
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId());

        ModelAssertions.assertTransactions(transactions, 1, amountDeposit);
    }


    @DisplayName("Попытка депозита с отрицательной и нулевой суммой")
    @ParameterizedTest @ValueSource(doubles = {-1.0 , 0.0 , 0.001})
    void invalidNegativeAndNullDeposit(double amountDeposit){
        //создание модельки невалидного депозита
        DepositRequest depositRequest = new CreateDepositRequestBuilder()
                .withId(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId())
                .withBalance(amountDeposit)
                .depositBuild();
        //отправка депозита
        Response response = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.DEPOSIT,
                ResponseSpecs.badRequest()
        ).post(depositRequest).extract().response();
        ModelAssertions.assertPlainErrorMessage(response, ResponseSpecs.DEPOSIT_MIN_AMOUNT);

        //проверка что транзакция не создалась
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId());

        ModelAssertions.assertTransactions(transactions, 0, 0.0);
    }

    @DisplayName("Попытка депозита с суммой превышающей допустимую сумму")
    @ParameterizedTest @ValueSource(doubles = {5000.001, 5000.1, 5001})
    void invalidExceedDeposit(double amountDeposit){
        DepositRequest depositRequest = new CreateDepositRequestBuilder()
                .withId(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId())
                .withBalance(amountDeposit)
                .depositBuild();

        Response response = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.DEPOSIT,
                ResponseSpecs.badRequest()
        ).post(depositRequest).extract().response();
        ModelAssertions.assertPlainErrorMessage(response, ResponseSpecs.DEPOSIT_MAX_AMOUNT);

        //проверка что транзакция не создалась
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId());

        ModelAssertions.assertTransactions(transactions, 0, 0.0);
    }

    @DisplayName("Проверка расчета баланса, при сразу нескольких депозитах")
    @Test
    void moreDepositsAtAccountBalance(){
        //создание первого депозита
        DepositRequest depositRequest1 = new CreateDepositRequestBuilder()
                .withId(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId())
                .depositBuild();
        //создание второго депозита
        DepositRequest depositRequest2 = new CreateDepositRequestBuilder()
                .withId(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId())
                .depositBuild();

        Double totalAmountBalance = depositRequest1.getBalance() + depositRequest2.getBalance();

        ValidatedCrudRequester<DepositResponse> depositRequester = new ValidatedCrudRequester<>(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.DEPOSIT,
                ResponseSpecs.ok()
        );
        depositRequester.post(depositRequest1);
        DepositResponse lastDepositResponse = depositRequester.post(depositRequest2);

        //проверка баланса
        assert totalAmountBalance.equals(lastDepositResponse.getBalance());

        //проверка что транзакции зачислены через гет
        List<Transaction> transactions = new CrudRequester(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.TRANSACTIONS_INFO,
                ResponseSpecs.ok()
        ).getList(context.getData(FIRST_CREATE_ACC, AccountResponse.class).getId());

        ModelAssertions.assertTransactions(transactions, 2, totalAmountBalance);
    }
}
