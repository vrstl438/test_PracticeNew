package api.domain.model.response;

import api.domain.model.ResponseModel;
import api.domain.model.response.AccountResponse.Transaction;
import lombok.Data;

import java.util.List;

@Data
public class DepositResponse implements ResponseModel {
    private Long id;
    private String accountNumber;
    private Double balance;
    private List<Transaction> transactions;
}
