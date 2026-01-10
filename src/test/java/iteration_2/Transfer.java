package iteration_2;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

public class Transfer {

    @DisplayName("Перевод денег с одного аккаунта на другой валидные значения")
    @ParameterizedTest
    @CsvSource({
            "9999",
            "10000",
            "1"
    })
    public void transferBetweenAccountsWithValidAmount(int amount) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic aWx5YTIwMDAwOiF1c2VSNzExMQ==")
                .body("""
                        {
                          "senderAccountId": 1,
                          "receiverAccountId": 2,
                          "amount": %d
                        }
                        """.formatted(amount))
                .when()
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .statusCode(200);
    }


    @DisplayName("Перевод с невалидным значением суммы <0")
    @ParameterizedTest
    @CsvSource({
            "-1",
            "0"
    })
    public void transferWithNegativeAmountReturnsError(int amount) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic aWx5YTIwMDAwOiF1c2VSNzExMQ==")
                .body("""
                        {
                                     "senderAccountId": 1,
                                     "receiverAccountId": 2,
                                     "amount": %d
                                   }
                        """.formatted(amount))
                .when()
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .statusCode(400)
                .body(containsString("Transfer amount must be at least 0.01"));
    }



    @DisplayName("Перевод с невалидным значением превышающий лимит")
    @Test
    public void transferExceedingLimitReturnsError() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic aWx5YTIwMDAwOiF1c2VSNzExMQ==")
                .body("""
                        {
                                     "senderAccountId": 1,
                                     "receiverAccountId": 2,
                                     "amount": 10001
                                   }
                        """)
                .when()
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .statusCode(400)
                .body(containsString("Transfer amount cannot exceed 10000"));
    }
}