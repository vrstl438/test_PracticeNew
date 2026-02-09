package requests.skelethon;

import domain.model.RequestModel;
import domain.model.requests.*;
import domain.model.response.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Endpoint {
    ADMIN_USER(
            "/api/v1/admin/users",
            UserRequest.class,
            UserResponse.class
    ),

    LOGIN(
            "/api/v1/auth/login",
            LoginRequest.class,
            UserResponse.class
    ),

    ACCOUNTS(
            "/api/v1/accounts",
            null,
            AccountResponse.class
    ),

    DEPOSIT(
            "/api/v1/accounts/deposit",
            DepositRequest.class,
            DepositResponse.class
    ),

    TRANSFER(
            "/api/v1/accounts/transfer",
            TransferRequest.class,
            TransferResponse.class
    ),

    EDIT_PROFILE(
            "/api/v1/customer/profile",
            EditNameRequest.class,
            EditUserResponse.class
    ),

    PROFILE_INFO(
            "/api/v1/customer/profile",
            null,
            ProfileInfoResponse.class
    ),

    TRANSACTIONS_INFO(
            "/api/v1/accounts/%s/transactions",
            null,
            AccountResponse.Transaction[].class
    );

    private final String url;
    private final Class<? extends RequestModel> requestModel;
    private final Class<?> responseModel;

    public String formatUrl(Object... params) {
        return String.format(url, params);
    }
}
