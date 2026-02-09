package domain.model.requests;

import domain.model.RequestModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginRequest implements RequestModel {
    private final String username;
    private final String password;
}
