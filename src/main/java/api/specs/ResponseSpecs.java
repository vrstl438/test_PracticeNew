package api.specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;

public class ResponseSpecs {

    public static ResponseSpecification ok() {
        return expectedStatusCode(200);
    }

    public static ResponseSpecification created() {
        return expectedStatusCode(201);
    }

    public static ResponseSpecification badRequest() {
        return expectedStatusCode(400);
    }

    public static ResponseSpecification expectedStatusCode(int expectedStatusCode){
        if (expectedStatusCode >= 200 && expectedStatusCode < 300){
            return successStatusCode(expectedStatusCode);
        } else if (expectedStatusCode >= 400) {
            return errorStatusCode(expectedStatusCode);
        }
        throw new IllegalArgumentException("not correct status code: " + expectedStatusCode);
    }

    //оставил метод для того, если надо быстро что-то добавить, и это применится абсолютно ко всем тестам
    private static ResponseSpecBuilder defaultSpec() {
        return new ResponseSpecBuilder()
                .log(LogDetail.ALL);
    }

    private static ResponseSpecification successStatusCode(int expectedSuccessStatusCode){
        return defaultSpec()
                .expectContentType(ContentType.JSON)
                .expectStatusCode(expectedSuccessStatusCode)
                .build();
    }

    private static ResponseSpecification errorStatusCode(int expectedErrorStatusCode){
        return defaultSpec()
                .expectStatusCode(expectedErrorStatusCode)
                .build();
    }
}