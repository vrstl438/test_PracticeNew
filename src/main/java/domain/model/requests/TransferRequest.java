package domain.model.requests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransferRequest {
    private Long senderAccountId;
    private Long receiverAccountId;
    private Double amount;
}
