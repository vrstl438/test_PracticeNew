package common.extensions;

import api.domain.builders.CreateUserRequestBuilder;
import api.domain.model.requests.UserRequest;
import api.domain.model.response.AccountResponse;
import api.domain.model.response.UserResponse;
import api.skelethon.Endpoint;
import api.skelethon.requesters.CrudRequester;
import api.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.annotations.CreateUserAndAccount;
import common.context.ScenarioContext;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static common.datakeys.Keys.USER_TOKEN;
import static common.datakeys.ResponseKey.*;
import static io.opentelemetry.api.internal.ApiUsageLogger.log;

@Slf4j
public class CreateUserAndAccountExtensions implements BeforeEachCallback {
    ScenarioContext context = new ScenarioContext();

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {

        CreateUserAndAccount annotation = extensionContext.getRequiredTestMethod().getAnnotation(CreateUserAndAccount.class);

        if (annotation != null){

            UserRequest userRequest = new CreateUserRequestBuilder().userBuild();

            Response createUserResponse = new CrudRequester(
                    RequestSpecs.adminAuthSpec(),
                    Endpoint.ADMIN_USER,
                    ResponseSpecs.created()
            ).post(userRequest).extract().response();

            String token = createUserResponse.getHeader(RequestSpecs.AUTHORIZATION_HEADER);
            context.saveData(CREATED_USER, createUserResponse.as(UserResponse.class));
            context.saveData(USER_TOKEN, token);

            switch (annotation.accountCount()) {
                case 1 -> context.saveData(FIRST_CREATE_ACC, createAccountWithUser());
                case 2 -> {
                    context.saveData(FIRST_CREATE_ACC, createAccountWithUser());
                    context.saveData(SECOND_CREATE_ACC, createAccountWithUser());
                }
                default -> log("Пользователь создан без аккаунта");
            }
        }
    }

    private AccountResponse createAccountWithUser(){
        return new ValidatedCrudRequester<AccountResponse>(
                RequestSpecs.userAuthSpec(context.getData(USER_TOKEN, String.class)),
                Endpoint.ACCOUNTS,
                ResponseSpecs.created()
        ).post();
    }
}
