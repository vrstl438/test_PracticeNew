package domain.model.response;

import lombok.Getter;

@Getter
public class TransferResponse {
    private String message;
    private Double amount;
    private Long receiverAccountId;
    private Long senderAccountId;
}
