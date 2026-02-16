package api.domain.model.response;

import api.domain.model.ResponseModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse implements ResponseModel {
    private Long id;
    private String accountNumber;
    private Double balance;
    private List <Transaction> transactions;

    @Data
    public static class Transaction implements ResponseModel {
        private Long id;
        private String type;
        private Double amount;
        private String timestamp;
        private Long relatedAccountId;
    }
}
