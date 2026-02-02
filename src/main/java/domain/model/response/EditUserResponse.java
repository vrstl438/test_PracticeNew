package domain.model.response;

import domain.model.ResponseModel;
import domain.model.Role;
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