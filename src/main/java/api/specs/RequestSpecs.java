package api.specs;

import api.config.TestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.preemptive;

public class RequestSpecs {
//добавить базовую спецификацию
    public static RequestSpecification adminAuthSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.baseUrl())
                .setContentType(ContentType.JSON)
                .setAuth(preemptive().basic("admin", "admin"))
                .log(LogDetail.ALL)
                .build();
    }

    public static RequestSpecification userAuthSpec(String username, String password) {
        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.baseUrl())
                .setContentType(ContentType.JSON)
                .setAuth(preemptive().basic(username, password))
                .log(LogDetail.ALL)
                .build();
    }

    public static RequestSpecification userAuthSpec(String token) {
        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.baseUrl())
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", token)
                .log(LogDetail.ALL)
                .build();
    }
}