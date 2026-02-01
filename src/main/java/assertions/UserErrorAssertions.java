package assertions;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import static org.assertj.core.api.Assertions.assertThat;

public class UserErrorAssertions {

    public static void assertPlainErrorMessage(Response response, String expectedMessage){
        //проверка на то что content type text/plain, а не json
        assertThat(response.getContentType()).contains(ContentType.TEXT.toString());
        //сравнение ожидаемого сообщения с фактическим
        assertThat(response.asString().trim()).isEqualTo(expectedMessage);
    }
}
