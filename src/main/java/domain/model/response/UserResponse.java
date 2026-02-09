package domain.model.response;

import domain.model.ResponseModel;
import domain.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse implements ResponseModel {
    private Long id;
    private String username;
    private String password;
    private String name;
    private Role role;
    private List<String> accounts;
}
