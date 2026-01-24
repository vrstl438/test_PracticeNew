package domain.model.response;

import domain.model.response.AccountResponse.Transaction;
import lombok.Data;

import java.util.List;

@Data
public class DepositResponse {
    private Long id;
    private String accountNumber;
    private Double balance;
    private List<Transaction> transactions;
}
