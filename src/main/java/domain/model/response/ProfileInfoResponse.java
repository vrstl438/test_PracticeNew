package domain.model.response;

import domain.model.ResponseModel;
import domain.model.Role;
import lombok.Getter;

import java.util.List;

@Getter
public class ProfileInfoResponse implements ResponseModel {
    private Long id;
    private String username;
    private String password;
    private String name;
    private Role role;
    private List<AccountResponse> accounts;
}