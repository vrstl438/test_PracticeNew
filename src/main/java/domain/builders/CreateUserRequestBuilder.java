package domain.builders;

import domain.generators.PasswordGenerator;
import domain.generators.UsernameGenerator;
import domain.model.Role;
import domain.model.requests.UserRequest;

public class CreateUserRequestBuilder {
    private String username = UsernameGenerator.validUsername();
    private String password = PasswordGenerator.validPassword();
    private Role role = Role.USER;

    public CreateUserRequestBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public CreateUserRequestBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public CreateUserRequestBuilder withRole(Role role) {
        this.role = role;
        return this;
    }

    public UserRequest userBuild() {
        return new UserRequest(username, password, role);
    }
}
