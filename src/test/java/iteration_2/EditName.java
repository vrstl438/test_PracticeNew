package iteration_2;

import io.restassured.http.ContentType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class EditName {

    private static final Log log = LogFactory.getLog(EditName.class);

    @DisplayName("Смена имени на валидное")
    @ParameterizedTest
    @CsvSource({
            "Ilya Ilya"
    })
    public void changeProfileNameWithValidValue(String name) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic cGV0eWE6cGV0eWEhQTE=")
                .body("""
                        {
                          "name": "%s"
                        }
                        """.formatted(name))
                .when()
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .statusCode(200)
                .body("customer.name", equalTo(name));
    }

    @DisplayName("Смена имени некорректное кол-во слов")
    @ParameterizedTest
    @CsvSource({
            "Ilya",
            "Ilya Ilya Ilya",
            "Ilya  Ilya"
    })
    void changeProfileNameWithInvalidWordCount(String name) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic cGV0eWE6cGV0eWEhQTE=")
                .body("""
                        {
                          "name": "%s"
                        }
                        """.formatted(name))
                .when()
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .statusCode(400)
                .body(containsString("Name must contain two words with letters only"));
    }

    @DisplayName("Смена имени - невалидные значения")
    @ParameterizedTest
    @CsvSource({
            "Ilya1 Ilya",
            "Ilya Ilya1",
            "Ilya@ Ilya",
            "Ilya Ilya!",
            "Ily@q Il@ya",
            " Ilya Ilya",
            "Ilya Ilya ",
            "Ilya  Ilya"
    })
    void changeProfileNameWithInvalidCharacters(String name) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic cGV0eWE6cGV0eWEhQTE=")
                .body("""
                        {
                          "name": "%s"
                        }
                        """.formatted(name))
                .when()
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .statusCode(400);
    }
}