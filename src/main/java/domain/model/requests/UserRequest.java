package domain.model.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import domain.model.Role;

@Getter
@AllArgsConstructor
public class UserRequest {
    private final String username;
    private final String password;
    private final Role role;
}