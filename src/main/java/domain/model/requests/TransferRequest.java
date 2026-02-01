package domain.model.requests;

import domain.model.RequestModel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransferRequest implements RequestModel {
    private Long senderAccountId;
    private Long receiverAccountId;
    private Double amount;
}
