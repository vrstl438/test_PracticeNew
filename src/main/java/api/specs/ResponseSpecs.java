package api.specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;

public class ResponseSpecs {

    public static final String DEPOSIT_MIN_AMOUNT = "Deposit amount must be at least 0.01";
    public static final String DEPOSIT_MAX_AMOUNT = "Deposit amount cannot exceed 5000";
    public static final String TRANSFER_MIN_AMOUNT = "Transfer amount must be at least 0.01";
    public static final String TRANSFER_MAX_AMOUNT = "Transfer amount cannot exceed 10000";
    public static final String INVALID_NAME_FORMAT = "Name must contain two words with letters only";

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
        return expectedStatusCode(expectedStatusCode, true);
    }

    public static ResponseSpecification expectedStatusCode(int expectedStatusCode, boolean expectJson) {
        if (expectedStatusCode >= 200 && expectedStatusCode < 300) {
            return successStatusCode(expectedStatusCode, expectJson);
        } else if (expectedStatusCode >= 400) {
            return errorStatusCode(expectedStatusCode);
        }
        throw new IllegalArgumentException("not correct status code: " + expectedStatusCode);
    }

    public static ResponseSpecification okText() {
        return expectedStatusCode(200, false);
    }

    //оставил метод для того, если надо быстро что-то добавить, и это применится абсолютно ко всем тестам
    private static ResponseSpecBuilder defaultSpec() {
        return new ResponseSpecBuilder()
                .log(LogDetail.ALL);
    }

    private static ResponseSpecification successStatusCode(int expectedSuccessStatusCode) {
        return successStatusCode(expectedSuccessStatusCode, true);
    }

    private static ResponseSpecification successStatusCode(int expectedSuccessStatusCode, boolean expectJson) {
        ResponseSpecBuilder builder = defaultSpec().expectStatusCode(expectedSuccessStatusCode);
        if (expectJson) {
            builder.expectContentType(ContentType.JSON);
        }
        return builder.build();
    }

    private static ResponseSpecification errorStatusCode(int expectedErrorStatusCode){
        return defaultSpec()
                .expectStatusCode(expectedErrorStatusCode)
                .build();
    }
}