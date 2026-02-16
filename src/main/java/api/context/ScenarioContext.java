package api.context;

import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScenarioContext {
    public static final String AUTH_HEADER = "Authorization";

    private String userToken;

    public void setUserTokenFromResponse(Response response) {
        this.userToken = response.getHeader(AUTH_HEADER);
    }

    public void clear() {
        this.userToken = null;
    }
}
