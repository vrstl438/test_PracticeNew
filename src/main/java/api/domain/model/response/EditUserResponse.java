package api.domain.model.response;

import api.domain.model.ResponseModel;
import api.domain.model.Role;
import lombok.Data;

import java.util.List;

@Data
public class EditUserResponse implements ResponseModel {
    private CustomerResponse customer;
    private String message;

    @Data
    public static class CustomerResponse{
        private Long id;
        private String username;
        private String password;
        private String name;
        private Role role;
        private List<AccountResponse> accounts;
    }
}