package api.domain.model.requests;

import api.domain.model.RequestModel;
import api.domain.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserRequest implements RequestModel {
    private final String username;
    private final String password;
    private final Role role;
}