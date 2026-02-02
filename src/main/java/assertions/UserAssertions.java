package assertions;

import domain.model.Role;
import domain.model.requests.DepositRequest;
import domain.model.requests.EditNameRequest;
import domain.model.requests.TransferRequest;
import domain.model.requests.UserRequest;
import domain.model.response.*;
import domain.model.response.AccountResponse.Transaction;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UserAssertions {

    public static void assertUserCreated(UserResponse response, UserRequest request) {
        assertThat(response.getId() > 0);
        assertThat(response.getUsername()).isEqualTo(request.getUsername());
        assertThat(response.getRole()).isEqualTo(request.getRole());
        assertThat(response.getPassword()).isNotEqualTo(request.getPassword());
        assertThat(response.getName()).isNull();
    }

    public static void assertAccountCreated(AccountResponse response) {
        assertThat(response.getId()).isNotNull().isPositive();
        assertThat(response.getAccountNumber()).isNotNull().hasSizeGreaterThanOrEqualTo(3);
        assertThat(response.getBalance()).isNotNull().isZero();
        assertThat(response.getTransactions()).isNotNull().isEmpty();
    }

    public static void assertDepositCreated(DepositResponse response, DepositRequest request) {
        assertThat(response.getId()).isEqualTo(request.getId());
        assertThat(response.getAccountNumber()).isNotNull().hasSizeGreaterThanOrEqualTo(3);
        assertThat(response.getBalance()).isEqualTo(request.getBalance());

        //транзакции
        assertThat(response.getTransactions()).isNotNull().isNotEmpty();

        Transaction transaction = response.getTransactions().get(0);
        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getAmount()).isEqualTo(request.getBalance());
        assertThat(transaction.getType()).isEqualTo("DEPOSIT");
        assertThat(transaction.getTimestamp()).isNotNull();
        assertThat(transaction.getRelatedAccountId()).isEqualTo(response.getId());
    }

    public static void assertTransferCreated(TransferResponse response, TransferRequest request) {
        assertThat(response.getMessage()).isEqualTo("Transfer successful");
        assertThat(response.getAmount()).isNotNull().isPositive();
        assertThat(response.getAmount()).isEqualTo(request.getAmount());
        assertThat(response.getReceiverAccountId()).isEqualTo(request.getReceiverAccountId());
        assertThat(response.getReceiverAccountId()).isNotNull().isPositive();
        assertThat(response.getSenderAccountId()).isEqualTo(request.getSenderAccountId());
        assertThat(response.getSenderAccountId()).isNotNull().isPositive();
    }

    public static void assertEditUserResponse(EditUserResponse response, EditNameRequest request) {
        assertThat(response.getMessage()).isEqualTo("Profile updated successfully");
        assertThat(response.getCustomer()).isNotNull();
        assertThat(response.getCustomer().getId()).isNotNull().isPositive();
        assertThat(response.getCustomer().getUsername()).isNotNull().isNotEmpty();
        assertThat(response.getCustomer().getName()).isEqualTo(request.getName());
        assertThat(response.getCustomer().getRole()).isNotNull().isEqualTo(Role.USER);
        assertThat(response.getCustomer().getAccounts()).isNotNull();
    }

    public static void assertTransactions(List<Transaction> transactions, int expectedSize, Double expectedTotalAmount) {
        assertThat(transactions).hasSize(expectedSize);

        Double totalAmount = transactions.stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
        assertThat(totalAmount).isCloseTo(expectedTotalAmount, org.assertj.core.data.Offset.offset(0.01));
    }
}