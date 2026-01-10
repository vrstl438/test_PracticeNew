package iteration_2;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.Matchers.*;
import static io.restassured.RestAssured.given;

public class Deposit {

    @DisplayName("Депозит с валидными значениями")
    @ParameterizedTest
    @CsvSource({
            "5000",
            "4999",
            "1"
    })
    public void createDepositHappyPath(int balance) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic aWx5YTIwMDAwOiF1c2VSNzExMQ==")
                .body("""
                        {
                        "id": 1,
                        "balance": %d
                        }
                        """.formatted(balance))

                .when()
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("accountNumber", equalTo("ACC1"))
                .body("balance", notNullValue())
                .body("transactions", notNullValue());
    }


    @DisplayName("Депозит со значением превышающим лимит")
    @Test
    public void depositExceedingLimitReturnsError() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic aWx5YTIwMDAwOiF1c2VSNzExMQ==")
                .body("""
                        {
                        "id": 1,
                        "balance": 5001
                        }
                        """)

                .when()
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(400)
                .body(containsString("Deposit amount cannot exceed 5000"));
    }


    @DisplayName("Депозит с невалидным значением баланса <0")
    @ParameterizedTest
    @CsvSource({
            "-1",
            "0"
    })
    public void depositWithNegativeBalanceReturnsError(int balance) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic aWx5YTIwMDAwOiF1c2VSNzExMQ==")
                .body("""
                        {
                        "id": 1,
                        "balance": %d
                        }
                        """.formatted(balance))

                .when()
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(400)
                .body(containsString("Deposit amount must be at least 0.01"));
    }


    @DisplayName("Депозит на несуществующий аккаунт")
    @Test
    public void depositToNonExistentAccountReturnsError() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic aWx5YTIwMDAwOiF1c2VSNzExMQ==")
                .body("""
                        {
                        "id": 19,
                        "balance": 1000
                        }
                        """)
                .when()
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(404);
    }
}